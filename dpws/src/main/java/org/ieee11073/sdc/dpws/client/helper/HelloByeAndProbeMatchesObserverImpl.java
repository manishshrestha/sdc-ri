package org.ieee11073.sdc.dpws.client.helper;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.ieee11073.sdc.dpws.client.*;
import org.ieee11073.sdc.dpws.guice.NetworkJobThreadPool;
import org.ieee11073.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Optional;

/**
 * Helper class to forward Hello, Bye, ProbeMatches, and ProbeTimeout events.
 */
public class HelloByeAndProbeMatchesObserverImpl implements HelloByeAndProbeMatchesObserver {
    private static final Logger LOG = LoggerFactory.getLogger(HelloByeAndProbeMatchesObserverImpl.class);

    private final DiscoveredDeviceResolver discoveredDeviceResolver;
    private final ListeningExecutorService networkJobExecutor;
    private final WsAddressingUtil wsaUtil;
    private final EventBus discoveryBus;

    @Inject
    HelloByeAndProbeMatchesObserverImpl(@Assisted DiscoveredDeviceResolver discoveredDeviceResolver,
                                        @NetworkJobThreadPool ListeningExecutorService networkJobExecutor,
                                        WsAddressingUtil wsaUtil) {
        this.discoveredDeviceResolver = discoveredDeviceResolver;
        this.networkJobExecutor = networkJobExecutor;
        this.wsaUtil = wsaUtil;
        this.discoveryBus = new EventBus();
    }

    public void registerDiscoveryObserver(org.ieee11073.sdc.dpws.client.DiscoveryObserver observer) {
        discoveryBus.register(observer);
    }

    public void unregisterDiscoveryObserver(org.ieee11073.sdc.dpws.client.DiscoveryObserver observer) {
        discoveryBus.unregister(observer);
    }

    public void publishDeviceLeft(URI deviceUuid, DeviceLeftMessage.TriggerType triggerType) {
        discoveryBus.post(new DeviceLeftMessage(deviceUuid, triggerType));
    }

    @Subscribe
    void onHello(HelloMessage helloMessage) {
        ListenableFuture<Optional<DiscoveredDevice>> future = networkJobExecutor.submit(() ->
                discoveredDeviceResolver.resolve(helloMessage));
        Futures.addCallback(future, new FutureCallback<Optional<DiscoveredDevice>>() {
            @Override
            public void onSuccess(@Nullable Optional<DiscoveredDevice> discoveredDevice) {
                if (discoveredDevice == null) {
                    LOG.warn("{} delivered null pointer", DiscoveredDeviceResolver.class);
                    return;
                }

                discoveredDevice.ifPresent(dp -> discoveryBus.post(new DeviceEnteredMessage(dp)));
            }

            @Override
            public void onFailure(Throwable throwable) {
                // nothing to do here - log messages were created by DiscoveredDeviceResolver helper
            }
        }, networkJobExecutor);
    }

    @Subscribe
    void onBye(ByeMessage byeMessage) {
        wsaUtil.getAddressUri(byeMessage.getPayload().getEndpointReference()).ifPresent(uri ->
                discoveryBus.post(new DeviceLeftMessage(uri, DeviceLeftMessage.TriggerType.BYE)));
    }

    /**
     * Receive probe matches message and resolve before forwarding event.
     */
    @Subscribe
    void onProbeMatches(ProbeMatchesMessage probeMatchesMessage) {
        ListenableFuture<Optional<DiscoveredDevice>> future = networkJobExecutor.submit(() ->
                discoveredDeviceResolver.resolve(probeMatchesMessage));
        Futures.addCallback(future, new FutureCallback<Optional<DiscoveredDevice>>() {
            @Override
            public void onSuccess(@Nullable Optional<DiscoveredDevice> discoveredDevice) {
                if (discoveredDevice == null) {
                    LOG.warn("{} delivered null pointer", DiscoveredDeviceResolver.class);
                    return;
                }

                discoveredDevice.ifPresent(dp -> discoveryBus.post(new ProbedDeviceFoundMessage(dp,
                        probeMatchesMessage.getProbeRequestId())));
            }

            @Override
            public void onFailure(Throwable throwable) {
                // nothing to do here - log messages were created by DiscoveredDeviceResolver helper
            }
        }, networkJobExecutor);
    }

    @Subscribe
    void onProbeTimeout(ProbeTimeoutMessage probeTimeoutMessage) {
        discoveryBus.post(new DeviceProbeTimeoutMessage(probeTimeoutMessage.getProbeMatchesCount(),
                probeTimeoutMessage.getProbeRequestId()));
    }
}
