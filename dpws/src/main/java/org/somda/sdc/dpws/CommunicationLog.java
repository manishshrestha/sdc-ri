package org.somda.sdc.dpws;

import org.somda.sdc.dpws.soap.CommunicationContext;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Communication log interface.
 */
public interface CommunicationLog {

    /**
     * Logs an HTTP message based on an {@linkplain OutputStream}.
     * <p>
     * It does not block i.e. it can return before data is written to the output.
     *
     * @param direction            direction used for filename.
     * @param transportType        the transport protocol used i.e. udp, http, etc.
     * @param messageType          the type of the message i.e. request, response.
     * @param communicationContext communication information such as target address and port
     * @param level                the level of the message i.e. network or application.
     * @param message              the output stream to branch to the log file.
     * @return an output stream, that streams to the original output stream and optionally streams to another stream
     * similarly to the tee Unix command. The other stream can be a log file stream.
     */
    OutputStream logMessage(Direction direction, TransportType transportType, MessageType messageType,
                            CommunicationContext communicationContext, Level level, OutputStream message);

    /**
     * Creates an {@linkplain OutputStream} to write the log message into.
     *
     * @param direction            direction used for filename.
     * @param transportType        the transport protocol used i.e. udp, http, etc.
     * @param messageType          the type of the message i.e. request, response.
     * @param communicationContext communication information such as target address and port.
     * @param level                level of the message, i.e. network or application.
     * @return an output stream to write the log message into.
     */
    OutputStream logMessage(Direction direction, TransportType transportType, MessageType messageType,
                            CommunicationContext communicationContext, Level level);


    /**
     * Creates an {@linkplain OutputStream} to write the related log message into.
     *
     * @param relatedTo            OutputStream created by a call to logMessage() containing
     *                             the Message that this one is related to.
     * @param direction            direction used for filename.
     * @param transportType        the transport protocol used i.e. udp, http, etc.
     * @param messageType          the type of the message i.e. request, response.
     * @param communicationContext communication information such as target address and port.
     * @param level                level of the message, i.e. network or application.
     * @return an output stream to write the log message into.
     */
    OutputStream logRelatedMessage(OutputStream relatedTo,
                                   Direction direction,
                                   TransportType transportType,
                                   MessageType messageType,
                                   CommunicationContext communicationContext,
                                   Level level);

    /**
     * Logs an HTTP message based on an {@linkplain InputStream}.
     * <p>
     * It blocks until everything has been read.
     *
     * @param direction            direction used for filename.
     * @param transportType        the transport protocol used i.e. udp, http, etc.
     * @param messageType          the type of the message i.e. request, response.
     * @param communicationContext communication information such as target address and port
     * @param level                level of the message, i.e. network or application.
     * @param message              the message to log as input stream.
     *                             As the input stream might be unusable after reading,
     *                             another one is created to be used for further processing;
     *                             see return value.
     * @return a new input stream that mirrors the data from the message input data.
     */
    InputStream logMessage(Direction direction, TransportType transportType, MessageType messageType,
                           CommunicationContext communicationContext, Level level, InputStream message);


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

    /**
     * Defines the message type.
     */
    enum MessageType {
        REQUEST("request"), RESPONSE("response"),
        UNKNOWN("unknown");

        private final String stringRepresentation;

        MessageType(String stringRepresentation) { this.stringRepresentation = stringRepresentation; }

        /**
         * returns the name of this enum constant.
         * @return the name.
         */
        @Override
        public  String toString() { return stringRepresentation; }
    }

    /**
     * Level that a message was logged on.
     */
    enum Level {
        APPLICATION,  // Message was logged on application-level. All decoding-,decompression, etc. steps have been
                      //   performed. Headers that are not applicable any more have been removed.
        NETWORK       // Message was logged on network-level. It contains all headers and encodings that were present
                      //   during transmission.
    }
}
