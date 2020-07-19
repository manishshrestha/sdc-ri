package org.somda.sdc.proto.discovery.common;

import com.google.inject.Inject;
import com.google.protobuf.AbstractMessageLite;
import org.somda.sdc.dpws.DpwsConstants;
import org.somda.sdc.dpws.guice.DiscoveryUdpQueue;
import org.somda.sdc.dpws.soap.ApplicationInfo;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.TransportInfo;
import org.somda.sdc.dpws.udp.UdpMessage;
import org.somda.sdc.dpws.udp.UdpMessageQueueObserver;
import org.somda.sdc.dpws.udp.UdpMessageQueueService;

import java.util.Collections;

public class UdpUtil {
    private final UdpMessageQueueService udpMessageQueueService;

    @Inject
    UdpUtil(@DiscoveryUdpQueue UdpMessageQueueService udpMessageQueueService) {
        this.udpMessageQueueService = udpMessageQueueService;
    }

    public void sendMulticast(AbstractMessageLite<?, ?> message) {
        var bytes = message.toByteArray();
        udpMessageQueueService.sendMessage(new UdpMessage(bytes, bytes.length));
    }

    public void sendResponse(AbstractMessageLite<?, ?> message, UdpMessage requestMessage) {
        var bytes = message.toByteArray();
        udpMessageQueueService.sendMessage(new UdpMessage(bytes, bytes.length, new CommunicationContext(
                new ApplicationInfo(),
                new TransportInfo(
                        DpwsConstants.URI_SCHEME_SOAP_OVER_UDP,
                        null, null,
                        requestMessage.getHost(), requestMessage.getPort(),
                        Collections.emptyList()))));
    }

    public void registerObserver(UdpMessageQueueObserver observer) {
        this.udpMessageQueueService.registerUdpMessageQueueObserver(observer);
    }

    public void unregisterObserver(UdpMessageQueueObserver observer) {
        this.udpMessageQueueService.unregisterUdpMessageQueueObserver(observer);
    }
}
