package org.somda.sdc.dpws;

import org.somda.sdc.dpws.udp.UdpMessage;

import java.io.InputStream;

/**
 * Implementation of {@link CommunicationLog} that does not output anything.
 */
public class CommunicationLogEmptySink implements CommunicationLog {
    @Override
    public void logHttpMessage(CommunicationLogImpl.HttpDirection direction, String address, Integer port, byte[] httpMessage) {
        return;
    }

    @Override
    public InputStream logHttpMessage(CommunicationLogImpl.HttpDirection direction, String address, Integer port, InputStream httpMessage) {
        return httpMessage;
    }

    @Override
    public void logUdpMessage(CommunicationLogImpl.UdpDirection direction, String address, Integer port, UdpMessage udpMessage) {
    }
}
