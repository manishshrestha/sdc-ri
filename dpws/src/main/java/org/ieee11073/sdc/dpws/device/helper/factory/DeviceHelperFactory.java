package org.ieee11073.sdc.dpws.device.helper.factory;

import com.google.inject.assistedinject.Assisted;
import org.ieee11073.sdc.dpws.device.helper.DiscoveryDeviceUdpMessageProcessor;
import org.ieee11073.sdc.dpws.soap.RequestResponseServer;
import org.ieee11073.sdc.dpws.udp.UdpMessageQueueService;
import org.ieee11073.sdc.dpws.soap.SoapMessage;

/**
 * Factory to create instances of device helper classes.
 */
public interface DeviceHelperFactory {

    /**
     * Create {@linkplain DiscoveryDeviceUdpMessageProcessor} instance.
     *
     * @param rrServer           Request response server where to process incoming and outgoing
     *                           {@link SoapMessage} instances.
     * @param udpMsgQueueService UDP message queue where to receive incoming UDP messages from and send outgoing UDP
     *                           messages to.
     */
    DiscoveryDeviceUdpMessageProcessor createDiscoveryDeviceUdpMessageProcessor(@Assisted RequestResponseServer rrServer,
                                                                                @Assisted UdpMessageQueueService udpMsgQueueService);
}
