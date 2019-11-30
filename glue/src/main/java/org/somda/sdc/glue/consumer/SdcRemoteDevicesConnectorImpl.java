package org.somda.sdc.glue.consumer;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.biceps.common.storage.PreprocessingException;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.consumer.access.factory.RemoteMdibAccessFactory;
import org.somda.sdc.biceps.model.message.*;
import org.somda.sdc.biceps.model.participant.Mdib;
import org.somda.sdc.dpws.service.HostedServiceProxy;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.soap.interception.Interceptor;
import org.somda.sdc.dpws.soap.interception.InterceptorException;
import org.somda.sdc.dpws.soap.interception.NotificationObject;
import org.somda.sdc.dpws.soap.wseventing.SubscribeResult;
import org.somda.sdc.glue.common.*;
import org.somda.sdc.glue.common.factory.ModificationsBuilderFactory;
import org.somda.sdc.glue.consumer.event.RemoteDeviceConnectedMessage;
import org.somda.sdc.glue.consumer.factory.SdcRemoteDeviceFactory;
import org.somda.sdc.glue.consumer.report.ReportProcessingException;
import org.somda.sdc.glue.consumer.report.ReportProcessor;
import org.somda.sdc.glue.consumer.sco.ScoController;
import org.somda.sdc.glue.consumer.sco.factory.ScoControllerFactory;
import org.somda.sdc.glue.guice.Consumer;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.namespace.QName;
import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SdcRemoteDevicesConnectorImpl implements SdcRemoteDevicesConnector {
    private static final Logger LOG = LoggerFactory.getLogger(SdcRemoteDevicesConnectorImpl.class);

    private ListeningExecutorService executorService;
    private Map<URI, SdcRemoteDevice> sdcRemoteDevices;
    private EventBus eventBus;
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

    @Inject
    SdcRemoteDevicesConnectorImpl(@Consumer ListeningExecutorService executorService,
                                  ConcurrentHashMap sdcRemoteDevices,
                                  EventBus eventBus,
                                  Provider<ReportProcessor> reportProcessorProvider,
                                  ScoControllerFactory scoControllerFactory,
                                  @Named(ConsumerConfig.REQUESTED_EXPIRES) Duration requestedExpires,
                                  @Named(ConsumerConfig.RESPONSE_WAITING_TIME) Duration responseWaitingTime,
                                  SoapUtil soapUtil,
                                  ModificationsBuilderFactory modificationsBuilderFactory,
                                  RemoteMdibAccessFactory remoteMdibAccessFactory,
                                  ObjectFactory messageModelFactory,
                                  MdibVersionUtil mdibVersionUtil,
                                  SdcRemoteDeviceFactory sdcRemoteDeviceFactory) {
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
    }

    @Override
    public ListenableFuture<SdcRemoteDevice> connect(HostingServiceProxy hostingServiceProxy,
                                                     ConnectConfiguration connectConfiguration) throws PrerequisitesException {
        // precheck: necessary services present?
        checkRequiredServices(hostingServiceProxy, connectConfiguration.getRequiredPortTypes());

        return executorService.submit(() -> {
            try {
                ReportProcessor reportProcessor = createReportProcessor();
                RemoteMdibAccess mdibAccess = createRemoteMdibAccess(hostingServiceProxy);
                Optional<ScoController> scoController = createScoController(hostingServiceProxy);

                subscribeServices(hostingServiceProxy, connectConfiguration.getActions(), reportProcessor, scoController.orElse(null));

                GetContextStatesResponse getContextStatesResponse = null;
                if (mdibAccess.getContextStates().isEmpty()) {
                    getContextStatesResponse = requestContextStates(hostingServiceProxy);
                }

                try {
                    reportProcessor.startApplyingReportsOnMdib(mdibAccess, getContextStatesResponse);
                } catch (ReportProcessingException | PreprocessingException e) {
                    throw new PrerequisitesException("Could not start applying reports on remote MDIB access", e);
                }

                final SdcRemoteDevice sdcRemoteDevice = sdcRemoteDeviceFactory.createSdcRemoteDevice(
                        hostingServiceProxy,
                        mdibAccess,
                        reportProcessor,
                        scoController.orElse(null));
                sdcRemoteDevice.startAsync().awaitRunning();

                sdcRemoteDevices.put(hostingServiceProxy.getEndpointReferenceAddress(), sdcRemoteDevice);

                eventBus.post(new RemoteDeviceConnectedMessage(sdcRemoteDevice));

                return sdcRemoteDevice;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private ReportProcessor createReportProcessor() {
        return reportProcessorProvider.get();
    }

    private Optional<ScoController> createScoController(HostingServiceProxy hostingServiceProxy) {
        HostedServiceProxy contextServiceProxy = null;
        try {
            contextServiceProxy = findHostedServiceProxy(hostingServiceProxy, WsdlConstants.PORT_TYPE_CONTEXT_QNAME);
        } catch (PrerequisitesException e) {
            // ignore and proceed with empty context service
        }

        try {
            final HostedServiceProxy setServiceProxy = findHostedServiceProxy(hostingServiceProxy, WsdlConstants.PORT_TYPE_SET_QNAME);
            return Optional.of(scoControllerFactory.createScoController(hostingServiceProxy, setServiceProxy, contextServiceProxy));
        } catch (PrerequisitesException e) {
            return Optional.empty();
        }
    }

    @Override
    public void disconnect(URI eprAddress) {
        SdcRemoteDevice sdcRemoteDevice = sdcRemoteDevices.remove(eprAddress);
        if (sdcRemoteDevice != null) {
            // invalidate sdcRemoteDevice
            // unsubscribe everything

        }
    }

    @Override
    public Collection<SdcRemoteDevice> getConnectedDevices() {
        return new ArrayList<>(sdcRemoteDevices.values());
    }

    @Override
    public Optional<SdcRemoteDevice> getConnectedDevice(URI eprAddress) {
        return Optional.empty();
    }

    @Override
    public void registerObserver(SdcRemoteDevicesObserver observer) {
        eventBus.register(observer);
    }

    @Override
    public void unregisterObserver(SdcRemoteDevicesObserver observer) {
        eventBus.unregister(observer);
    }

    private void checkRequiredServices(HostingServiceProxy hostingServiceProxy,
                                       Collection<QName> requiredPortTypes) throws PrerequisitesException {
        List<QName> nonFoundPortTypes = new LinkedList<>(requiredPortTypes);
        hostingServiceProxy.getHostedServices().values().forEach(hostedServiceProxy ->
                nonFoundPortTypes.removeAll(hostedServiceProxy.getType().getTypes()));
        if (!nonFoundPortTypes.isEmpty()) {
            throw new PrerequisitesException(String.format("Required port types not found: ", nonFoundPortTypes));
        }
    }

    private void subscribeServices(HostingServiceProxy hostingServiceProxy,
                                   Collection<String> actionsToSubscribe,
                                   ReportProcessor reportProcessor,
                                   @Nullable ScoController scoController) throws PrerequisitesException {
        // Multimap<ServiceId, ActionUri>
        final Multimap<String, String> subscriptions = getServiceIdWithActionsToSubscribe(hostingServiceProxy, actionsToSubscribe);
        for (String serviceId : subscriptions.keySet()) {
            final Collection<String> actions = subscriptions.get(serviceId);
            if (actions.isEmpty()) {
                LOG.warn("Expect to find at least one action to subscribe for service id {}, but none found", serviceId);
                return;
            }
            final HostedServiceProxy hostedServiceProxy = hostingServiceProxy.getHostedServices().get(serviceId);
            if (hostedServiceProxy == null) {
                LOG.warn("Expect to found a hosted service proxy to access for service id {}, but none found", serviceId);
                return;
            }

            final ListenableFuture<SubscribeResult> subscribeResult = hostedServiceProxy.getEventSinkAccess().subscribe(
                    new ArrayList<>(actions),
                    requestedExpires,
                    new Interceptor() {
                        @Subscribe
                        void onNotification(NotificationObject notificationObject) {
                            final AbstractReport report = soapUtil.getBody(notificationObject.getNotification(),
                                    AbstractReport.class).orElseThrow(() -> new RuntimeException(
                                    String.format("Received unexpected report message from service %s", serviceId)));
                            if (report instanceof OperationInvokedReport) {
                                if (scoController != null) {
                                    scoController.processOperationInvokedReport((OperationInvokedReport) report);
                                }
                            } else {
                                reportProcessor.processReport(report);
                            }
                        }
                    });

            try {
                subscribeResult.get(responseWaitingTime.toSeconds(), TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                throw new PrerequisitesException(
                        String.format("Subscribe request towards service with service id %s failed. Physical target address: %s",
                                serviceId,
                                hostedServiceProxy.getActiveEprAddress().toString()), e);
            }
        }
    }

    private Multimap<String, String> getServiceIdWithActionsToSubscribe(HostingServiceProxy hostingServiceProxy, Collection<String> actionsToSubscribe) {
        Multimap<String, String> subscriptions = ArrayListMultimap.create();
        for (String action : actionsToSubscribe) {
            final QName targetPortType = SubscribableActionsMapping.TARGET_QNAMES.get(action);
            if (targetPortType == null) {
                LOG.warn("Found an action that could not be mapped to a target port type: {}", action);
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

    private RemoteMdibAccess createRemoteMdibAccess(HostingServiceProxy hostingServiceProxy) throws PrerequisitesException {
        // find get service
        final HostedServiceProxy getServiceProxy = findHostedServiceProxy(hostingServiceProxy, WsdlConstants.PORT_TYPE_GET_QNAME);

        final RemoteMdibAccess mdibAccess = remoteMdibAccessFactory.createRemoteMdibAccess();

        try {
            final SoapMessage getMdibResponseMessage = getServiceProxy.getRequestResponseClient().sendRequestResponse(
                    soapUtil.createMessage(ActionConstants.ACTION_GET_MDIB, messageModelFactory.createGetMdib()));
            final GetMdibResponse getMdibResponse = soapUtil.getBody(getMdibResponseMessage,
                    GetMdibResponse.class).orElseThrow(() -> new PrerequisitesException("Remote endpoint did not send a GetMdibResponse message in response to " +
                    String.format("a GetMdib to service %s with physical address %s",
                            getServiceProxy.getType().getServiceId(), getServiceProxy.getActiveEprAddress())));

            final Mdib mdib = getMdibResponse.getMdib();
            final ModificationsBuilder modBuilder = modificationsBuilderFactory.createModificationsBuilder(mdib);

            mdibAccess.writeDescription(
                    mdibVersionUtil.getMdibVersion(getMdibResponse),
                    mdib.getMdDescription().getDescriptionVersion(),
                    mdib.getMdState().getStateVersion(),
                    modBuilder.get());

        } catch (MarshallingException | InterceptorException | TransportException | SoapFaultException e) {
            throw new PrerequisitesException(String.format("Could not send a GetMdib request to service %s with physical address %s",
                    getServiceProxy.getType().getServiceId(), getServiceProxy.getActiveEprAddress()), e);
        } catch (PreprocessingException e) {
            throw new PrerequisitesException("Could not write initial MDIB to remote MDIB access", e);
        }

        return mdibAccess;
    }

    private HostedServiceProxy findHostedServiceProxy(HostingServiceProxy hostingServiceProxy, QName portType) throws PrerequisitesException {
        HostedServiceProxy foundProxy = null;
        for (HostedServiceProxy hostedServiceProxy : hostingServiceProxy.getHostedServices().values()) {
            if (hostedServiceProxy.getType().getTypes().contains(portType)) {
                foundProxy = hostedServiceProxy;
                break;
            }
        }

        if (foundProxy == null) {
            throw new PrerequisitesException(
                    String.format("Get service not found for remote device with UUID %s and " +
                                    "physical target address %s",
                            hostingServiceProxy.getEndpointReferenceAddress(),
                            hostingServiceProxy.getActiveXAddr()));

        }

        return foundProxy;
    }

    private GetContextStatesResponse requestContextStates(HostingServiceProxy hostingServiceProxy) throws PrerequisitesException {
        final HostedServiceProxy contextServiceProxy = findHostedServiceProxy(hostingServiceProxy, WsdlConstants.PORT_TYPE_CONTEXT_QNAME);
        try {
            final SoapMessage getContextStatesResponseMessage = contextServiceProxy.getRequestResponseClient().sendRequestResponse(
                    soapUtil.createMessage(ActionConstants.ACTION_GET_CONTEXT_STATES, messageModelFactory.createGetMdib()));
            final GetContextStatesResponse getContextStatesResponse = soapUtil.getBody(getContextStatesResponseMessage,
                    GetContextStatesResponse.class).orElseThrow(() -> new PrerequisitesException("Remote endpoint did not send a GetContextStatesResponse message in response to " +
                    String.format("a GetContextStates to service %s with physical address %s",
                            contextServiceProxy.getType().getServiceId(), contextServiceProxy.getActiveEprAddress())));
            return getContextStatesResponse;
        } catch (MarshallingException | InterceptorException | TransportException | SoapFaultException e) {
            throw new PrerequisitesException(String.format("Could not send a GetContextStates request to service %s with physical address %s",
                    contextServiceProxy.getType().getServiceId(), contextServiceProxy.getActiveEprAddress()), e);
        }
    }
}

