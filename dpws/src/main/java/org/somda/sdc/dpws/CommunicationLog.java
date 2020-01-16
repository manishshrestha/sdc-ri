package org.somda.sdc.dpws;

import org.somda.sdc.dpws.udp.UdpMessage;

import java.io.InputStream;

/**
 * Communication log interface.
 */
public interface CommunicationLog {
    /**
     * Log an HTTP message based on a byte array.
     *
     * @param direction   direction used for filename.
     * @param address     address information used for filename.
     * @param port        port information used for filename.
     * @param httpMessage the message to log.
     */
    void logHttpMessage(CommunicationLogImpl.HttpDirection direction, String address, Integer port, byte[] httpMessage);

    /**
     * Log an HTTP message based on an input stream.
     *
     * @param direction   direction used for filename.
     * @param address     address information used for filename.
     * @param port        port information used for filename.
     * @param httpMessage the message to log as input stream.
     *                    As the input stream might be unusable after reading, another one is created to be used for
     *                    further processing; see return value.
     * @return a new input stream that mirrors the data from the httpMessage input data.
     */
    InputStream logHttpMessage(CommunicationLogImpl.HttpDirection direction, String address, Integer port, InputStream httpMessage);

    /**
     * Log a UDP message based on an {@linkplain UdpMessage}.
     *
     * @param direction  direction used for filename.
     * @param address    address information used for filename.
     * @param port       port information used for filename.
     * @param udpMessage the UDP message to log.
     */
    void logUdpMessage(CommunicationLogImpl.UdpDirection direction, String address, Integer port, UdpMessage udpMessage);
}
