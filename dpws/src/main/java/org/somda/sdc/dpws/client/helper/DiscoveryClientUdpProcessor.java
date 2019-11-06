package org.somda.sdc.dpws.client.helper;

import com.google.common.eventbus.Subscribe;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.somda.sdc.dpws.soap.MarshallingService;
import org.somda.sdc.dpws.soap.NotificationSink;
import org.somda.sdc.dpws.soap.SoapDebug;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.udp.UdpMessage;
import org.somda.sdc.dpws.udp.UdpMessageQueueObserver;
import org.somda.sdc.dpws.udp.UdpMessageQueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;

/**
 * Receives WS-Discovery SOAP messages via UDP.
 * <p>
 * To receive {@link UdpMessage} instances, {@linkplain DiscoveryClientUdpProcessor} needs to be registered at a
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
    private void receiveUdpMessage(UdpMessage msg) {
        SoapMessage notification;
        // Unmarshal SOAP request message
        try {
            notification = marshallingService.unmarshal(new ByteArrayInputStream(msg.getData(), 0, msg.getLength()));
        } catch (MarshallingException e) {
            LOG.warn("Incoming UDP message could not be unmarshalled. Message Bytes: {}", msg.toString());
            return;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Incoming SOAP/UDP message: {}", SoapDebug.get(notification));
        }

        // Forward SOAP message to given notification interceptor chain
        notificationSink.receiveNotification(notification);
    }
}
