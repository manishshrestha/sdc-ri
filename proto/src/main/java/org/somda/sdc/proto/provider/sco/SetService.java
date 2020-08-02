package org.somda.sdc.proto.provider.sco;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.Service;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import io.grpc.stub.StreamObserver;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.somda.sdc.biceps.model.message.AbstractReport;
import org.somda.sdc.biceps.model.message.AbstractSetResponse;
import org.somda.sdc.biceps.model.message.ActivateResponse;
import org.somda.sdc.biceps.model.message.InvocationInfo;
import org.somda.sdc.biceps.model.participant.InstanceIdentifier;
import org.somda.sdc.proto.addressing.AddressingUtil;
import org.somda.sdc.proto.mapping.message.PojoToProtoMapper;
import org.somda.sdc.proto.mapping.message.ProtoToPojoMapper;
import org.somda.sdc.proto.mapping.participant.PojoToProtoTreeMapper;
import org.somda.sdc.proto.model.SdcMessages;
import org.somda.sdc.proto.model.SetServiceGrpc;
import org.somda.sdc.proto.model.biceps.OperationInvokedReportMsg;
import org.somda.sdc.proto.provider.EventSource;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

class SetService extends AbstractIdleService implements Service, EventSource {
    private final static int QUEUE_SIZE = 100;
    private final ScoController scoController;
    private final PojoToProtoTreeMapper treeMapper;
    private final ProtoToPojoMapper messageMapper;
    private final PojoToProtoMapper fromPojoMessageMapper;
    private final AddressingUtil addressingUtil;
    private Map<Integer, BlockingQueue<QueueItem>> subscribedInvokedReports;

    @Inject
    SetService(@Assisted ScoController scoController,
               PojoToProtoTreeMapper treeMapper,
               ProtoToPojoMapper fromProtoMessageMapper,
               PojoToProtoMapper fromPojoMessageMapper,
               AddressingUtil addressingUtil) {
        this.scoController = scoController;
        this.treeMapper = treeMapper;
        this.messageMapper = fromProtoMessageMapper;
        this.fromPojoMessageMapper = fromPojoMessageMapper;
        this.addressingUtil = addressingUtil;
        this.subscribedInvokedReports = new ConcurrentHashMap<>();
    }

    @Override
    protected void startUp() {
    }

    @Override
    protected void shutDown() {
        subscribedInvokedReports.values().forEach(it -> it.offer(new QueueTerminationItem()));
    }

    private void offerInvokedReport(OperationInvokedReportMsg report) {
        subscribedInvokedReports.values().forEach(it -> it.offer(new InvokedReportItem(report)));
    }

    @Override
    public void sendNotification(String action, AbstractReport report) throws Exception {
        // todo map and offer
    }

    private static class SetServiceCallbacks extends SetServiceGrpc.SetServiceImplBase {
        private final ScoController scoController;
        private final ProtoToPojoMapper fromProtoMessageMapper;
        private final PojoToProtoMapper fromPojoMessageMapper;
        private final AddressingUtil addressingUtil;
        private final Map<Integer, BlockingQueue<QueueItem>> subscribedInvokedReports;
        private final AtomicInteger susbcriptions;

        SetServiceCallbacks(ScoController scoController,
                            ProtoToPojoMapper fromProtoMessageMapper,
                            PojoToProtoMapper fromPojoMessageMapper,
                            AddressingUtil addressingUtil,
                            Map<Integer, BlockingQueue<QueueItem>> subscribedInvokedReports) {
            this.scoController = scoController;
            this.fromProtoMessageMapper = fromProtoMessageMapper;
            this.fromPojoMessageMapper = fromPojoMessageMapper;
            this.addressingUtil = addressingUtil;
            this.subscribedInvokedReports = subscribedInvokedReports;
            this.susbcriptions = new AtomicInteger(0);
        }

        @Override
        public void activate(SdcMessages.ActivateRequest request,
                StreamObserver<SdcMessages.ActivateResponse> responseObserver) {
            var resp = scoController.processIncomingSetOperation(
                    request.getPayload().getAbstractSet().getOperationHandleRef(),
                    new InstanceIdentifier(), // todo pass certificate common name here
                    fromProtoMessageMapper.map(request.getPayload()));

            var message = SdcMessages.ActivateResponse.newBuilder();
            message.setAddressing(addressingUtil.assemblyAddressing("activate"));
            message.setPayload(fromPojoMessageMapper.mapActivateResponse(createResponse(ActivateResponse.class, resp)));
            responseObserver.onNext(message.build());
            responseObserver.onCompleted();
        }

        @Override
        public void setMetricState(SdcMessages.ActivateRequest request,
                StreamObserver<SdcMessages.ActivateResponse> responseObserver) {
            super.setMetricState(request, responseObserver);
        }

        @Override
        public void setComponentState(SdcMessages.SetComponentStateRequest request,
                StreamObserver<SdcMessages.SetComponentStateResponse> responseObserver) {
            super.setComponentState(request, responseObserver);
        }

        @Override
        public void setContextState(SdcMessages.SetContextStateRequest request,
                StreamObserver<SdcMessages.SetContextStateResponse> responseObserver) {
            super.setContextState(request, responseObserver);
        }

        @Override
        public void setAlertState(SdcMessages.SetAlertStateRequest request,
                StreamObserver<SdcMessages.SetAlertStateResponse> responseObserver) {
            super.setAlertState(request, responseObserver);
        }

        @Override
        public void setString(SdcMessages.SetStringRequest request,
                StreamObserver<SdcMessages.SetStringResponse> responseObserver) {
            super.setString(request, responseObserver);
        }

        @Override
        public void setValue(SdcMessages.SetValueRequest request,
                StreamObserver<SdcMessages.SetValueResponse> responseObserver) {
            super.setValue(request, responseObserver);
        }

        @Override
        public void operationInvokedReport(SdcMessages.OperationInvokedReportRequest request,
                StreamObserver<SdcMessages.OperationInvokedReportStream> responseObserver) {
            var queue = new ArrayBlockingQueue<QueueItem>(QUEUE_SIZE);
            var subscriptionNumber = susbcriptions.incrementAndGet();
            subscribedInvokedReports.put(subscriptionNumber, queue);
            try {
                while (true) {
                    var element = queue.poll();
                    // this will throw and kill the subscription, should be changed at some point
                    assert element != null;
                    if (element instanceof QueueTerminationItem) {
                        responseObserver.onCompleted();
                        return;
                    } else if (element instanceof InvokedReportItem) {
                        var report = ((InvokedReportItem) element).getReport();
                        var message = SdcMessages.OperationInvokedReportStream.newBuilder()
                                .setOperationInvoked(report)
                                .setAddressing(addressingUtil.assemblyAddressing("action"));
                        responseObserver.onNext(message.build());
                    }
                }
            } finally {
                subscribedInvokedReports.remove(subscriptionNumber);
            }
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

    private interface QueueItem {
    }

    private class InvokedReportItem implements QueueItem {
        private final OperationInvokedReportMsg report;

        InvokedReportItem(OperationInvokedReportMsg report) {
            this.report = report;
        }

        OperationInvokedReportMsg getReport() {
            return report;
        }
    }

    private class QueueTerminationItem implements QueueItem {
    }
}