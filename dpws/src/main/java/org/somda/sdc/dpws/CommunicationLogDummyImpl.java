package org.somda.sdc.dpws;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Implementation of {@link CommunicationLog} that does not output anything.
 */
public class CommunicationLogDummyImpl implements CommunicationLog {
    
    @Override
    public OutputStream logMessage(Direction direction, TransportType transportType, String address, Integer port, OutputStream httpMessage) {
        return httpMessage;
    } 

    @Override
    public InputStream logMessage(Direction direction, TransportType transportType, String address, Integer port, InputStream httpMessage) {
        return httpMessage;
    }
}
