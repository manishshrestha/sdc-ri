package org.somda.sdc.dpws;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Communication log interface.
 */
public interface CommunicationLog {
    
    /**
     * Logs an HTTP message based on an {@linkplain OutputStream}.
     *
     * It does not block i.e. it can return before data is written to the output.
     *
     * @param direction     direction used for filename.
     * @param transportType the transport protocol used i.e. udp, http, etc.
     * @param address       address information used for filename.
     * @param port          port information used for filename.
     * @param message   the output stream to branch to the log file.
     * @return an output stream, that streams to the original output stream and optionally streams to another stream
 * 				similarly to the tee Unix command. The other stream can be a log file stream.
     */
    OutputStream logMessage(Direction direction, TransportType transportType, String address, Integer port,
                            OutputStream message);

    /**
     * Logs an HTTP message based on an {@linkplain InputStream}.
     *
     * It blocks until everything has been read.
     *
     * @param direction     direction used for filename.
     * @param transportType the transport protocol used i.e. udp, http, etc.
     * @param address       address information used for filename.
     * @param port          port information used for filename.
     * @param message   the message to log as input stream.
     *                      As the input stream might be unusable after reading, another one is created to be used for
     *                      further processing; see return value.
     * @return a new input stream that mirrors the data from the message input data.
     */
    InputStream logMessage(Direction direction, TransportType transportType, String address, Integer port,
                           InputStream message);


    /**
     * Direction enumeration.
     */
    enum Direction {
        INBOUND("ibound"), OUTBOUND("obound");

        private final String stringRepresentation;

        Direction(String stringRepresentation) {
            this.stringRepresentation = stringRepresentation;
        }

        @Override
        public String toString() {
            return stringRepresentation;
        }
    }

    /**
     * Defines the transport type.
     */
    enum TransportType {
        UDP("udp"), HTTP("http");

        private final String stringRepresentation;

        TransportType(String stringRepresentation) {
            this.stringRepresentation = stringRepresentation;
        }

        @Override
        public String toString() {
            return stringRepresentation;
        }
    }
}
