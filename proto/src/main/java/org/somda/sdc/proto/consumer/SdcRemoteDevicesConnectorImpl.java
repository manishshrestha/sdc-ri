package org.somda.sdc.proto.consumer;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Provider;
import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.common.storage.PreprocessingException;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.consumer.access.factory.RemoteMdibAccessFactory;
import org.somda.sdc.biceps.model.message.GetContextStatesResponse;
import org.somda.sdc.biceps.model.message.ObjectFactory;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.common.util.ExecutorWrapperService;
import org.somda.sdc.dpws.DpwsConfig;
import org.somda.sdc.dpws.DpwsFramework;
import org.somda.sdc.dpws.service.HostedServiceProxy;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.wseventing.SubscribeResult;
import org.somda.sdc.glue.common.MdibVersionUtil;
import org.somda.sdc.glue.common.SubscribableActionsMapping;
import org.somda.sdc.glue.common.factory.ModificationsBuilderFactory;
import org.somda.sdc.glue.consumer.ConsumerConfig;
import org.somda.sdc.proto.addressing.AddressingUtil;
import org.somda.sdc.proto.consumer.event.RemoteDeviceConnectedMessage;
import org.somda.sdc.proto.consumer.event.WatchdogMessage;
import org.somda.sdc.proto.consumer.factory.SdcRemoteDeviceFactory;
import org.somda.sdc.proto.consumer.factory.SdcRemoteDeviceWatchdogFactory;
import org.somda.sdc.proto.consumer.report.ReportProcessingException;
import org.somda.sdc.proto.consumer.report.ReportProcessor;
import org.somda.sdc.proto.consumer.sco.ScoController;
import org.somda.sdc.proto.consumer.sco.factory.ScoControllerFactory;
import org.somda.sdc.proto.guice.ProtoConsumer;
import org.somda.sdc.proto.mapping.message.ProtoToPojoMapper;
import org.somda.sdc.proto.model.OperationInvokedReportRequest;
import org.somda.sdc.proto.model.OperationInvokedReportStream;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.namespace.QName;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("UnstableApiUsage")
public class SdcRemoteDevicesConnectorImpl extends AbstractIdleService
        implements SdcRemoteDevicesConnector, WatchdogObserver {
    private static final Logger LOG = LogManager.getLogger(SdcRemoteDevicesConnectorImpl.class);

    private ExecutorWrapperService<ListeningExecutorService> executorService;
    private Map<String, SdcRemoteDevice> sdcRemoteDevices;
    private EventBus eventBus;
    private final DpwsFramework dpwsFramework;
    private final AddressingUtil addressingUtil;
    private final ProtoToPojoMapper protoToPojoMapper;
    private final SdcRemoteDeviceWatchdogFactory watchdogFactory;
    private final Logger instanceLogger;
    private final Provider<ReportProcessor> reportProcessorProvider;
    private final ScoControllerFactory scoControllerFactory;
    private final Duration requestedExpires;
    private final Duration responseWaitingTime;
    private final SoapUtil soapUtil;
    private final ModificationsBuilderFactory modificationsBuilderFactory;
    private final RemoteMdibAccessFactory remoteMdibAccessFactory;
    private final ObjectFactory messageModelFactory;
    private final MdibVersionUtil mdibVersionUtil;
    private final SdcRemoteDeviceFactory sdcRemoteDeviceFactory;
    private final String frameworkIdentifier;

    @Inject
    SdcRemoteDevicesConnectorImpl(@ProtoConsumer ExecutorWrapperService<ListeningExecutorService> executorService,
                                  ConcurrentHashMap<String, SdcRemoteDevice> sdcRemoteDevices,
                                  EventBus eventBus,
                                  Provider<ReportProcessor> reportProcessorProvider,
                                  ScoControllerFactory scoControllerFactory,
                                  @Named(ConsumerConfig.REQUESTED_EXPIRES) Duration requestedExpires,
                                  @Named(DpwsConfig.MAX_WAIT_FOR_FUTURES) Duration responseWaitingTime,
                                  SoapUtil soapUtil,
                                  ModificationsBuilderFactory modificationsBuilderFactory,
                                  RemoteMdibAccessFactory remoteMdibAccessFactory,
                                  ObjectFactory messageModelFactory,
                                  MdibVersionUtil mdibVersionUtil,
                                  SdcRemoteDeviceFactory sdcRemoteDeviceFactory,
                                  DpwsFramework dpwsFramework,
                                  AddressingUtil addressingUtil,
                                  ProtoToPojoMapper protoToPojoMapper,
                                  SdcRemoteDeviceWatchdogFactory watchdogFactory,
                                  @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.dpwsFramework = dpwsFramework;
        this.addressingUtil = addressingUtil;
        this.protoToPojoMapper = protoToPojoMapper;
        this.watchdogFactory = watchdogFactory;
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.executorService = executorService;
        this.sdcRemoteDevices = sdcRemoteDevices;
        this.eventBus = eventBus;
        this.reportProcessorProvider = reportProcessorProvider;
        this.scoControllerFactory = scoControllerFactory;
        this.requestedExpires = requestedExpires;
        this.responseWaitingTime = responseWaitingTime;
        this.soapUtil = soapUtil;
        this.modificationsBuilderFactory = modificationsBuilderFactory;
        this.remoteMdibAccessFactory = remoteMdibAccessFactory;
        this.messageModelFactory = messageModelFactory;
        this.mdibVersionUtil = mdibVersionUtil;
        this.sdcRemoteDeviceFactory = sdcRemoteDeviceFactory;
        this.frameworkIdentifier = frameworkIdentifier;

        dpwsFramework.registerService(List.of(executorService, this));
    }

    @Override
    public ListenableFuture<SdcRemoteDevice> connect(org.somda.sdc.proto.consumer.Consumer consumer,
                                                     ConnectConfiguration connectConfiguration)
            throws PrerequisitesException {
        // Early exit if there is a connection already
        checkExistingConnection(consumer);

        // precheck: necessary services present?
        checkRequiredServices(consumer, connectConfiguration.getRequiredPortTypes());

        return executorService.get().submit(() -> {
            try {
                //final Logger tempLog = HostingServiceLogger.getLogger(LOG, consumer, frameworkIdentifier);
                //tempLog.info("Start connecting");
                var reportProcessor = createReportProcessor();
                var mdibAccess = createRemoteMdibAccess(consumer);
                var scoController = createScoController(consumer);

                //tempLog.info("Start watchdog");

                var watchdog = watchdogFactory.create(consumer, this);

                // Map<ServiceId, SubscribeResult>
                // use these later for watchdog, which is in charge of automatic renew
                var subscribeResults = subscribeServices(consumer, watchdog,
                        connectConfiguration.getActions(), reportProcessor, scoController.orElse(null));

                GetContextStatesResponse getContextStatesResponse = null;
                if (mdibAccess.getContextStates().isEmpty()) {
                    //tempLog.info("No context states found, try to request separately");
                    getContextStatesResponse = requestContextStates(consumer);
                }

                try {
                    //tempLog.info("Start applying reports");
                    reportProcessor.startApplyingReportsOnMdib(mdibAccess, getContextStatesResponse);
                } catch (ReportProcessingException | PreprocessingException e) {
                    throw new PrerequisitesException("Could not start applying reports on remote MDIB access", e);
                }

                //tempLog.info("Create and run remote device structure");
                var sdcRemoteDevice = sdcRemoteDeviceFactory.create(
                        consumer,
                        mdibAccess,
                        reportProcessor,
                        scoController.orElse(null),
                        watchdog
                );

                sdcRemoteDevice.startAsync().awaitRunning();
                //tempLog.info("Remote device is running");

                if (sdcRemoteDevices.putIfAbsent(
                        consumer.getEprAddress(), sdcRemoteDevice) == null) {
                    eventBus.post(new RemoteDeviceConnectedMessage(sdcRemoteDevice));
                } else {
                    throw new PrerequisitesException(String.format(
                            "A remote device with EPR address %s was already connected",
                            consumer.getEprAddress()));
                }

                return sdcRemoteDevice;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void checkExistingConnection(org.somda.sdc.proto.consumer.Consumer consumer) throws PrerequisitesException {
        if (sdcRemoteDevices.get(consumer.getEprAddress()) != null) {
            throw new PrerequisitesException(String.format("A remote device with EPR address %s was already connected",
                    consumer.getEprAddress()));
        }
    }

    private ReportProcessor createReportProcessor() {
        return reportProcessorProvider.get();
    }

    private Optional<ScoController> createScoController(org.somda.sdc.proto.consumer.Consumer consumer) {
        return consumer.getBlockingSetService().map(scoControllerFactory::create);
    }

    @Override
    public ListenableFuture<?> disconnect(String eprAddress) {
        SdcRemoteDevice sdcRemoteDevice = sdcRemoteDevices.remove(eprAddress);
        if (sdcRemoteDevice != null) {
            if (sdcRemoteDevice.isRunning()) {
                return executorService.get().submit(() -> {
                    // invalidate sdcRemoteDevice
                    // unsubscribe everything
                    sdcRemoteDevice.stopAsync().awaitTerminated();

                });
            }
        } else {
            instanceLogger.info("disconnect() called for unknown epr address {}, device already disconnected?",
                    eprAddress);
        }
        return Futures.immediateCancelledFuture();
    }

    @Override
    public Collection<SdcRemoteDevice> getConnectedDevices() {
        return new ArrayList<>(sdcRemoteDevices.values());
    }

    @Override
    public Optional<SdcRemoteDevice> getConnectedDevice(String eprAddress) {
        return Optional.ofNullable(sdcRemoteDevices.get(eprAddress));
    }

    @Override
    public void registerObserver(SdcRemoteDevicesObserver observer) {
        eventBus.register(observer);
    }

    @Override
    public void unregisterObserver(SdcRemoteDevicesObserver observer) {
        eventBus.unregister(observer);
    }

    private void checkRequiredServices(org.somda.sdc.proto.consumer.Consumer consumer,
                                       Collection<QName> requiredPortTypes) throws PrerequisitesException {
        // todo
//        List<QName> nonFoundPortTypes = new LinkedList<>(requiredPortTypes);
//        consumer.getHostedServices().values().forEach(hostedServiceProxy ->
//                nonFoundPortTypes.removeAll(hostedServiceProxy.getType().getTypes()));
//        if (!nonFoundPortTypes.isEmpty()) {
//            throw new PrerequisitesException(String.format("Required port types not found: %s", nonFoundPortTypes));
//        }
    }

    private Map<String, SubscribeResult> subscribeServices(org.somda.sdc.proto.consumer.Consumer consumer,
                                                           SdcRemoteDeviceWatchdog watchdog,
                                                           Collection<String> actionsToSubscribe,
                                                           ReportProcessor reportProcessor,
                                                           @Nullable ScoController scoController)
            throws PrerequisitesException {
        consumer.getNonblockingSetService().ifPresent(setService -> {
            if (scoController == null) {
                return;
            }

            setService.operationInvokedReport(OperationInvokedReportRequest.newBuilder()
                            .setAddressing(addressingUtil.assemblyAddressing("subscribe-operations")).build(),
                    new StreamObserver<>() {
                        @Override
                        public void onNext(OperationInvokedReportStream operationInvokedReportStream) {
                            scoController.processOperationInvokedReport(
                                    protoToPojoMapper.map(operationInvokedReportStream.getOperationInvoked()));
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            watchdog.postWatchdogMessage(new Exception("OperationInvokedReports are no longer " +
                                    String.format("subscribed as there was an unexpected error: %s",
                                            throwable.getMessage()), throwable));
                        }

                        @Override
                        public void onCompleted() {
                            watchdog.postWatchdogMessage(
                                    new Exception("OperationInvokedReports are no longer subscribed as the remote " +
                                            "device cancelled the report stream"));
                        }
                    });
        });

        // todo implement subscription to MDIB changes
        return Collections.emptyMap();


//        // Multimap<ServiceId, ActionUri>
//        final Multimap<String, String> subscriptions =
//                getServiceIdWithActionsToSubscribe(hostingServiceProxy, actionsToSubscribe);
//        Map<String, SubscribeResult> subscribeResults = new HashMap<>(subscriptions.size());
//
//        for (String serviceId : subscriptions.keySet()) {
//            final Collection<String> actions = subscriptions.get(serviceId);
//            if (actions.isEmpty()) {
//                instanceLogger.warn("Expect to find at least one action to subscribe for service id {}, " +
//                        "but none found", serviceId);
//                continue;
//            }
//            final HostedServiceProxy hostedServiceProxy = hostingServiceProxy.getHostedServices().get(serviceId);
//            if (hostedServiceProxy == null) {
//                instanceLogger.warn("Expect to found a hosted service proxy to access for service id {}, " +
//                        "but none found", serviceId);
//                continue;
//            }
//
//            final ListenableFuture<SubscribeResult> subscribeResult = hostedServiceProxy.getEventSinkAccess().subscribe(
//                    new ArrayList<>(actions),
//                    requestedExpires,
//                    new Interceptor() {
//                        @MessageInterceptor
//                        void onNotification(NotificationObject notificationObject) {
//                            final AbstractReport report = soapUtil.getBody(notificationObject.getNotification(),
//                                    AbstractReport.class).orElseThrow(() -> new RuntimeException(
//                                    String.format("Received unexpected report message from service %s", serviceId)));
//                            instanceLogger.debug("Incoming SOAP/HTTP notification: {}", report);
//                            if (report instanceof OperationInvokedReport) {
//                                if (scoController != null) {
//                                    scoController.processOperationInvokedReport((OperationInvokedReport) report);
//                                }
//                            } else {
//                                reportProcessor.processReport(report);
//                            }
//                        }
//                    });
//
//            try {
//                subscribeResults.put(serviceId, subscribeResult.get(responseWaitingTime.toSeconds(), TimeUnit.SECONDS));
//            } catch (InterruptedException | ExecutionException | TimeoutException e) {
//                throw new PrerequisitesException(
//                        String.format("Subscribe request towards service with service id %s failed. " +
//                                        "Physical target address: %s",
//                                serviceId,
//                                hostedServiceProxy.getActiveEprAddress().toString()), e);
//            }
//        }
//
//        return subscribeResults;
    }

    private Multimap<String, String> getServiceIdWithActionsToSubscribe(HostingServiceProxy hostingServiceProxy,
                                                                        Collection<String> actionsToSubscribe) {
        Multimap<String, String> subscriptions = ArrayListMultimap.create();
        for (String action : actionsToSubscribe) {
            final QName targetPortType = SubscribableActionsMapping.TARGET_QNAMES.get(action);
            if (targetPortType == null) {
                instanceLogger.warn("Found an action that could not be mapped to a target port type: {}", action);
                continue;
            }

            for (HostedServiceProxy hostedServiceProxy : hostingServiceProxy.getHostedServices().values()) {
                if (hostedServiceProxy.getType().getTypes().contains(targetPortType)) {
                    subscriptions.put(hostedServiceProxy.getType().getServiceId(), action);
                }
            }
        }

        return subscriptions;
    }

    private RemoteMdibAccess createRemoteMdibAccess(org.somda.sdc.proto.consumer.Consumer consumer)
            throws PrerequisitesException {
        throw new RuntimeException("createRemoteMdibAccess() NOT IMPLEMENTED YET");
//        // find get service
//        final HostedServiceProxy getServiceProxy =
//                findHostedServiceProxy(consumer, WsdlConstants.PORT_TYPE_GET_QNAME);
//
//        final RemoteMdibAccess mdibAccess = remoteMdibAccessFactory.createRemoteMdibAccess();
//
//        try {
//            final SoapMessage getMdibResponseMessage = getServiceProxy.getRequestResponseClient().sendRequestResponse(
//                    soapUtil.createMessage(ActionConstants.ACTION_GET_MDIB, messageModelFactory.createGetMdib()));
//            final GetMdibResponse getMdibResponse = soapUtil.getBody(getMdibResponseMessage,
//                    GetMdibResponse.class).orElseThrow(() -> new PrerequisitesException(
//                    "Remote endpoint did not send a GetMdibResponse message in response to " +
//                            String.format("a GetMdib to service %s with physical address %s",
//                                    getServiceProxy.getType().getServiceId(), getServiceProxy.getActiveEprAddress())));
//
//            final Mdib mdib = getMdibResponse.getMdib();
//            final ModificationsBuilder modBuilder = modificationsBuilderFactory.createModificationsBuilder(mdib);
//
//            mdibAccess.writeDescription(
//                    mdibVersionUtil.getMdibVersion(getMdibResponse),
//                    mdib.getMdDescription().getDescriptionVersion(),
//                    mdib.getMdState().getStateVersion(),
//                    modBuilder.get());
//
//        } catch (MarshallingException | InterceptorException | TransportException | SoapFaultException e) {
//            throw new PrerequisitesException(String.format(
//                    "Could not send a GetMdib request to service %s with physical address %s",
//                    getServiceProxy.getType().getServiceId(), getServiceProxy.getActiveEprAddress()), e);
//        } catch (PreprocessingException e) {
//            throw new PrerequisitesException("Could not write initial MDIB to remote MDIB access", e);
//        }
//
//        return mdibAccess;
    }

    private HostedServiceProxy findHostedServiceProxy(HostingServiceProxy hostingServiceProxy, QName portType)
            throws PrerequisitesException {
        HostedServiceProxy foundProxy = null;
        for (HostedServiceProxy hostedServiceProxy : hostingServiceProxy.getHostedServices().values()) {
            if (hostedServiceProxy.getType().getTypes().contains(portType)) {
                foundProxy = hostedServiceProxy;
                break;
            }
        }

        if (foundProxy == null) {
            throw new PrerequisitesException(
                    String.format("Service port type %s not found for remote device with UUID %s and " +
                                    "physical target address %s",
                            portType,
                            hostingServiceProxy.getEndpointReferenceAddress(),
                            hostingServiceProxy.getActiveXAddr()));
        }

        return foundProxy;
    }

    private GetContextStatesResponse requestContextStates(org.somda.sdc.proto.consumer.Consumer consumer)
            throws PrerequisitesException {
        throw new RuntimeException("requestContextStates() NOT IMPLEMENTED YET");
//        final HostedServiceProxy contextServiceProxy =
//                findHostedServiceProxy(hostingServiceProxy, WsdlConstants.PORT_TYPE_CONTEXT_QNAME);
//        try {
//            final SoapMessage getContextStatesResponseMessage =
//                    contextServiceProxy.getRequestResponseClient().sendRequestResponse(
//                            soapUtil.createMessage(ActionConstants.ACTION_GET_CONTEXT_STATES,
//                                    messageModelFactory.createGetContextStates()));
//            return soapUtil.getBody(getContextStatesResponseMessage,
//                    GetContextStatesResponse.class).orElseThrow(() -> new PrerequisitesException(
//                    "Remote endpoint did not send a GetContextStatesResponse message in response to " +
//                            String.format("a GetContextStates to service %s with physical address %s",
//                                    contextServiceProxy.getType().getServiceId(),
//                                    contextServiceProxy.getActiveEprAddress())));
//        } catch (MarshallingException | InterceptorException | TransportException | SoapFaultException e) {
//            throw new PrerequisitesException(String.format(
//                    "Could not send a GetContextStates request to service %s with physical address %s",
//                    contextServiceProxy.getType().getServiceId(), contextServiceProxy.getActiveEprAddress()), e);
//        }
    }

    @Subscribe
    void onConnectionLoss(WatchdogMessage watchdogMessage) {
        instanceLogger.info("Lost connection to device {}. Reason: {}", watchdogMessage.getPayload(),
                watchdogMessage.getReason().getMessage());
        disconnect(watchdogMessage.getPayload());
    }

    @Override
    protected void startUp() throws Exception {
        // nothing to do here
    }

    @Override
    protected void shutDown() throws Exception {
        instanceLogger.info("Shutting down, disconnecting all devices");
        List.copyOf(sdcRemoteDevices.keySet()).forEach(this::disconnect);
    }
}

