package org.somda.sdc.dpws;

import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import org.apache.commons.io.output.TeeOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.HttpApplicationInfo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Default implementation of {@linkplain CommunicationLog}.
 */
public class CommunicationLogImpl implements CommunicationLog {
    private static final Logger LOG = LoggerFactory.getLogger(CommunicationLogImpl.class);


    private final CommunicationLogSink logSink;

    @Inject
    public CommunicationLogImpl(CommunicationLogSink sink) {

        this.logSink = sink;
    }

    @Override
    public TeeOutputStream logMessage(Direction direction, TransportType transportType, CommunicationContext communicationContext,
                                      OutputStream message) {

        OutputStream logFile = this.logSink.getTargetStream(transportType, direction, communicationContext);

        if (communicationContext.getApplicationInfo() instanceof HttpApplicationInfo) {
            var appInfo = (HttpApplicationInfo) communicationContext.getApplicationInfo();
            LOG.warn("http headers in {} log message {}", direction.toString(), appInfo.getHttpHeaders());
        }

        return new TeeOutputStream(message, logFile);

    }

    @Override
    public OutputStream logMessage(Direction direction, TransportType transportType, CommunicationContext communicationContext) {
        return this.logSink.getTargetStream(transportType, direction, communicationContext);
    }

    @Override
    public InputStream logMessage(Direction direction, TransportType transportType,
                                  CommunicationContext communicationContext, InputStream message) {

        if (communicationContext.getApplicationInfo() instanceof HttpApplicationInfo) {
            var appInfo = (HttpApplicationInfo) communicationContext.getApplicationInfo();
            LOG.warn("http headers in {} log message {}", direction.toString(), appInfo.getHttpHeaders());
        }

        return writeLogFile(transportType, direction, communicationContext, message);
    }

    private InputStream writeLogFile(TransportType transportType, Direction direction, CommunicationContext communicationContext,
                                     InputStream inputStream) {

        try {
            final byte[] bytes = ByteStreams.toByteArray(inputStream);

            new ByteArrayInputStream(bytes).transferTo(this.logSink.getTargetStream(transportType, direction, communicationContext));

            return new ByteArrayInputStream(bytes);

        } catch (IOException e) {
            LOG.warn("Could not write to communication log file", e);
        }

        return inputStream;
    }

}
