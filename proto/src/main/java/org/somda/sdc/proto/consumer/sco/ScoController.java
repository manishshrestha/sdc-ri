package org.somda.sdc.proto.consumer.sco;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.message.AbstractSet;
import org.somda.sdc.biceps.model.message.AbstractSetResponse;
import org.somda.sdc.biceps.model.message.Activate;
import org.somda.sdc.biceps.model.message.OperationInvokedReport;
import org.somda.sdc.biceps.model.message.SetString;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.common.util.ExecutorWrapperService;
import org.somda.sdc.glue.common.WsdlConstants;
import org.somda.sdc.proto.addressing.AddressingUtil;
import org.somda.sdc.proto.consumer.SetServiceAccess;
import org.somda.sdc.proto.consumer.sco.factory.ScoTransactionFactory;
import org.somda.sdc.proto.consumer.sco.helper.OperationInvocationDispatcher;
import org.somda.sdc.proto.guice.ProtoConsumer;
import org.somda.sdc.proto.mapping.message.PojoToProtoMapper;
import org.somda.sdc.proto.mapping.message.ProtoToPojoMapper;
import org.somda.sdc.proto.model.ActivateRequest;
import org.somda.sdc.proto.model.SetServiceGrpc;
import org.somda.sdc.proto.model.SetStringRequest;

import javax.annotation.Nullable;

/**
 * Controller class that is responsible for invoking set requests and processing incoming operation invoked reports.
 */
public class ScoController implements SetServiceAccess {
    private static final Logger LOG = LogManager.getLogger(ScoController.class);
    private final SetServiceGrpc.SetServiceBlockingStub setServiceProxy;
    private final OperationInvocationDispatcher operationInvocationDispatcher;
    private final ExecutorWrapperService<ListeningExecutorService> executorService;
    private final ScoTransactionFactory scoTransactionFactory;
    private final PojoToProtoMapper pojoToProtoMapper;
    private final ProtoToPojoMapper protoToPojoMapper;
    private final AddressingUtil addressingUtil;
    private final Logger instanceLogger;

    @AssistedInject
    ScoController(@Assisted SetServiceGrpc.SetServiceBlockingStub setServiceProxy,
                  OperationInvocationDispatcher operationInvocationDispatcher,
                  @ProtoConsumer ExecutorWrapperService<ListeningExecutorService> executorService,
                  ScoTransactionFactory scoTransactionFactory,
                  PojoToProtoMapper pojoToProtoMapper,
                  ProtoToPojoMapper protoToPojoMapper,
                  AddressingUtil addressingUtil,
                  @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.setServiceProxy = setServiceProxy;
        this.operationInvocationDispatcher = operationInvocationDispatcher;
        this.executorService = executorService;
        this.scoTransactionFactory = scoTransactionFactory;
        this.pojoToProtoMapper = pojoToProtoMapper;
        this.protoToPojoMapper = protoToPojoMapper;
        this.addressingUtil = addressingUtil;
    }

    @Override
    public <T extends AbstractSet, V extends AbstractSetResponse> ListenableFuture<ScoTransaction<V>> invoke(
            T setRequest,
            Class<V> responseClass) {
        return invoke(setRequest, null, responseClass);
    }

    @Override
    public <T extends AbstractSet, V extends AbstractSetResponse> ListenableFuture<ScoTransaction<V>> invoke(
            T setRequest,
            @Nullable java.util.function.Consumer<OperationInvokedReport.ReportPart> reportListener,
            Class<V> responseClass) {
        return executorService.get().submit(() -> {
            instanceLogger.debug("Invoke {} operation with payload: {}",
                    setRequest.getClass().getSimpleName(), setRequest.toString());
            final V response = responseClass.cast(sendMessage(setRequest));
            instanceLogger.debug("Received {} message with payload: {}",
                    response.getClass().getSimpleName(), response.toString());

            final ScoTransactionImpl<V> transaction =
                    scoTransactionFactory.create(response, reportListener);

            operationInvocationDispatcher.registerTransaction(transaction);

            return transaction;
        });
    }

    /**
     * Accepts an operation invoked report and dispatches report parts to SCO transactions.
     *
     * @param report the report to dispatch (note that a report can contain multiple report parts
     *               that belong to different transaction.
     * @see ScoTransaction
     */
    public void processOperationInvokedReport(OperationInvokedReport report) {
        operationInvocationDispatcher.dispatchReport(report);
    }

    private <T extends AbstractSet> Object sendMessage(T setRequest)
            throws InvocationException {
        var action = WsdlConstants.ACTION_SET_PREFIX + setRequest.getClass().getSimpleName();
        var addressing = addressingUtil.assemblyAddressing(action);
        if (setRequest instanceof Activate) {
            var request = ActivateRequest.newBuilder()
                    .setAddressing(addressing)
                    .setPayload(pojoToProtoMapper.mapActivate((Activate) setRequest))
                    .build();
            return protoToPojoMapper.map(setServiceProxy.activate(request).getPayload());
        } else if (setRequest instanceof SetString) {
            var request = SetStringRequest.newBuilder()
                    .setAddressing(addressing)
                    .setPayload(pojoToProtoMapper.mapSetString((SetString) setRequest))
                    .build();
            return protoToPojoMapper.map(setServiceProxy.setString(request).getPayload());
        }

        throw new InvocationException(String.format("Operation type not supported at the moment: %s",
                setRequest.getClass()));
    }
}
