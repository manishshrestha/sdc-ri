package org.somda.sdc.dpws;

import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import org.apache.commons.io.output.TeeOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.dpws.soap.CommunicationContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Default implementation of {@linkplain CommunicationLog}.
 */
public class CommunicationLogImpl implements CommunicationLog {
    private static final Logger LOG = LoggerFactory.getLogger(CommunicationLogImpl.class);

    private static final String SEPARATOR = "_";

    private final CommunicationLogSink logSink;

    @Inject
    public CommunicationLogImpl(CommunicationLogSink sink) {

        this.logSink = sink;
    }

    @Override
    public TeeOutputStream logMessage(Direction direction, TransportType transportType, CommunicationContext communicationContext,
                                      OutputStream message) {

        var address = communicationContext.getTransportInfo().getRemoteAddress().get();
        var port = communicationContext.getTransportInfo().getRemotePort().get();

        OutputStream logFile = this.logSink.getTargetStream(transportType,
                makeName(direction.toString(), address, port));

        return new TeeOutputStream(message, logFile);

    }

    @Override
    public OutputStream logMessage(Direction direction, TransportType transportType, CommunicationContext communicationContext) {
        var address = communicationContext.getTransportInfo().getRemoteAddress().get();
        var port = communicationContext.getTransportInfo().getRemotePort().get();

        return this.logSink.getTargetStream(transportType, makeName(direction.toString(), address, port));
    }

    @Override
    public InputStream logMessage(Direction direction, TransportType transportType,
                                  CommunicationContext communicationContext, InputStream message) {
        var address = communicationContext.getTransportInfo().getRemoteAddress().get();
        var port = communicationContext.getTransportInfo().getRemotePort().get();

        return writeLogFile(transportType, makeName(direction.toString(), address, port),
                message);
    }

    private InputStream writeLogFile(TransportType transportType, String filename,
                                     InputStream inputStream) {

        try {
            final byte[] bytes = ByteStreams.toByteArray(inputStream);

            new ByteArrayInputStream(bytes).transferTo(this.logSink.getTargetStream(transportType, filename));

            return new ByteArrayInputStream(bytes);

        } catch (IOException e) {
            LOG.warn("Could not write to communication log file", e);
        }

        return inputStream;
    }

    private String makeName(String direction, String destinationAddress, Integer destinationPort) {
        LocalTime date = LocalTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH-mm-ss-SSS");
        return System.nanoTime() + SEPARATOR + date.format(formatter) + SEPARATOR + direction + SEPARATOR
                + destinationAddress + SEPARATOR + destinationPort;
    }
}
