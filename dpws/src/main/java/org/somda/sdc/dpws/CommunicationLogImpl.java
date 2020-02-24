package org.somda.sdc.dpws;

import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.io.output.TeeOutputStream;

import java.io.*;
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
    public TeeOutputStream logMessage(Direction direction, TransportType transportType, String address, Integer port,
                                      OutputStream httpMessage) {

        OutputStream logFile = this.logSink.createBranch(transportType,
                makeName(direction.toString(), address, port));

        return new TeeOutputStream(httpMessage, logFile);

    }

    @Override
    public InputStream logMessage(Direction direction, TransportType transportType,
                                  String address, Integer port, InputStream httpMessage) {
        return writeLogFile(transportType, makeName(direction.toString(), address, port),
                httpMessage);
    }

    private InputStream writeLogFile(TransportType transportType, String filename,
            InputStream inputStream) {

        try {
            final byte[] bytes = ByteStreams.toByteArray(inputStream);

            new ByteArrayInputStream(bytes).transferTo(this.logSink.createBranch(transportType, filename));

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
