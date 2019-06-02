package org.ieee11073.sdc.dpws.udp;

import com.google.common.util.concurrent.Service;

/**
 * Two message queues to send and receive UDP messages.
 *
 * Use {@link #sendMessage(UdpMessage)} to send a UDP message using the UDP binding set with
 * {@link #setUdpBinding(UdpBindingService)}.
 *
 * Use {@link #registerUdpMessageQueueObserver(UdpMessageQueueObserver)} to add recipients that receive incoming UDP messages caught
 * by UDP binding set with {@link #setUdpBinding(UdpBindingService)}.
 */
public interface UdpMessageQueueService extends Service, UdpMessageReceiverCallback {
    /**
     * Inject UDP binding service.
     *
     * The binding service is started on demand.
     */
    void setUdpBinding(UdpBindingService udpBinding);

    /**
     * Queue outgoing UDP message for sending it using the UDP binding.
     *
     * @param message Message to send.
     * @return True if message could be queued, otherwise false.
     */
    boolean sendMessage(UdpMessage message);

    /**
     * Register observer to receive incoming UDP messages.
     *
     * @param observer The observer to registerOrUpdate.
     */
    void registerUdpMessageQueueObserver(UdpMessageQueueObserver observer);

    /**
     * Unegister observer to stop receiving incoming UDP messages.
     *
     * @param observer The observer to unregister.
     */
    void unregisterUdpMessageQueueObserver(UdpMessageQueueObserver observer);
}
