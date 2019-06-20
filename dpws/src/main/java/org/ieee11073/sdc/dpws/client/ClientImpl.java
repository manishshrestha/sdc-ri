package org.ieee11073.sdc.dpws.client;

import com.google.common.util.concurrent.*;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.ieee11073.sdc.dpws.DiscoveryUdpQueue;
import org.ieee11073.sdc.dpws.DpwsConfig;
import org.ieee11073.sdc.dpws.TransportBinding;
import org.ieee11073.sdc.dpws.client.helper.*;
import org.ieee11073.sdc.dpws.client.helper.factory.ClientHelperFactory;
import org.ieee11073.sdc.dpws.factory.TransportBindingFactory;
import org.ieee11073.sdc.dpws.guice.NetworkJobThreadPool;
import org.ieee11073.sdc.dpws.helper.NotificationSourceUdpCallback;
import org.ieee11073.sdc.dpws.helper.factory.DpwsHelperFactory;
import org.ieee11073.sdc.dpws.service.HostingServiceProxy;
import org.ieee11073.sdc.dpws.soap.NotificationSink;
import org.ieee11073.sdc.dpws.soap.NotificationSource;
import org.ieee11073.sdc.dpws.soap.RequestResponseClient;
import org.ieee11073.sdc.dpws.soap.exception.MarshallingException;
import org.ieee11073.sdc.dpws.soap.exception.TransportException;
import org.ieee11073.sdc.dpws.soap.factory.NotificationSourceFactory;
import org.ieee11073.sdc.dpws.soap.factory.RequestResponseClientFactory;
import org.ieee11073.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.HelloByeAndProbeMatchesObserver;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.WsDiscoveryClient;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.factory.WsDiscoveryClientFactory;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.model.ProbeMatchesType;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.model.ResolveMatchType;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.model.ResolveMatchesType;
import org.ieee11073.sdc.dpws.udp.UdpMessageQueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.URI;
import java.time.Duration;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Default implementation of {@link Client}.
 */
public class ClientImpl extends AbstractIdleService implements Client, Service, HelloByeAndProbeMatchesObserver {
    private static final Logger LOG = LoggerFactory.getLogger(ClientImpl.class);

    private final UdpMessageQueueService discoveryMessageQueue;
    private final HostingServiceResolver hostingServiceResolver;
    private final DiscoveryClientUdpProcessor msgProcessor;
    private final HelloByeAndProbeMatchesObserverImpl helloByeAndProbeMatchesObserverImpl;
    private final WsDiscoveryClient wsDiscoveryClient;
    private final ListeningExecutorService executorService;
    private final WsAddressingUtil wsAddressingUtil;
    private final TransportBindingFactory transportBindingFactory;
    private final RequestResponseClientFactory requestResponseClientFactory;
    private final Duration maxWaitForFutures;

    @Inject
    ClientImpl(@Named(DpwsConfig.MAX_WAIT_FOR_FUTURES) Duration maxWaitForFutures,
               WsDiscoveryClientFactory discoveryClientFactory,
               NotificationSourceFactory notificationSourceFactory,
               DpwsHelperFactory dpwsHelperFactory,
               @DiscoveryUdpQueue UdpMessageQueueService discoveryMessageQueue,
               NotificationSink notificationSink,
               ClientHelperFactory clientHelperFactory,
               @NetworkJobThreadPool ListeningExecutorService executorService,
               WsAddressingUtil wsAddressingUtil,
               TransportBindingFactory transportBindingFactory,
               RequestResponseClientFactory requestResponseClientFactory,
               HostingServiceResolver hostingServiceResolver) {
        this.maxWaitForFutures = maxWaitForFutures;
        this.discoveryMessageQueue = discoveryMessageQueue;
        this.hostingServiceResolver = hostingServiceResolver;
        this.executorService = executorService;
        this.wsAddressingUtil = wsAddressingUtil;
        this.transportBindingFactory = transportBindingFactory;
        this.requestResponseClientFactory = requestResponseClientFactory;

        // Create binding between a notification source and outgoing UDP messages to send probes and resolves
        NotificationSourceUdpCallback callback =
                dpwsHelperFactory.createNotificationSourceUdpCallback(discoveryMessageQueue);
        // Bind the callback to a notification source
        NotificationSource notificationSource = notificationSourceFactory.createNotificationSource(callback);
        // Connect that notification source to a WS-Discovery client
        wsDiscoveryClient = discoveryClientFactory.createWsDiscoveryClient(notificationSource);

        // Create binding between a notification sink and incoming UDP messages to receive hello, bye, probeMatches and
        // resolveMatches
        msgProcessor = clientHelperFactory.createDiscoveryClientUdpProcessor(notificationSink);

        // Bind the notification sink to the WS-Discovery client that was created before
        notificationSink.register(wsDiscoveryClient);

        // Create device resolver proxy helper object
        DiscoveredDeviceResolver discoveredDeviceResolver = clientHelperFactory.createDiscoveredDeviceResolver(wsDiscoveryClient);

        // Create observer for WS-Discovery messages
        helloByeAndProbeMatchesObserverImpl = clientHelperFactory.createDiscoveryObserver(discoveredDeviceResolver);
    }

