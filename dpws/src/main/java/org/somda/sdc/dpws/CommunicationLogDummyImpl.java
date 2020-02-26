package org.somda.sdc.dpws;

import org.somda.sdc.dpws.soap.CommunicationContext;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Implementation of {@link CommunicationLog} that does not output anything.
 */
public class CommunicationLogDummyImpl implements CommunicationLog {
    
    @Override
    public OutputStream logMessage(Direction direction, TransportType transportType, CommunicationContext communicationContext, OutputStream message) {
        return message;
    } 

    @Override
    public InputStream logMessage(Direction direction, TransportType transportType, CommunicationContext communicationContext, InputStream message) {
        return message;
    }
}
