package org.ieee11073.sdc.dpws.client.helper.factory;

import com.google.inject.assistedinject.Assisted;
import org.ieee11073.sdc.dpws.client.helper.*;
import org.ieee11073.sdc.dpws.client.helper.*;
import org.ieee11073.sdc.dpws.service.HostingServiceProxy;
import org.ieee11073.sdc.dpws.soap.NotificationSink;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.WsDiscoveryClient;

import java.util.function.Consumer;

/**
 * Factory to create helper objects for the DPWS client.
 */
public interface ClientHelperFactory {
    DeviceProxyResolver createDeviceProxyResolver(@Assisted WsDiscoveryClient wsDiscoveryClient);
    DiscoveryClientUdpProcessor createDiscoveryClientUdpProcessor(@Assisted NotificationSink notificationSink);
    HostingServiceResolver createHostingServiceResolver(@Assisted HostingServiceRegistry hostingServiceRegistry);
    DeviceProxyObserver createDeviceProxyObserver(@Assisted DeviceProxyResolver deviceProxyResolver);
    WatchDog createWatchdog(@Assisted WsDiscoveryClient wsDiscoveryClient,
                            @Assisted Consumer<HostingServiceProxy> watchdogTriggerCallback);
}
