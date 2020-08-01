package org.somda.sdc.proto.provider.services;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import io.grpc.BindableService;
import io.grpc.stub.StreamObserver;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.message.AbstractReport;
import org.somda.sdc.biceps.model.message.EpisodicAlertReport;
import org.somda.sdc.biceps.model.message.EpisodicMetricReport;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.proto.common.ProtoConstants;
import org.somda.sdc.proto.mapping.message.PojoToProtoMapper;
import org.somda.sdc.proto.mapping.participant.PojoToProtoTreeMapper;
import org.somda.sdc.proto.mapping.participant.factory.PojoToProtoTreeMapperFactory;
import org.somda.sdc.proto.model.GetServiceGrpc;
import org.somda.sdc.proto.model.MdibReportingServiceGrpc;
import org.somda.sdc.proto.model.SdcMessages;
import org.somda.sdc.proto.model.addressing.AddressingTypes;
import org.somda.sdc.proto.model.biceps.GetMdDescriptionResponseMsg;
import org.somda.sdc.proto.model.biceps.GetMdStateResponseMsg;
import org.somda.sdc.proto.model.biceps.GetMdibResponseMsg;
import org.somda.sdc.proto.model.common.CommonTypes;

import java.util.Collections;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class HighPriorityServices {
    private static final Logger LOG = LogManager.getLogger(HighPriorityServices.class);

    private final MdibReportingService reportingService;
    private final PojoToProtoMapper messageMapper;
    private final Logger instanceLogger;
    private final Map<CommonTypes.QName, BindableService> services;

    @AssistedInject
    HighPriorityServices(@Assisted LocalMdibAccess mdibAccess,
                         @Named(org.somda.sdc.common.CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier,
                         PojoToProtoTreeMapperFactory treeMapperFactory,
                         PojoToProtoMapper messageMapper) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        final PojoToProtoTreeMapper treeMapper = treeMapperFactory.create(mdibAccess);
        this.messageMapper = messageMapper;

        final GetService getService = new GetService(treeMapper);
        this.reportingService = new MdibReportingService(frameworkIdentifier);

        this.services = Map.of(
                ProtoConstants.GET_SERVICE_QNAME, getService,
                ProtoConstants.MDIB_REPORTING_SERVICE_QNAME, reportingService
        );
    }

    /**
     * @return all {@linkplain BindableService} instances handled by this container
     */
    public Map<CommonTypes.QName, BindableService> getServices() {
        return services;
    }

    /**
     * Sends an episodic report to all subscribers
     * @param action of the episodic report
     * @param payload JAXB message
     * @throws RuntimeException if message cannot be mapped to proto
     */
    public void sendEpisodicNotification(String action, AbstractReport payload) throws RuntimeException {
        var episodicReport = SdcMessages.EpisodicReport.newBuilder();

        if (payload instanceof EpisodicMetricReport) {
            episodicReport.setMetric(messageMapper.mapEpisodicMetricReport((EpisodicMetricReport) payload));
        } else if (payload instanceof EpisodicAlertReport) {
            episodicReport.setAlert(messageMapper.mapEpisodicAlertReport((EpisodicAlertReport) payload));
        } else {
            throw new RuntimeException("Unsupported report type " + payload.getClass().getSimpleName());
        }
        instanceLogger.debug("Sending episodic notification with action {}", action);

        reportingService.offerNotification(action, episodicReport.build());
    }

    /**
     * Ends all subscriptions currently active.
     */
    public void subscriptionEndToAll() {
        reportingService.endAll();
    }

    static class GetService extends GetServiceGrpc.GetServiceImplBase {

        private final PojoToProtoTreeMapper treeMapper;

        GetService(PojoToProtoTreeMapper treeMapper) {
            this.treeMapper = treeMapper;
        }

        @Override
        public void getMdib(final SdcMessages.GetMdibRequest request, final StreamObserver<SdcMessages.GetMdibResponse> responseObserver) {
            // payload is for suckers

            var responseBody = treeMapper.mapMdib();
            var response = SdcMessages.GetMdibResponse.newBuilder()
                    .setPayload(GetMdibResponseMsg.newBuilder().setMdib(responseBody).build());

            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        }

        @Override
        public void getMdDescription(final SdcMessages.GetMdDescriptionRequest request, final StreamObserver<SdcMessages.GetMdDescriptionResponse> responseObserver) {
            var responseBody = treeMapper.mapMdDescription(Collections.emptyList());
            var response = SdcMessages.GetMdDescriptionResponse.newBuilder()
                    .setPayload(GetMdDescriptionResponseMsg.newBuilder().setMdDescription(responseBody).build());

            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        }

        @Override
        public void getMdState(final SdcMessages.GetMdStateRequest request, final StreamObserver<SdcMessages.GetMdStateResponse> responseObserver) {
            var responseBody = treeMapper.mapMdState(Collections.emptyList());
            var response = SdcMessages.GetMdStateResponse.newBuilder()
                    .setPayload(GetMdStateResponseMsg.newBuilder().setMdState(responseBody).build());

            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        }
    }

    static class MdibReportingService extends MdibReportingServiceGrpc.MdibReportingServiceImplBase {
        private static final Logger LOG = LogManager.getLogger(MdibReportingService.class);

        // TODO: mechanic is dumb, fix!
        private static final String END_ACTION = "STOP THE PRESSES";
        private static final int QUEUE_SIZE = 50;

        private final ListMultimap<String, Queue<Pair<String, SdcMessages.EpisodicReport>>> queueMap;
        private final Logger instanceLogger;

        MdibReportingService(String frameworkIdentifier) {
            this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
            this.queueMap = Multimaps.synchronizedListMultimap(ArrayListMultimap.create());
        }

        public void offerNotification(String action, SdcMessages.EpisodicReport report) {
            // filter by actions here
            instanceLogger.debug("Offering notification for action {} to all handlers", action);
            var reportQueues = this.queueMap.get(action);
            if (reportQueues != null) {
                reportQueues.forEach(queue -> queue.offer(new ImmutablePair<>(action, report)));
            }
        }

        public void endAll() {
            queueMap.values().forEach(queue -> queue.offer(new ImmutablePair<>(END_ACTION, null)));
        }

        @Override
        public void episodicReport(final SdcMessages.EpisodicReportRequest request, final StreamObserver<SdcMessages.EpisodicReportStream> responseObserver) {

            // get id for this handle
            var actions = request.getFilter().getActionFilter().getActionList();
            var queue = new ArrayBlockingQueue<Pair<String, SdcMessages.EpisodicReport>>(QUEUE_SIZE);
            try {
                // add queue to map for all requested actions
                actions.forEach(action -> queueMap.put(action, queue));

                while (true) {
                    var element = queue.poll();
                    // this will throw and kill the subscription, should be changed at some point
                    assert element != null;
                    var action = element.getLeft();
                    if (END_ACTION.equals(action)) {
                        responseObserver.onCompleted();
                        return;
                    }
                    var report = element.getRight();
                    var message = SdcMessages.EpisodicReportStream.newBuilder()
                            .setReport(report)
                            .setAddressing(AddressingTypes.Addressing.newBuilder().setAction(action).build());
                    responseObserver.onNext(message.build());
                }
            } finally {
                actions.forEach(action -> queueMap.remove(action, queue));
            }
        }

        @Override
        public void periodicReport(final SdcMessages.PeriodicReportRequest request, final StreamObserver<SdcMessages.PeriodicReportStream> responseObserver) {
            super.periodicReport(request, responseObserver);
        }
    }

//    static class SetService extends SetServiceGrpc.SetServiceImplBase {
//        private final PojoToProtoTreeMapper treeMapper;
//
//        SetService(PojoToProtoTreeMapper treeMapper) {
//            this.treeMapper = treeMapper;
//        }
//
//
//    }
}