    @Override
    public void probe(DiscoveryFilter discoveryFilter) throws TransportException {
        checkRunning();

        try {
            wsDiscoveryClient.sendProbe(discoveryFilter.getDiscoveryId(), discoveryFilter.getTypes(),
                    discoveryFilter.getScopes());
        } catch (MarshallingException e) {
            LOG.warn("Marshalling failed while probing for devices", e.getCause());
        }
    }

    @Override
    public ListenableFuture<ProbeMatchesType> directedProbe(URI xAddr) {
        checkRunning();

        TransportBinding tBinding = transportBindingFactory.createTransportBinding(xAddr);
        RequestResponseClient rrc = requestResponseClientFactory.createRequestResponseClient(tBinding);
        return wsDiscoveryClient.sendDirectedProbe(rrc, new ArrayList<>(), new ArrayList<>());
    }

    @Override
    public ListenableFuture<DiscoveredDevice> resolve(URI eprAddress) {
        checkRunning();

        try {
            final SettableFuture<DiscoveredDevice> deviceSettableFuture = SettableFuture.create();
            final ListenableFuture<ResolveMatchesType> resolveMatchesFuture = wsDiscoveryClient
                    .sendResolve(wsAddressingUtil.createEprWithAddress(eprAddress));
            Futures.addCallback(resolveMatchesFuture, new FutureCallback<ResolveMatchesType>() {
                @Override
                public void onSuccess(@Nullable ResolveMatchesType resolveMatchesType) {
                    if (resolveMatchesType == null) {
                        LOG.warn("Received ResolveMatches with empty payload.");
                    } else {
                        final ResolveMatchType rm = resolveMatchesType.getResolveMatch();
                        deviceSettableFuture.set(new DiscoveredDevice(
                                URI.create(rm.getEndpointReference().getAddress().getValue()),
                                rm.getTypes(),
                                rm.getScopes().getValue(),
                                rm.getXAddrs(),
                                rm.getMetadataVersion()));
                    }
                }

                @Override
                public void onFailure(Throwable throwable) {
                    deviceSettableFuture.setException(throwable);
                }
            }, executorService);

            return deviceSettableFuture;
        } catch (MarshallingException e) {
            LOG.warn("Marshalling failed while probing for devices", e.getCause());
            final SettableFuture<DiscoveredDevice> errorFuture = SettableFuture.create();
            errorFuture.setException(e);
            return errorFuture;
        } catch (TransportException e) {
            LOG.warn("Sending failed on transport layer", e.getCause());
            final SettableFuture<DiscoveredDevice> errorFuture = SettableFuture.create();
            errorFuture.setException(e);
            return errorFuture;
        }
    }

    @Override
    public ListenableFuture<HostingServiceProxy> connect(DiscoveredDevice discoveredDevice) {
        checkRunning();
        final ListenableFuture<HostingServiceProxy> future = hostingServiceResolver.resolveHostingService(discoveredDevice);

//        if (enableWatchdog) {
//            Futures.addCallback(future, new FutureCallback<HostingServiceProxy>() {
//                @Override
//                public void onSuccess(@Nullable HostingServiceProxy hostingServiceProxy) {
//                    if (hostingServiceProxy != null) {
//                        watchdog.inspect(hostingServiceProxy);
//                    }
//                }
//
//                @Override
//                public void onFailure(Throwable throwable) {
//                    // Ignore failures
//                }
//            }, executorService);
//        }

        return future;
    }

    @Override
    public ListenableFuture<HostingServiceProxy> connect(URI eprAddress) {
        checkRunning();

        final ListenableFuture<DiscoveredDevice> resolveFuture = resolve(eprAddress);
        final SettableFuture<HostingServiceProxy> hspFuture = SettableFuture.create();

        Futures.addCallback(resolveFuture, new FutureCallback<DiscoveredDevice>() {
            @Override
            public void onSuccess(@Nullable DiscoveredDevice discoveredDevice) {
                if (discoveredDevice == null) {
                    throw new RuntimeException(String.format("Resolve of %s failed.", eprAddress));
                }

                try {
                    hspFuture.set(connect(discoveredDevice).get(maxWaitForFutures.toMillis(), TimeUnit.MILLISECONDS));
                } catch (Exception e) {
                    throw new RuntimeException(String.format("Connect of %s failed.", eprAddress));
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                hspFuture.setException(throwable);
            }
        }, executorService);

        return hspFuture;
    }

    @Override
    protected void startUp() {
        discoveryMessageQueue.registerUdpMessageQueueObserver(msgProcessor);
        wsDiscoveryClient.registerHelloByeAndProbeMatchesObserver(helloByeAndProbeMatchesObserverImpl);
    }

    @Override
    protected void shutDown() {
        wsDiscoveryClient.unregisterHelloByeAndProbeMatchesObserver(helloByeAndProbeMatchesObserverImpl);
        discoveryMessageQueue.unregisterUdpMessageQueueObserver(msgProcessor);
    }


    @Override
    public void registerDiscoveryObserver(DiscoveryObserver observer) {
        helloByeAndProbeMatchesObserverImpl.registerDiscoveryObserver(observer);
    }

    @Override
    public void unregisterDiscoveryObserver(DiscoveryObserver observer) {
        helloByeAndProbeMatchesObserverImpl.unregisterDiscoveryObserver(observer);
    }

    private void checkRunning() {
        if (!isRunning()) {
            String msg = "Try to invoke method on non-running client.";
            LOG.warn(msg);
            throw new RuntimeException(msg);
        }
    }
}
