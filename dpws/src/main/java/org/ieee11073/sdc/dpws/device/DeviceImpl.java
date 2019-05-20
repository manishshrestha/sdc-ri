package org.ieee11073.sdc.dpws.device;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.Service;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.ieee11073.sdc.dpws.DiscoveryUdpQueue;
import org.ieee11073.sdc.dpws.DpwsConstants;
import org.ieee11073.sdc.dpws.device.helper.factory.DeviceHelperFactory;
import org.ieee11073.sdc.dpws.device.helper.ByteResourceHandler;
import org.ieee11073.sdc.dpws.device.helper.RequestResponseServerHttpHandler;
import org.ieee11073.sdc.dpws.device.helper.DiscoveryDeviceUdpMessageProcessor;
import org.ieee11073.sdc.dpws.helper.factory.DpwsHelperFactory;
import org.ieee11073.sdc.dpws.http.HttpServerRegistry;
import org.ieee11073.sdc.dpws.model.ThisDeviceType;
import org.ieee11073.sdc.dpws.model.ThisModelType;
import org.ieee11073.sdc.dpws.service.HostedService;
import org.ieee11073.sdc.dpws.service.HostedServiceInterceptor;
import org.ieee11073.sdc.dpws.service.HostingService;
import org.ieee11073.sdc.dpws.service.factory.HostedServiceFactory;
import org.ieee11073.sdc.dpws.service.factory.HostedServiceInterceptorFactory;
import org.ieee11073.sdc.dpws.service.factory.HostingServiceFactory;
import org.ieee11073.sdc.dpws.soap.NotificationSource;
import org.ieee11073.sdc.dpws.soap.RequestResponseServer;
import org.ieee11073.sdc.dpws.soap.SoapConstants;
import org.ieee11073.sdc.dpws.soap.SoapUtil;
import org.ieee11073.sdc.dpws.soap.factory.NotificationSourceFactory;
import org.ieee11073.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.ieee11073.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.WsDiscoveryTargetService;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.factory.WsDiscoveryTargetServiceFactory;
import org.ieee11073.sdc.dpws.soap.wseventing.EventSource;
import org.ieee11073.sdc.dpws.udp.UdpMessageQueueService;
import org.ieee11073.sdc.common.helper.StreamUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DeviceImpl extends AbstractIdleService implements Device, Service, DiscoveryAccess, HostingServiceAccess {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceImpl.class);

    private final WsDiscoveryTargetServiceFactory targetServiceFactory;
    private final Provider<DefaultDeviceConfiguration> defaultConfigProvider;
    private final WsAddressingUtil wsaUtil;
    private final NotificationSourceFactory notificationSourceFactory;
    private final DeviceHelperFactory deviceHelperFactory;
    private final DpwsHelperFactory dpwsHelperFactory;
    private final HostingServiceFactory hostingServiceFactory;
    private final HttpServerRegistry httpServerRegistry;
    private final SoapUtil soapUtil;
    private final RequestResponseServer wsdRequestResponseInterceptorChain;
    private final UdpMessageQueueService discoveryMessageQueue;
    private final RequestResponseServerHttpHandler reqResHandler;
    private final Provider<EventSource> eventSourceProvider;
    private final HostedServiceFactory hostedServiceFactory;
    private final HostedServiceInterceptorFactory hostedServiceInterceptorFactory;
    private final StreamUtil streamUtil;

    private DeviceConfiguration config;
    private WsDiscoveryTargetService wsdTargetService;
    private HostingService hostingService;
    private final List<HostedService> hostedServicesOnStartup;
    private List<URI> scopesOnStartup;
    private List<QName> typesOnStartup;
    private ThisDeviceType thisDeviceOnStartup;
    private ThisModelType thisModelOnStartup;
    private DiscoveryDeviceUdpMessageProcessor udpMsgProcessor;

    @Inject
    DeviceImpl(WsDiscoveryTargetServiceFactory targetServiceFactory,
               Provider<DefaultDeviceConfiguration> defaultConfigProvider,
               WsAddressingUtil wsaUtil,
               NotificationSourceFactory notificationSourceFactory,
               DeviceHelperFactory deviceHelperFactory,
               DpwsHelperFactory dpwsHelperFactory,
               RequestResponseServer wsdRequestResponseInterceptorChain,
               HostingServiceFactory hostingServiceFactory,
               HttpServerRegistry httpServerRegistry,
               SoapUtil soapUtil,
               RequestResponseServerHttpHandler reqResHandler,
               @DiscoveryUdpQueue UdpMessageQueueService discoveryMessageQueue,
               Provider<EventSource> eventSourceProvider,
               HostedServiceFactory hostedServiceFactory,
               HostedServiceInterceptorFactory hostedServiceInterceptorFactory,
               StreamUtil streamUtil) {
        this.targetServiceFactory = targetServiceFactory;
        this.defaultConfigProvider = defaultConfigProvider;
        this.wsaUtil = wsaUtil;
        this.notificationSourceFactory = notificationSourceFactory;
        this.deviceHelperFactory = deviceHelperFactory;
        this.dpwsHelperFactory = dpwsHelperFactory;
        this.hostingServiceFactory = hostingServiceFactory;
        this.httpServerRegistry = httpServerRegistry;
        this.soapUtil = soapUtil;
        this.wsdRequestResponseInterceptorChain = wsdRequestResponseInterceptorChain;
        this.discoveryMessageQueue = discoveryMessageQueue;
        this.reqResHandler = reqResHandler;
        this.eventSourceProvider = eventSourceProvider;
        this.hostedServiceFactory = hostedServiceFactory;
        this.hostedServiceInterceptorFactory = hostedServiceInterceptorFactory;
        this.streamUtil = streamUtil;
        this.hostedServicesOnStartup = new ArrayList<>();
    }

    @Override
    protected void startUp() throws Exception {
        if (config == null) {
            LOG.warn("No device configuration found. Use default.");
            config = defaultConfigProvider.get();
        }

        EndpointReferenceType deviceEpr = config.getEndpointReference();
        LOG.info("Start device with URN '{}'.", deviceEpr.getAddress().getValue());

        /*
         * Configure WS-Discovery
         */

        // Bind notification source to discovery message queue to allow the target service to send notifications
        NotificationSource wsdNotificationSource = notificationSourceFactory.createNotificationSource(
                dpwsHelperFactory.createNotificationSourceUdpCallback(discoveryMessageQueue));

        URI uniqueEprAddress = wsaUtil.getAddressUri(config.getEndpointReference()).orElseThrow(() ->
                new RuntimeException("No valid endpoint reference found in device config."));
        String hostingServerCtxtPath = buildContextPathBase(uniqueEprAddress);
        // Create WS-Discovery target service
        wsdTargetService = targetServiceFactory.createWsDiscoveryTargetService(deviceEpr, wsdNotificationSource);
        wsdTargetService.setXAddrs(config.getHostingServiceBindings().parallelStream()
                .map(uri -> uri.toString() + hostingServerCtxtPath)
                .collect(Collectors.toList()));

        // Register target service to a request response server interceptor chain
        wsdRequestResponseInterceptorChain.register(wsdTargetService);

        // Create broker for request response server with UDP messages
        udpMsgProcessor = deviceHelperFactory
                .createDiscoveryDeviceUdpMessageProcessor(wsdRequestResponseInterceptorChain, discoveryMessageQueue);
        // Bind request response server to discovery message queue to get notified on incoming UDP messages
        discoveryMessageQueue.registerUdpMessageQueueObserver(udpMsgProcessor);



        /*
         * Configure Hosting Service
         */

        // Register HTTP bindings to HTTP context registry; append EPR UUID from host as context path
        config.getHostingServiceBindings().forEach(uri -> httpServerRegistry.registerContext(uri.getHost(),
                uri.getPort(), "/" + soapUtil.createUuidFromUri(wsaUtil.getAddressUri(deviceEpr).get()).toString(),
                reqResHandler));
        // Create hosting service
        hostingService = hostingServiceFactory.createHostingService(wsdTargetService);
        // Register request-response hosting service interceptor to receive incoming request-response messages
        reqResHandler.register(hostingService);
        // Allow WS-Discovery to react on incoming HTTP requests
        reqResHandler.register(wsdTargetService);

        hostedServicesOnStartup.forEach(this::addHostedServiceToHostingService);
        hostingService.getHostedServices().forEach(hostedService ->
                hostedService.getWebService().getEventSource().startAsync().awaitRunning());
        Optional.ofNullable(thisDeviceOnStartup).ifPresent(thisDeviceType ->
                hostingService.setThisDevice(thisDeviceType));
        Optional.ofNullable(thisModelOnStartup).ifPresent(thisModelType ->
                hostingService.setThisModel(thisModelType));
        Optional.ofNullable(typesOnStartup).ifPresent(qNames ->
                wsdTargetService.setTypes(qNames));
        Optional.ofNullable(scopesOnStartup).ifPresent(uris ->
                wsdTargetService.setScopes(scopesAsStrs(uris)));

        LOG.info("Device {} is running.", hostingService);

        wsdTargetService.sendHello();
    }

    @Override
    protected void shutDown() throws Exception {
        LOG.info("Shut down device {}.", hostingService);
        wsdTargetService.sendBye();
        hostingService.getHostedServices().forEach(hostedService ->
                hostedService.getWebService().getEventSource().stopAsync().awaitTerminated());
        httpServerRegistry.stopAsync().awaitTerminated();
        discoveryMessageQueue.unregisterUdpMessageQueueObserver(udpMsgProcessor);
        LOG.info("Device {} shut down.", hostingService);
    }

    @Override
    public void setConfiguration(DeviceConfiguration deviceConfiguration) {
        this.config = deviceConfiguration;
    }

    @Override
    public DiscoveryAccess getDiscoveryAccess() {
        return this;
    }

    @Override
    public HostingServiceAccess getHostingServiceAccess() {
        return this;
    }

    private void checkRunning() {
        if (!isRunning()) {
            throw new IllegalStateException("Device is not running.");
        }
    }

    @Override
    public void setTypes(List<QName> types) {
        ArrayList<QName> tmpTypes = new ArrayList<>();
        tmpTypes.add(DpwsConstants.DEVICE_TYPE);
        tmpTypes.addAll(types);
        if (isRunning()) {
            wsdTargetService.setTypes(tmpTypes);
        } else {
            typesOnStartup = tmpTypes;
        }
    }

    @Override
    public void setScopes(List<URI> scopes) {
        if (isRunning()) {
            wsdTargetService.setScopes(scopesAsStrs(scopes));
        } else {
            scopesOnStartup = scopes;
        }
    }

    private List<String> scopesAsStrs(List<URI> scopes) {
        return scopes.parallelStream().map(URI::toString).collect(Collectors.toList());
    }

    @Override
    public void setThisDevice(ThisDeviceType thisDevice) {
        if (isRunning()) {
            hostingService.setThisDevice(thisDevice);
        } else {
            thisDeviceOnStartup = thisDevice;
        }

    }

    @Override
    public void setThisModel(ThisModelType thisModel) {
        if (isRunning()) {
            hostingService.setThisModel(thisModel);
        } else {
            thisModelOnStartup = thisModel;
        }
    }

    @Override
    public void addHostedService(HostedService hostedService) {
        if (isRunning()) {
            addHostedServiceToHostingService(hostedService);
        } else {
            hostedServicesOnStartup.add(hostedService);
        }
    }

    private void addHostedServiceToHostingService(HostedService hostedService) {
        // Create event source
        EventSource eventSource = eventSourceProvider.get();
        // Inject event source to Web Service
        hostedService.getWebService().setEventSource(eventSource);
        // Add event source to HTTP req-res-server for event source management
        reqResHandler.register(eventSource);

        // Create Web Service access path
        String contextPathPart = buildContextPathPart(hostedService.getType().getServiceId());

        // If no EPR addresses are given already, create one/some from hosting service
        if (hostedService.getType().getEndpointReference().isEmpty()) {
            List<URI> uris = hostingService.getXAddrs().parallelStream()
                    .map(uri -> URI.create(uri.toString() + contextPathPart))
                    .collect(Collectors.toList());

            hostedService = hostedServiceFactory.createHostedService(hostedService.getType().getServiceId(),
                    hostedService.getType().getTypes(), uris, hostedService.getWebService(),
                    hostedService.getWsdlDocument());
        }

        // Make given WSDL document accessible through HTTP
        String contextPath = buildContextPathBase(hostingService.getEndpointReferenceAddress()) + contextPathPart;
        String wsdlContextPath = contextPath + "/wsdl";

        // Retrieve WSDL document bytes
        byte[] tmpWsdlDocBytes;
        try {
            tmpWsdlDocBytes = streamUtil.getByteArrayFromInputStream(hostedService.getWsdlDocument());
        } catch (IOException e) {
            LOG.warn("Could not add hosted service properly. IO exception while requesting WSDL document stream.", e);
            return;
        }

        // Make WSDL document bytes available as HTTP resource
        final byte[] wsdlDocBytes = tmpWsdlDocBytes;
        for (EndpointReferenceType epr : hostedService.getType().getEndpointReference()) {
            if (wsdlDocBytes.length == 0) {
                throw new RuntimeException("Empty WSDL document detected.");
            }
            URI uri = wsaUtil.getAddressUri(epr).orElseThrow(() ->
                    new RuntimeException("Invalid EPR detected when trying to add hosted service."));
            httpServerRegistry.registerContext(uri.getHost(), uri.getPort(), contextPath, reqResHandler);
            URI wsdlLocation = httpServerRegistry.registerContext(uri.getHost(), uri.getPort(), wsdlContextPath,
                    SoapConstants.MEDIA_TYPE_WSDL, new ByteResourceHandler(wsdlDocBytes));
            hostedService.getWsdlLocations().add(wsdlLocation);
        }

        // Create hosted service interceptor to access GetMetadata requests
        HostedServiceInterceptor hsInterceptor = hostedServiceInterceptorFactory.createHostedServiceInterceptor(
                hostedService, wsdTargetService);
        // Register interceptor at HTTP req-res-handler
        reqResHandler.register(hsInterceptor);

        // Register Web Service interceptor at HTTP req-res-handler
        reqResHandler.register(hostedService.getWebService());

        // Add hosted service to hosting service to get metadata descriptions updated
        hostingService.addHostedService(hostedService);

        // @todo Send out Hello with metadata version increment
    }

    private String buildContextPathPart(String serviceId) {
        return "/" + serviceId;
    }

    private String buildContextPathBase(URI uuidUri) {
        return "/" + soapUtil.createUuidFromUri(uuidUri).toString();
    }
}
