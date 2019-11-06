package org.somda.sdc.dpws.helper;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.somda.sdc.dpws.soap.MarshallingService;
import org.somda.sdc.dpws.soap.SoapDebug;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.interception.NotificationCallback;
import org.somda.sdc.dpws.udp.UdpMessage;
import org.somda.sdc.dpws.udp.UdpMessageQueueService;
import org.somda.sdc.dpws.soap.NotificationSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;

/**
 * Marshall {@link SoapMessage} and distribute them via {@link UdpMessageQueueService}.
 *
 * {@linkplain NotificationSourceUdpCallback} implements {@link NotificationCallback} and is intended to be used by
 * a {@link NotificationSource} instance to be connected with a {@link UdpMessageQueueService}.
 */
public class NotificationSourceUdpCallback implements NotificationCallback {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationSourceUdpCallback.class);

    private final UdpMessageQueueService udpMessageQueue;
    private final MarshallingService marshallingService;

    @AssistedInject
    NotificationSourceUdpCallback(@Assisted UdpMessageQueueService udpMessageQueue,
                                  MarshallingService marshallingService) {
        this.udpMessageQueue = udpMessageQueue;
        this.marshallingService = marshallingService;
    }

    @Override
    public void onNotification(SoapMessage notification) throws MarshallingException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Outgoing SOAP/UDP message: {}", SoapDebug.get(notification));
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        marshallingService.marshal(notification, bos);
        byte[] data = bos.toByteArray();
        udpMessageQueue.sendMessage(new UdpMessage(data, data.length));
    }
}
