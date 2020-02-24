package org.somda.sdc.dpws.client.helper;

import com.google.common.eventbus.Subscribe;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.somda.sdc.dpws.DpwsConstants;
import org.somda.sdc.dpws.soap.*;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.udp.UdpMessage;
import org.somda.sdc.dpws.udp.UdpMessageQueueObserver;
import org.somda.sdc.dpws.udp.UdpMessageQueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.Collections;

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
        notificationSink.receiveNotification(notification, new TransportInfo(
                DpwsConstants.URI_SCHEME_SOAP_OVER_UDP,
                null,
                null,
                msg.getHost(),
                msg.getPort(),
                Collections.emptyList()));
    }
}
