package org.ieee11073.sdc.dpws.udp;

import com.google.common.util.concurrent.Service;

/**
 * Two message queues to send and receive UDP messages.
 * <p>
 * Use {@link #sendMessage(UdpMessage)} to send a UDP message using the UDP binding set with
 * {@link #setUdpBinding(UdpBindingService)}.
 * <p>
 * Use {@link #registerUdpMessageQueueObserver(UdpMessageQueueObserver)} to add recipients that receive incoming UDP
 * messages caught by the UDP binding.
 */
public interface UdpMessageQueueService extends Service, UdpMessageReceiverCallback {
    /**
     * Injects the UDP binding service.
     *
     * @param udpBinding the UDP binding service to inject.
     */
    void setUdpBinding(UdpBindingService udpBinding);

    /**
     * Queues an outgoing UDP message.
     *
     * @param message the message to be send.
     * @return true if the message could be queued, otherwise false.
     */
    boolean sendMessage(UdpMessage message);

    /**
     * Registers an observer to receive incoming UDP messages.
     *
     * @param observer the observer to register.
     */
    void registerUdpMessageQueueObserver(UdpMessageQueueObserver observer);

    /**
     * Unregister observer to stop receiving incoming UDP messages.
     *
     * @param observer the observer to unregister.
     */
    void unregisterUdpMessageQueueObserver(UdpMessageQueueObserver observer);
}
