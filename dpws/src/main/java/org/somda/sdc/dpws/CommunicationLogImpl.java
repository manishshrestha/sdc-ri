package org.somda.sdc.dpws;

import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import org.apache.commons.io.output.TeeOutputStream;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.somda.sdc.dpws.soap.CommunicationContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Default implementation of {@linkplain CommunicationLog}.
 */
public class CommunicationLogImpl implements CommunicationLog {
    private static final Logger LOG = LogManager.getLogger(CommunicationLogImpl.class);

    private final CommunicationLogSink logSink;

    @Inject
    public CommunicationLogImpl(CommunicationLogSink sink) {

        this.logSink = sink;
    }

    @Override
    public TeeOutputStream logMessage(Direction direction,
                                      TransportType transportType,
                                      CommunicationContext communicationContext,
                                      OutputStream message) {
        OutputStream logFile = this.logSink.createTargetStream(transportType, direction, communicationContext);
        return new TeeOutputStream(message, logFile);
    }

    @Override
    public OutputStream logMessage(Direction direction,
                                   TransportType transportType,
                                   CommunicationContext communicationContext) {
        return this.logSink.createTargetStream(transportType, direction, communicationContext);
    }

    @Override
    public InputStream logMessage(Direction direction,
                                  TransportType transportType,
                                  CommunicationContext communicationContext,
                                  InputStream message) {
        return writeLogFile(transportType, direction, communicationContext, message);
    }

    private InputStream writeLogFile(TransportType transportType,
                                     Direction direction,
                                     CommunicationContext communicationContext,
                                     InputStream inputStream) {
        try {
            final byte[] bytes = ByteStreams.toByteArray(inputStream);

            try (OutputStream targetStream = this.logSink.createTargetStream(transportType, direction, communicationContext)) {
                new ByteArrayInputStream(bytes).transferTo(targetStream);
            }
            return new ByteArrayInputStream(bytes);
        } catch (IOException e) {
            LOG.warn("Could not write to communication log file", e);
        }
        return inputStream;
    }
}
