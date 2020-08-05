package org.somda.sdc.proto.provider.service;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import io.grpc.BindableService;
import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.common.access.MdibAccess;
import org.somda.sdc.biceps.model.message.AbstractReport;
import org.somda.sdc.biceps.model.message.DescriptionModificationReport;
import org.somda.sdc.biceps.model.message.EpisodicAlertReport;
import org.somda.sdc.biceps.model.message.EpisodicComponentReport;
import org.somda.sdc.biceps.model.message.EpisodicContextReport;
import org.somda.sdc.biceps.model.message.EpisodicMetricReport;
import org.somda.sdc.biceps.model.message.EpisodicOperationalStateReport;
import org.somda.sdc.biceps.model.message.WaveformStream;
import org.somda.sdc.biceps.model.participant.AbstractContextState;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.device.EventSourceAccess;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.soap.wseventing.model.WsEventingStatus;
import org.somda.sdc.glue.provider.services.helper.ReportGenerator;
import org.somda.sdc.glue.provider.services.helper.factory.ReportGeneratorFactory;
import org.somda.sdc.proto.common.ProtoConstants;
import org.somda.sdc.proto.mapping.message.PojoToProtoMapper;
import org.somda.sdc.proto.mapping.participant.PojoToProtoOneOfMapper;
import org.somda.sdc.proto.mapping.participant.PojoToProtoTreeMapper;
import org.somda.sdc.proto.mapping.participant.factory.PojoToProtoTreeMapperFactory;
import org.somda.sdc.proto.model.*;
import org.somda.sdc.proto.model.addressing.Addressing;
import org.somda.sdc.proto.model.biceps.GetContextStatesResponseMsg;
import org.somda.sdc.proto.model.biceps.GetMdDescriptionResponseMsg;
import org.somda.sdc.proto.model.biceps.GetMdStateResponseMsg;
import org.somda.sdc.proto.model.biceps.GetMdibResponseMsg;
import org.somda.sdc.proto.model.common.QName;
import org.somda.sdc.proto.provider.sco.OperationInvocationReceiver;

