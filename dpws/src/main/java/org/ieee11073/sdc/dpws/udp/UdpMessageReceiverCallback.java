package org.ieee11073.sdc.dpws.udp;

/**
 * Callback to process UDP messages received by {@link UdpBindingService}.
 */
public interface UdpMessageReceiverCallback {
    /**
     * Receive incoming UDP message.
     *
     * @param udpMessage Raw data.
     */
    void receive(UdpMessage udpMessage);
}
