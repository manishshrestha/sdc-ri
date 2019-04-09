package org.ieee11073.sdc.dpws.client.helper;

import com.google.common.eventbus.Subscribe;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.ieee11073.sdc.dpws.soap.MarshallingService;
import org.ieee11073.sdc.dpws.soap.NotificationSink;
import org.ieee11073.sdc.dpws.soap.SoapDebug;
import org.ieee11073.sdc.dpws.soap.SoapMessage;
import org.ieee11073.sdc.dpws.soap.exception.MarshallingException;
import org.ieee11073.sdc.dpws.soap.exception.TransportException;
import org.ieee11073.sdc.dpws.udp.UdpMessage;
import org.ieee11073.sdc.dpws.udp.UdpMessageQueueObserver;
import org.ieee11073.sdc.dpws.udp.UdpMessageQueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;

/**
 * Receive and send WS-Discovery SOAP messages via UDP at the client side.
 *
 * To receive {@link UdpMessage} instances, {@linkplain DiscoveryClientUdpProcessor} shall be registered at a
 * {@link UdpMessageQueueService} by using
 * {@link UdpMessageQueueService#registerUdpMessageQueueObserver(UdpMessageQueueObserver)}.
 */
public class DiscoveryClientUdpProcessor implements UdpMessageQueueObserver {
    private static final Logger LOG = LoggerFactory.getLogger(DiscoveryClientUdpProcessor.class);

    private final MarshallingService marshallingService;
    private final NotificationSink notificationSink;

    @AssistedInject
    DiscoveryClientUdpProcessor(@Assisted NotificationSink notificationSink,
                                MarshallingService marshallingService) {
        this.notificationSink = notificationSink;
        this.marshallingService = marshallingService;
    }

    @Subscribe
    private void receiveUdpMessage(UdpMessage msg) throws TransportException {
        SoapMessage notification;
        // Unmarshal SOAP request message
        try {
            notification = marshallingService.unmarshal(new ByteArrayInputStream(msg.getData(), 0, msg.getLength()));
        } catch (MarshallingException e) {
            LOG.info("Incoming UDP message could not be unmarshalled. Reason: {}", e.getMessage());
            return;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Incoming SOAP/UDP message: {}", SoapDebug.get(notification));
        }

        // Forward SOAP message to given notification interceptor chain
        notificationSink.receiveNotification(notification);
    }
}