import java.util.Collections;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class HighPriorityServices implements EventSourceAccess {
    private static final Logger LOG = LogManager.getLogger(HighPriorityServices.class);

    private final MdibReportingService reportingService;
    private final PojoToProtoMapper messageMapper;
    private final Logger instanceLogger;
    private final Map<QName, BindableService> services;
    private final ReportGenerator reportGenerator;

    @AssistedInject
    HighPriorityServices(@Assisted LocalMdibAccess mdibAccess,
                         @Named(org.somda.sdc.common.CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier,
                         ReportGeneratorFactory reportGeneratorFactory,
                         PojoToProtoTreeMapperFactory treeMapperFactory,
                         PojoToProtoMapper messageMapper,
                         PojoToProtoOneOfMapper oneOfMapper) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        final PojoToProtoTreeMapper treeMapper = treeMapperFactory.create(mdibAccess);
        this.messageMapper = messageMapper;

        final GetService getService = new GetService(mdibAccess, treeMapper, oneOfMapper);
        this.reportingService = new MdibReportingService(frameworkIdentifier);

        this.services = Map.of(
                ProtoConstants.GET_SERVICE_QNAME, getService,
                ProtoConstants.MDIB_REPORTING_SERVICE_QNAME, reportingService
        );

        this.reportGenerator = reportGeneratorFactory.createReportGenerator(this);
        mdibAccess.registerObserver(reportGenerator);
    }

    /**
     * @return all {@linkplain BindableService} instances handled by this container
     */
    public Map<QName, BindableService> getServices() {
        return services;
    }

    /**
     * Sends an episodic report to all subscribers
     *
     * @param action  of the episodic report
     * @param payload JAXB message
     * @throws RuntimeException if message cannot be mapped to proto
     */
    public void sendEpisodicNotification(String action, AbstractReport payload) throws RuntimeException {
        var episodicReport = EpisodicReport.newBuilder();

        if (payload instanceof EpisodicMetricReport) {
            episodicReport.setMetric(messageMapper.mapEpisodicMetricReport((EpisodicMetricReport) payload));
        } else if (payload instanceof EpisodicAlertReport) {
            episodicReport.setAlert(messageMapper.mapEpisodicAlertReport((EpisodicAlertReport) payload));
        } else if (payload instanceof EpisodicComponentReport) {
            episodicReport.setComponent(messageMapper.mapEpisodicComponentReport((EpisodicComponentReport) payload));
        } else if (payload instanceof EpisodicContextReport) {
            episodicReport.setContext(messageMapper.mapEpisodicContextReport((EpisodicContextReport) payload));
        } else if (payload instanceof EpisodicOperationalStateReport) {
            episodicReport.setOperationalState(messageMapper.mapEpisodicOperationalStateReport((EpisodicOperationalStateReport) payload));
        } else if (payload instanceof WaveformStream) {
            episodicReport.setWaveform(messageMapper.mapWaveformStream((WaveformStream) payload));
        } else if (payload instanceof DescriptionModificationReport) {
            messageMapper.mapDescriptionModificationReport((DescriptionModificationReport) payload);
        } else {
            throw new RuntimeException("Unsupported report type " + payload.getClass().getSimpleName());
        }
        instanceLogger.debug("Sending episodic notification with action {}", action);

        reportingService.offerNotification(action, episodicReport.build());
    }


    /**
     * Ends all subscriptions currently active.
     */
    public void subscriptionEndToAll(final WsEventingStatus status) {
        reportingService.endAll();
    }

    @Override
    public void sendNotification(final String action, final Object payload) throws MarshallingException, TransportException {
        sendEpisodicNotification(action, (AbstractReport) payload);
    }

    static class GetService extends GetServiceGrpc.GetServiceImplBase {

        private final PojoToProtoTreeMapper treeMapper;
        private final LocalMdibAccess mdibAccess;
        private final PojoToProtoOneOfMapper oneOfMapper;

        GetService(
                LocalMdibAccess mdibAccess,
                PojoToProtoTreeMapper treeMapper,
                PojoToProtoOneOfMapper oneOfMapper) {
            this.mdibAccess = mdibAccess;
            this.treeMapper = treeMapper;
            this.oneOfMapper = oneOfMapper;
        }

        @Override
        public void getMdib(final GetMdibRequest request, final StreamObserver<GetMdibResponse> responseObserver) {
            // payload is for suckers

            var responseBody = treeMapper.mapMdib();
            var response = GetMdibResponse.newBuilder()
                    .setPayload(GetMdibResponseMsg.newBuilder().setMdib(responseBody).build());

            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        }

        @Override
        public void getMdDescription(final GetMdDescriptionRequest request, final StreamObserver<GetMdDescriptionResponse> responseObserver) {
            var responseBody = treeMapper.mapMdDescription(Collections.emptyList());
            var response = GetMdDescriptionResponse.newBuilder()
                    .setPayload(GetMdDescriptionResponseMsg.newBuilder().setMdDescription(responseBody).build());

            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        }

        @Override
        public void getMdState(final GetMdStateRequest request, final StreamObserver<GetMdStateResponse> responseObserver) {
            var responseBody = treeMapper.mapMdState(Collections.emptyList());
            var response = GetMdStateResponse.newBuilder()
                    .setPayload(GetMdStateResponseMsg.newBuilder().setMdState(responseBody).build());

            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        }

        @Override
        public void getContextStates(final GetContextStatesRequest request, final StreamObserver<GetContextStatesResponse> responseObserver) {
            var responsePayload = GetContextStatesResponseMsg.newBuilder();
            mdibAccess.findContextStatesByType(AbstractContextState.class)
                    .forEach(state -> responsePayload.addContextState(oneOfMapper.mapAbstractContextStateOneOf(state)));
            var response = GetContextStatesResponse.newBuilder().setPayload(responsePayload.build());

            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        }
    }

    static class MdibReportingService extends MdibReportingServiceGrpc.MdibReportingServiceImplBase {
        private static final Logger LOG = LogManager.getLogger(MdibReportingService.class);

        // TODO: mechanic is dumb, fix!
        private static final String END_ACTION = "STOP THE PRESSES";
        private static final int QUEUE_SIZE = 50;

        private final ListMultimap<String, Queue<Pair<String, EpisodicReport>>> queueMap;
        private final Logger instanceLogger;

        MdibReportingService(String frameworkIdentifier) {
            this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
            this.queueMap = Multimaps.synchronizedListMultimap(ArrayListMultimap.create());
        }

        public void offerNotification(String action, EpisodicReport report) {
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
        public void episodicReport(final EpisodicReportRequest request, final StreamObserver<EpisodicReportStream> responseObserver) {

            var context = Context.current();
            AtomicBoolean isCanceled = new AtomicBoolean(false);
            context.addListener(
                    new Context.CancellationListener() {
                        @Override
                        public void cancelled(Context context) {
                            isCanceled.set(true);
                        }
                    },
                    MoreExecutors.directExecutor()
            );

            // get id for this handle
            var actions = request.getFilter().getActionFilter().getActionList();
            var queue = new ArrayBlockingQueue<Pair<String, EpisodicReport>>(QUEUE_SIZE);
            try {
                // add queue to map for all requested actions
                actions.forEach(action -> queueMap.put(action, queue));

                while (!isCanceled.get()) {
                    var element = queue.take();

                    var action = element.getLeft();
                    if (END_ACTION.equals(action)) {
                        responseObserver.onCompleted();
                        return;
                    }
                    var report = element.getRight();
                    var message = EpisodicReportStream.newBuilder()
                            .setReport(report)
                            .setAddressing(Addressing.newBuilder().setAction(action).build());
                    responseObserver.onNext(message.build());
                }
            } catch (InterruptedException e) {
                LOG.warn("Queue interrupted", e);
            } finally {
                LOG.info("Episodic report ended");
                actions.forEach(action -> queueMap.remove(action, queue));
            }
        }

        @Override
        public void periodicReport(final PeriodicReportRequest request, final StreamObserver<PeriodicReportStream> responseObserver) {
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
