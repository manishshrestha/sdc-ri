package org.somda.sdc.dpws;

import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.dpws.udp.UdpMessage;
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
    CommunicationLogImpl(CommunicationLogSink sink) {

        this.logSink = sink;
    }

    @Override
    public TeeOutputStream logHttpMessage(HttpDirection direction, String address, Integer port,
            OutputStream httpMessage) {

        OutputStream log_file = this.logSink.createBranch(CommunicationLogSink.BranchPath.HTTP,
                makeName(direction.toString(), address, port));

        return new TeeOutputStream(httpMessage, log_file);

    }

    @Override
    public InputStream logHttpMessage(HttpDirection direction, String address, Integer port, InputStream httpMessage) {
        return writeLogFile(CommunicationLogSink.BranchPath.HTTP, makeName(direction.toString(), address, port),
                httpMessage);
    }

    @Override
    public void logUdpMessage(UdpDirection direction, String destinationAddress, Integer destinationPort,
            UdpMessage udpMessage) {
        writeLogFile(CommunicationLogSink.BranchPath.UDP,
                makeName(direction.toString(), destinationAddress, destinationPort),
                new ByteArrayInputStream(udpMessage.getData(), 0, udpMessage.getLength()));
    }

    private InputStream writeLogFile(CommunicationLogSink.BranchPath branchpath, String filename,
            InputStream inputStream) {

        try {
            final byte[] bytes = ByteStreams.toByteArray(inputStream);

            new ByteArrayInputStream(bytes).transferTo(this.logSink.createBranch(branchpath, filename));

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

    /**
     * UDP direction enumeration.
     */
    public enum UdpDirection {
        INBOUND("ibound-udp"), OUTBOUND("obound-udp");

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
        INBOUND_REQUEST("ibound-http-request"), INBOUND_RESPONSE("ibound-http-response"),
        OUTBOUND_REQUEST("obound-http-request"), OUTBOUND_RESPONSE("obound-http-response");

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
