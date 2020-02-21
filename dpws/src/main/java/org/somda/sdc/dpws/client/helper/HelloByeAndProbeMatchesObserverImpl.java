package org.somda.sdc.dpws.client.helper;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.dpws.client.DiscoveredDevice;
import org.somda.sdc.dpws.client.event.DeviceEnteredMessage;
import org.somda.sdc.dpws.client.event.DeviceLeftMessage;
import org.somda.sdc.dpws.client.event.DeviceProbeTimeoutMessage;
import org.somda.sdc.dpws.client.event.ProbedDeviceFoundMessage;
import org.somda.sdc.dpws.guice.NetworkJobThreadPool;
import org.somda.sdc.dpws.helper.ExecutorWrapperService;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsdiscovery.HelloByeAndProbeMatchesObserver;
import org.somda.sdc.dpws.soap.wsdiscovery.event.ByeMessage;
import org.somda.sdc.dpws.soap.wsdiscovery.event.HelloMessage;
import org.somda.sdc.dpws.soap.wsdiscovery.event.ProbeMatchesMessage;
import org.somda.sdc.dpws.soap.wsdiscovery.event.ProbeTimeoutMessage;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Optional;

/**
 * Helper class to forward Hello, Bye, ProbeMatches, and ProbeTimeout events.
 */
public class HelloByeAndProbeMatchesObserverImpl implements HelloByeAndProbeMatchesObserver {
    private static final Logger LOG = LoggerFactory.getLogger(HelloByeAndProbeMatchesObserverImpl.class);

    private final DiscoveredDeviceResolver discoveredDeviceResolver;
    private final ExecutorWrapperService<ListeningExecutorService> networkJobExecutor;
    private final WsAddressingUtil wsaUtil;
    private final EventBus discoveryBus;

    @Inject
    HelloByeAndProbeMatchesObserverImpl(@Assisted DiscoveredDeviceResolver discoveredDeviceResolver,
                                        @NetworkJobThreadPool ExecutorWrapperService<ListeningExecutorService> networkJobExecutor,
                                        WsAddressingUtil wsaUtil) {
        this.discoveredDeviceResolver = discoveredDeviceResolver;
        this.networkJobExecutor = networkJobExecutor;
        this.wsaUtil = wsaUtil;
        this.discoveryBus = new EventBus();
    }

    public void registerDiscoveryObserver(org.somda.sdc.dpws.client.DiscoveryObserver observer) {
        discoveryBus.register(observer);
    }

    public void unregisterDiscoveryObserver(org.somda.sdc.dpws.client.DiscoveryObserver observer) {
        discoveryBus.unregister(observer);
    }

    public void publishDeviceLeft(URI deviceUuid, DeviceLeftMessage.TriggeredBy triggeredBy) {
        discoveryBus.post(new DeviceLeftMessage(deviceUuid, triggeredBy));
    }

    @Subscribe
    void onHello(HelloMessage helloMessage) {
        ListenableFuture<Optional<DiscoveredDevice>> future = networkJobExecutor.getExecutorService().submit(() ->
                discoveredDeviceResolver.resolve(helloMessage));
        Futures.addCallback(future, new FutureCallback<>() {
            @SuppressWarnings("OptionalAssignedToNull")
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
                LOG.trace("Error while processing Hello message.", throwable);
            }
        }, networkJobExecutor.getExecutorService());
    }

    @Subscribe
    void onBye(ByeMessage byeMessage) {
        wsaUtil.getAddressUri(byeMessage.getPayload().getEndpointReference()).ifPresent(uri ->
                discoveryBus.post(new DeviceLeftMessage(uri, DeviceLeftMessage.TriggeredBy.BYE)));
    }

    /**
     * Receive probe matches message and resolve before forwarding event.
     */
    @Subscribe
    void onProbeMatches(ProbeMatchesMessage probeMatchesMessage) {
        ListenableFuture<Optional<DiscoveredDevice>> future = networkJobExecutor.getExecutorService().submit(() ->
                discoveredDeviceResolver.resolve(probeMatchesMessage));
        Futures.addCallback(future, new FutureCallback<>() {
            @SuppressWarnings("OptionalAssignedToNull")
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
                // nothing to do here - log messages were created by DiscoveredDeviceResolver util
            }
        }, networkJobExecutor.getExecutorService());
    }

    @Subscribe
    void onProbeTimeout(ProbeTimeoutMessage probeTimeoutMessage) {
        discoveryBus.post(new DeviceProbeTimeoutMessage(probeTimeoutMessage.getProbeMatchesCount(),
                probeTimeoutMessage.getProbeRequestId()));
    }
}
