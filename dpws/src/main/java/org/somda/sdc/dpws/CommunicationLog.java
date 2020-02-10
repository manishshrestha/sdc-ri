package org.somda.sdc.dpws;

import org.somda.sdc.dpws.udp.UdpMessage;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Communication log interface.
 */
public interface CommunicationLog {
    
    /**
     * Log an HTTP message based on an output stream.
     *
     * @param direction   direction used for filename.
     * @param address     address information used for filename.
     * @param port        port information used for filename.
     * @param httpMessage the output stream to branch to the log file.
     * @return an output stream, that streams to the original output stream and optionally streams to another stream 
 * 				similarly to the tee Unix command. The other stream can be a log file stream.
     */
    OutputStream logHttpMessage(CommunicationLogImpl.HttpDirection direction, String address, Integer port, OutputStream httpMessage);

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
