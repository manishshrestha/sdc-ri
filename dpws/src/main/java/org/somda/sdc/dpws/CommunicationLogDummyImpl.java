package org.somda.sdc.dpws;

import org.somda.sdc.dpws.soap.CommunicationContext;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Implementation of {@link CommunicationLog} that does not output anything.
 */
public class CommunicationLogDummyImpl implements CommunicationLog {
    
    @Override
    public OutputStream logMessage(Direction direction, TransportType transportType, MessageType messageType,
                                   CommunicationContext communicationContext, Level level, OutputStream message) {
        return message;
    }

    @Override
    public OutputStream logMessage(Direction direction, TransportType transportType, MessageType messageType,
                                   CommunicationContext communicationContext, Level level) {
        return OutputStream.nullOutputStream();
    }

    @Override
    public OutputStream logRelatedMessage(OutputStream relatedTo, Direction direction, TransportType transportType,
                                          MessageType messageType, CommunicationContext communicationContext,
                                          Level level) {
        return logMessage(direction, transportType, messageType, communicationContext, level);
    }

    @Override
    public InputStream logMessage(Direction direction, TransportType transportType, MessageType messageType,
                                  CommunicationContext communicationContext, Level level, InputStream message) {
        return message;
    }
}
