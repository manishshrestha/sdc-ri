package org.somda.sdc.dpws.udp;

import com.google.common.util.concurrent.Service;

import java.io.IOException;

/**
 * Service to receive and send UDP messages.
 */
public interface UdpBindingService extends Service {

    /**
     * Sets a message receiver callback to fetch any messages from the UDP socket.
     * <p>
     * A received message contains the payload as well as sender address and port.
     * @param receiver the message receiver to set.
     */
    void setMessageReceiver(UdpMessageReceiverCallback receiver);

    /**
     * Sends a UDP message given as parameter to the connected UDP socket.
     * <p>
     * The function blocks until the message is sent.
     *
     * @param message the message to send. The message shall contain host and port of the receiver in case of unicast;
     *                multicast does not need transport information as those are stored in the binding service.
     * @throws IOException on any IO problem.
     */
    void sendMessage(UdpMessage message) throws IOException;
}
