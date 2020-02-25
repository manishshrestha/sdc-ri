package org.somda.sdc.dpws;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Implementation of {@link CommunicationLog} that does not output anything.
 */
public class CommunicationLogDummyImpl implements CommunicationLog {
    
    @Override
    public OutputStream logMessage(Direction direction, TransportType transportType, String address, Integer port, OutputStream message) {
        return message;
    } 

    @Override
    public InputStream logMessage(Direction direction, TransportType transportType, String address, Integer port, InputStream message) {
        return message;
    }
}
