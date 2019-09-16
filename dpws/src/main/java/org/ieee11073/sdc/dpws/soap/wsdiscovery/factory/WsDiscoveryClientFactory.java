package org.ieee11073.sdc.dpws.soap.wsdiscovery.factory;

import com.google.inject.assistedinject.Assisted;
import org.ieee11073.sdc.dpws.soap.NotificationSource;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.WsDiscoveryClient;

/**
 * Factory to create WS-Discovery clients.
 */
public interface WsDiscoveryClientFactory {
    /**
     * @param notificationSource The source where Probe and Resolve messages are sent from.
     * @return the instance.
     */
    WsDiscoveryClient createWsDiscoveryClient(@Assisted NotificationSource notificationSource);
}
