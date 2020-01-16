package org.somda.sdc.dpws;

import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.dpws.udp.UdpMessage;

import javax.inject.Named;
import java.io.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Default implementation of {@linkplain CommunicationLog}.
 */
public class CommunicationLogImpl implements CommunicationLog {
    private static final Logger LOG = LoggerFactory.getLogger(CommunicationLogImpl.class);

    private File logDirectory;

    private static final String SEPARATOR = "_";
    private static final String SUFFIX = ".xml";

    @Inject
    CommunicationLogImpl(@Named(DpwsConfig.COMMUNICATION_LOG_DIRECTORY) File logDirectory) {
        this.logDirectory = null;
        if (!logDirectory.mkdirs()) {
            LOG.warn("Could not create communication log directory '{}'", logDirectory.getAbsolutePath());
        }
        if (!logDirectory.exists()) {
            LOG.warn("Communication log directory '{}' does not exist", logDirectory.getAbsolutePath());
        } else {
            this.logDirectory = logDirectory;
        }
    }

    @Override
    public void logHttpMessage(HttpDirection direction, String address, Integer port, byte[] httpMessage) {
        writeLogFile(
                makeName(direction.toString(), address, port),
                new ByteArrayInputStream(httpMessage));
    }

    @Override
    public InputStream logHttpMessage(HttpDirection direction, String address, Integer port, InputStream httpMessage) {
        return writeLogFile(
                makeName(direction.toString(), address, port),
                httpMessage);
    }

    @Override
    public void logUdpMessage(UdpDirection direction, String destinationAddress, Integer destinationPort, UdpMessage udpMessage) {
        writeLogFile(
                makeName(direction.toString(), destinationAddress, destinationPort),
                new ByteArrayInputStream(udpMessage.getData(), 0, udpMessage.getLength()));
    }

    private InputStream writeLogFile(String filename, InputStream inputStream) {
        if (logDirectory == null) {
            return inputStream;
        }
        try {
            final byte[] bytes = ByteStreams.toByteArray(inputStream);
            if (bytes.length > 0) {
                new ByteArrayInputStream(bytes)
                        .transferTo(new FileOutputStream(logDirectory.getAbsolutePath() + File.separator + filename));

                return new ByteArrayInputStream(bytes);
            }
        } catch (IOException e) {
            LOG.warn("Could not write communication log file", e);
        }

        return inputStream;
    }

    private String makeName(String direction, String destinationAddress, Integer destinationPort) {
        LocalTime date = LocalTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH-mm-ss-SSS");
        return System.nanoTime() +
                SEPARATOR +
                date.format(formatter) +
                SEPARATOR +
                direction +
                SEPARATOR +
                destinationAddress +
                SEPARATOR +
                destinationPort +
                SUFFIX;
    }

    /**
     * UDP direction enumeration.
     */
    public enum UdpDirection {
        INBOUND("ibound-udp"),
        OUTBOUND("obound-udp");

        private final String stringRepresentation;

        UdpDirection(String stringRepresentation) {
            this.stringRepresentation = stringRepresentation;
        }

        @Override
        public String toString() {
            return stringRepresentation;
        }
    }

    /**
     * HTTP direction enumeration.
     */
    public enum HttpDirection {
        INBOUND_REQUEST("ibound-http-request"),
        INBOUND_RESPONSE("ibound-http-response"),
        OUTBOUND_REQUEST("obound-http-request"),
        OUTBOUND_RESPONSE("obound-http-response");

        private final String stringRepresentation;

        HttpDirection(String stringRepresentation) {
            this.stringRepresentation = stringRepresentation;
        }

        @Override
        public String toString() {
            return stringRepresentation;
        }
    }
}
