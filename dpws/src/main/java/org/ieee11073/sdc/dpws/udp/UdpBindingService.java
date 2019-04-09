package org.ieee11073.sdc.dpws.udp;

import com.google.common.util.concurrent.Service;

import java.io.IOException;

/**
 * Service to receive and send UDP messages.
 */
public interface UdpBindingService extends Service {

    /**
     * Set message receiver callback to fetch any messages from the UDP socket.
     */
    void setMessageReceiver(UdpMessageReceiverCallback receiver);

    /**
     * Send byte array given as parameter to UDP socket.
     *
     * The function blocks until the message is sent.
     *
     * @param message The byte array to send. Uses the length attribute to retrieve byte array size.
     * @throws IOException On any IO problem.
     */
    void sendMessage(UdpMessage message) throws IOException;
}
