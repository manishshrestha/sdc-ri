package org.ieee11073.sdc.dpws.helper.factory;

import com.google.inject.assistedinject.Assisted;
import org.ieee11073.sdc.dpws.helper.NotificationSourceUdpCallback;
import org.ieee11073.sdc.dpws.udp.UdpMessageQueueService;

/**
 * Factory to create instances of {@link NotificationSourceUdpCallback}.
 */
public interface DpwsHelperFactory {
    /**
     * Create {@linkplain NotificationSourceUdpCallback} instance.
     *
     * @param msgQueue UDP message queue to send SOAP messages to network.
     */
    NotificationSourceUdpCallback createNotificationSourceUdpCallback(@Assisted UdpMessageQueueService msgQueue);
}
