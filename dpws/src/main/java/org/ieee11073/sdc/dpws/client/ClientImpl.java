package org.ieee11073.sdc.dpws.client;

import com.google.common.util.concurrent.*;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.ieee11073.sdc.dpws.DiscoveryUdpQueue;
import org.ieee11073.sdc.dpws.client.helper.*;
import org.ieee11073.sdc.dpws.client.helper.factory.ClientHelperFactory;
import org.ieee11073.sdc.dpws.guice.NetworkJobThreadPool;
import org.ieee11073.sdc.dpws.helper.NotificationSourceUdpCallback;
import org.ieee11073.sdc.dpws.helper.factory.DpwsHelperFactory;
import org.ieee11073.sdc.dpws.service.HostingServiceProxy;
import org.ieee11073.sdc.dpws.soap.NotificationSink;
import org.ieee11073.sdc.dpws.soap.NotificationSource;
import org.ieee11073.sdc.dpws.soap.exception.MarshallingException;
import org.ieee11073.sdc.dpws.soap.exception.TransportException;
import org.ieee11073.sdc.dpws.soap.factory.NotificationSourceFactory;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.HelloByeAndProbeMatchesObserver;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.WsDiscoveryClient;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.factory.WsDiscoveryClientFactory;
import org.ieee11073.sdc.dpws.udp.UdpMessageQueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.URI;

/**
 * Default implementation of {@link Client}.
 */
public class ClientImpl extends AbstractIdleService implements Client, Service, HelloByeAndProbeMatchesObserver {
    private static final Logger LOG = LoggerFactory.getLogger(ClientImpl.class);

    private final Boolean enableWatchdog;
    private final UdpMessageQueueService discoveryMessageQueue;
    private final HostingServiceResolver hostingServiceResolver;
    private final HostingServiceRegistry hostingServiceRegistry;
    private final DiscoveryClientUdpProcessor msgProcessor;
    private final DeviceProxyObserver deviceProxyObserver;
    private final WatchDog watchdog;
    private final WsDiscoveryClient wsDiscoveryClient;
    private final ListeningExecutorService executorService;

    @Inject
    ClientImpl(@Named(ClientConfig.ENABLE_WATCHDOG) Boolean enableWatchdog,
               WsDiscoveryClientFactory discoveryClientFactory,
               NotificationSourceFactory notificationSourceFactory,
               DpwsHelperFactory dpwsHelperFactory,
               @DiscoveryUdpQueue UdpMessageQueueService discoveryMessageQueue,
               NotificationSink notificationSink,
               ClientHelperFactory clientHelperFactory,
               HostingServiceRegistry hostingServiceRegistry,
               @NetworkJobThreadPool ListeningExecutorService executorService) {
        this.enableWatchdog = enableWatchdog;
        this.discoveryMessageQueue = discoveryMessageQueue;
        this.hostingServiceRegistry = hostingServiceRegistry;
        this.hostingServiceResolver = clientHelperFactory.createHostingServiceResolver(hostingServiceRegistry);
        this.executorService = executorService;

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
        DeviceProxyResolver deviceProxyResolver = clientHelperFactory.createDeviceProxyResolver(wsDiscoveryClient);

        // Create observer for WS-Discovery messages
        deviceProxyObserver = clientHelperFactory.createDeviceProxyObserver(deviceProxyResolver);

        watchdog = clientHelperFactory.createWatchdog(wsDiscoveryClient, hostingServiceProxy -> {
            URI deviceUuid = hostingServiceProxy.getEndpointReferenceAddress();
            disconnectHostingService(deviceUuid);
            deviceProxyObserver.publishDeviceLeft(deviceUuid, DeviceLeftMessage.TriggerType.WATCHDOG);
        });
    }

    @Override
    public void probe(DiscoveryFilter discoveryFilter) {
        checkRunning();

        try {
            wsDiscoveryClient.sendProbe(discoveryFilter.getDiscoveryId(), discoveryFilter.getTypes(),
                    discoveryFilter.getScopes());
        } catch (MarshallingException e) {
            LOG.info("Marshalling failed while probing for devices", e.getCause());
        } catch (TransportException e) {
            LOG.info("Message transmission failed while probing for devices", e.getCause());
        }
    }

    @Override
    public ListenableFuture<HostingServiceProxy> connectHostingService(DeviceProxy deviceProxy) {
        checkRunning();
        ListenableFuture<HostingServiceProxy> future = hostingServiceResolver.resolveHostingService(deviceProxy);

        if (enableWatchdog) {
            Futures.addCallback(future, new FutureCallback<HostingServiceProxy>() {
                @Override
                public void onSuccess(@Nullable HostingServiceProxy hostingServiceProxy) {
                    if (hostingServiceProxy != null) {
                        watchdog.inspect(hostingServiceProxy);
                    }
                }

                @Override
                public void onFailure(Throwable throwable) {
                    // Ignore failures
                }
            }, executorService);
        }

        return future;
    }

    @Override
    public void disconnectHostingService(URI deviceUuid) {
        checkRunning();
        hostingServiceRegistry.unregister(deviceUuid);
    }

    @Override
    protected void startUp() throws Exception {
        discoveryMessageQueue.registerUdpMessageQueueObserver(msgProcessor);
        wsDiscoveryClient.registerHelloByeAndProbeMatchesObserver(deviceProxyObserver);
    }

    @Override
    protected void shutDown() throws Exception {
        wsDiscoveryClient.unregisterHelloByeAndProbeMatchesObserver(deviceProxyObserver);
        discoveryMessageQueue.unregisterUdpMessageQueueObserver(msgProcessor);
    }


    @Override
    public void registerDiscoveryObserver(DiscoveryObserver observer) {
        deviceProxyObserver.registerDiscoveryObserver(observer);
    }

    @Override
    public void unregisterDiscoveryObserver(DiscoveryObserver observer) {
        deviceProxyObserver.unregisterDiscoveryObserver(observer);
    }

    private void checkRunning() {
        if (!isRunning()) {
            String msg = "Try to invoke method on non-running client.";
            LOG.warn(msg);
            throw new RuntimeException(msg);
        }
    }
}
