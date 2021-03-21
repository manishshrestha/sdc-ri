package org.somda.sdc.proto.provider.sco;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import io.grpc.stub.StreamObserver;
import org.somda.sdc.biceps.model.message.AbstractSetResponse;
import org.somda.sdc.biceps.model.message.InvocationInfo;
import org.somda.sdc.biceps.model.participant.InstanceIdentifier;
import org.somda.sdc.proto.addressing.AddressingUtil;
import org.somda.sdc.proto.mapping.message.PojoToProtoMapper;
import org.somda.sdc.proto.mapping.message.ProtoToPojoMapper;
import org.somda.protosdc.proto.model.ActivateRequest;
import org.somda.protosdc.proto.model.ActivateResponse;
import org.somda.protosdc.proto.model.OperationInvokedReportRequest;
import org.somda.protosdc.proto.model.OperationInvokedReportStream;
import org.somda.protosdc.proto.model.SetAlertStateRequest;
import org.somda.protosdc.proto.model.SetAlertStateResponse;
import org.somda.protosdc.proto.model.SetComponentStateRequest;
import org.somda.protosdc.proto.model.SetComponentStateResponse;
import org.somda.protosdc.proto.model.SetContextStateRequest;
import org.somda.protosdc.proto.model.SetContextStateResponse;
import org.somda.protosdc.proto.model.SetMetricStateRequest;
import org.somda.protosdc.proto.model.SetMetricStateResponse;
import org.somda.protosdc.proto.model.SetServiceGrpc;
import org.somda.protosdc.proto.model.SetStringRequest;
import org.somda.protosdc.proto.model.SetStringResponse;
import org.somda.protosdc.proto.model.SetValueRequest;
import org.somda.protosdc.proto.model.SetValueResponse;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;

public class SetService extends SetServiceGrpc.SetServiceImplBase {

    private final ScoController scoController;
    private final ProtoToPojoMapper protoToPojoMapper;
    private final PojoToProtoMapper pojoToProtoMapper;
    private final AddressingUtil addressingUtil;
    private final OperationInvokedEventSource eventSource;

    @Inject
    SetService(@Assisted ScoController scoController,
               @Assisted OperationInvokedEventSource eventSource,
               ProtoToPojoMapper protoToPojoMapper,
               PojoToProtoMapper pojoToProtoMapper,
               AddressingUtil addressingUtil) {
        this.scoController = scoController;
        this.eventSource = eventSource;
        this.protoToPojoMapper = protoToPojoMapper;
        this.pojoToProtoMapper = pojoToProtoMapper;
        this.addressingUtil = addressingUtil;
    }

    @Override
    public void activate(ActivateRequest request,
                         StreamObserver<ActivateResponse> responseObserver) {
        var resp = scoController.processIncomingSetOperation(
                request.getPayload().getAbstractSet().getOperationHandleRef().getString(),
                new InstanceIdentifier(), // todo pass certificate common name here
                protoToPojoMapper.map(request.getPayload()).getArgument());

        var responseMsg = ActivateResponse.newBuilder();
        responseMsg.setAddressing(addressingUtil.assembleAddressing("activate"));
        responseMsg.setPayload(pojoToProtoMapper.mapActivateResponse(
                createResponse(org.somda.sdc.biceps.model.message.ActivateResponse.class, resp)));
        responseObserver.onNext(responseMsg.build());
        responseObserver.onCompleted();
    }

    @Override
    public void setMetricState(final SetMetricStateRequest request, final StreamObserver<SetMetricStateResponse> responseObserver) {
        super.setMetricState(request, responseObserver);
    }

    @Override
    public void setComponentState(SetComponentStateRequest request,
                                  StreamObserver<SetComponentStateResponse> responseObserver) {
        super.setComponentState(request, responseObserver);
    }

    @Override
    public void setContextState(SetContextStateRequest request,
                                StreamObserver<SetContextStateResponse> responseObserver) {
        super.setContextState(request, responseObserver);
    }

    @Override
    public void setAlertState(SetAlertStateRequest request,
                              StreamObserver<SetAlertStateResponse> responseObserver) {
        super.setAlertState(request, responseObserver);
    }

    @Override
    public void setString(SetStringRequest request,
                          StreamObserver<SetStringResponse> responseObserver) {
        var resp = scoController.processIncomingSetOperation(
                request.getPayload().getAbstractSet().getOperationHandleRef().getString(),
                new InstanceIdentifier(), // todo pass certificate common name here
                request.getPayload().getRequestedStringValue());

        var responseMsg = SetStringResponse.newBuilder();
        responseMsg.setAddressing(addressingUtil.assembleAddressing("setstring"));
        responseMsg.setPayload(pojoToProtoMapper.mapSetStringResponse(
                createResponse(org.somda.sdc.biceps.model.message.SetStringResponse.class, resp)));
        responseObserver.onNext(responseMsg.build());
        responseObserver.onCompleted();
    }

    @Override
    public void setValue(SetValueRequest request,
                         StreamObserver<SetValueResponse> responseObserver) {
        var resp = scoController.processIncomingSetOperation(
                request.getPayload().getAbstractSet().getOperationHandleRef().getString(),
                new InstanceIdentifier(), // todo pass certificate common name here
                new BigDecimal(request.getPayload().getRequestedNumericValue()));

        var responseMsg = SetValueResponse.newBuilder();
        responseMsg.setAddressing(addressingUtil.assembleAddressing("setvalue"));
        responseMsg.setPayload(pojoToProtoMapper.mapSetValueResponse(
                createResponse(org.somda.sdc.biceps.model.message.SetValueResponse.class, resp)));
        responseObserver.onNext(responseMsg.build());
        responseObserver.onCompleted();
    }

    @Override
    public void operationInvokedReport(OperationInvokedReportRequest request,
                                       StreamObserver<OperationInvokedReportStream> responseObserver) {
        eventSource.handleOperationInvokedReport(request, responseObserver);
    }

    private <T extends AbstractSetResponse> T createResponse(Class<T> responseType, InvocationResponse respInfo) {
        try {
            var response = responseType.getConstructor().newInstance();
            var invocationInfo = new InvocationInfo();
            invocationInfo.setTransactionId(respInfo.getTransactionId());
            invocationInfo.setInvocationErrorMessage(respInfo.getInvocationErrorMessage());
            invocationInfo.setInvocationState(respInfo.getInvocationState());
            invocationInfo.setInvocationError(respInfo.getInvocationError());
            response.setInstanceId(respInfo.getMdibVersion().getInstanceId());
            response.setSequenceId(respInfo.getMdibVersion().getSequenceId());
            response.setMdibVersion(respInfo.getMdibVersion().getVersion());
            response.setInvocationInfo(invocationInfo);
            return response;
        } catch (InstantiationException
                | InvocationTargetException
                | NoSuchMethodException
                | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}