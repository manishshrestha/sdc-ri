package org.somda.sdc.dpws;

import org.somda.sdc.dpws.udp.UdpMessage;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Implementation of {@link CommunicationLog} that does not output anything.
 */
public class CommunicationLogEmptySink implements CommunicationLog {
	
	@Deprecated
    @Override
    public void logHttpMessage(CommunicationLogImpl.HttpDirection direction, String address, Integer port, byte[] httpMessage) {
    }
    
    @Override
    public OutputStream logHttpMessage(CommunicationLogImpl.HttpDirection direction, String address, Integer port, OutputStream httpMessage) {
        return httpMessage;
    } 

    @Override
    public InputStream logHttpMessage(CommunicationLogImpl.HttpDirection direction, String address, Integer port, InputStream httpMessage) {
        return httpMessage;
    }

    @Override
    public void logUdpMessage(CommunicationLogImpl.UdpDirection direction, String address, Integer port, UdpMessage udpMessage) {
    }
}
