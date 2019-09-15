package org.ieee11073.sdc.dpws.client.helper.factory;

import com.google.inject.assistedinject.Assisted;
import org.ieee11073.sdc.dpws.client.helper.DiscoveredDeviceResolver;
import org.ieee11073.sdc.dpws.client.helper.DiscoveryClientUdpProcessor;
import org.ieee11073.sdc.dpws.client.helper.HelloByeAndProbeMatchesObserverImpl;
import org.ieee11073.sdc.dpws.client.helper.WatchDog;
import org.ieee11073.sdc.dpws.service.HostingServiceProxy;
import org.ieee11073.sdc.dpws.soap.NotificationSink;
import org.ieee11073.sdc.dpws.soap.SoapMessage;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.WsDiscoveryClient;

import java.util.function.Consumer;

/**
 * Factory to create helper objects for the DPWS client.
 */
public interface ClientHelperFactory {
    /**
     * Creates a resolver for discovered devices.
     *
     * @param wsDiscoveryClient the discovery client needed to formulate resolve requests.
     * @return a {@link DiscoveredDeviceResolver} instance.
     */
    DiscoveredDeviceResolver createDiscoveredDeviceResolver(@Assisted WsDiscoveryClient wsDiscoveryClient);


    /**
     * Creates a {@linkplain DiscoveryClientUdpProcessor} instance.
     *
     * @param notificationSink notification that processes incoming {@link SoapMessage} instances.
     * @return a configured {@link DiscoveryClientUdpProcessor} instance.
     */
    DiscoveryClientUdpProcessor createDiscoveryClientUdpProcessor(@Assisted NotificationSink notificationSink);

    /**
     * Creates a discovery observer that listens to Hello, Bye and ProbeMatches messages.
     *
     * @param discoveredDeviceResolver the resolver that is used if hello messages do not contain XAddrs.
     * @return the oberserver instance.
     */
    HelloByeAndProbeMatchesObserverImpl createDiscoveryObserver(@Assisted DiscoveredDeviceResolver discoveredDeviceResolver);

    /**
     * Creates a watchdog.
     * <p>
     * todo DGr needs to be tested
     *
     * @param wsDiscoveryClient       discovery client to send heartbeat requests.
     * @param watchdogTriggerCallback callback that is triggered if the watchdog could not ping a remote device.
     * @return the watchdog instance.
     */
    WatchDog createWatchdog(@Assisted WsDiscoveryClient wsDiscoveryClient,
                            @Assisted Consumer<HostingServiceProxy> watchdogTriggerCallback);
}
