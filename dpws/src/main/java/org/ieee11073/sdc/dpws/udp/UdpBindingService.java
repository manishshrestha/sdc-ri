package org.ieee11073.sdc.dpws.udp;

import com.google.common.util.concurrent.Service;

import java.io.IOException;

/**
 * Service to receive and send UDP messages.
 */
public interface UdpBindingService extends Service {

    /**
     * Sets a message receiver callback to fetch any messages from the UDP socket.
     *
     * @param receiver the message receiver to set.
     */
    void setMessageReceiver(UdpMessageReceiverCallback receiver);

    /**
     * Sends a byte array given as parameter to the connected UDP socket.
     * <p>
     * The function blocks until the message is sent.
     *
     * @param message The byte array to send. Uses the length attribute to retrieve byte array size.
     * @throws IOException On any IO problem.
     */
    void sendMessage(UdpMessage message) throws IOException;
}
