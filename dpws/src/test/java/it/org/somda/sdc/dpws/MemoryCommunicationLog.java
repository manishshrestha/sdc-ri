package it.org.somda.sdc.dpws;


import com.google.common.io.ByteStreams;
import org.apache.commons.io.output.TeeOutputStream;
import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.soap.CommunicationContext;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

public class MemoryCommunicationLog implements CommunicationLog {

    private final ArrayList<Message> messages;

    public MemoryCommunicationLog() {
        this.messages = new ArrayList<>();
    }

    @Override
    public OutputStream logMessage(Direction direction, TransportType transportType, CommunicationContext communicationContext, OutputStream message) {
        var messageType = new Message(direction, transportType, communicationContext);
        messages.add(messageType);
        return new TeeOutputStream(message, messageType);
    }

    @Override
    public OutputStream logMessage(Direction direction, TransportType transportType, CommunicationContext communicationContext) {
        var messageType = new Message(direction, transportType, communicationContext);
        messages.add(messageType);
        return messageType;
    }

    @Override
    public InputStream logMessage(Direction direction, TransportType transportType, CommunicationContext communicationContext, InputStream message) {
        try {
            final byte[] bytes = ByteStreams.toByteArray(message);

            try (Message targetStream = new Message(direction, transportType, communicationContext)) {
                messages.add(targetStream);
                new ByteArrayInputStream(bytes).transferTo(targetStream);
            }
            return new ByteArrayInputStream(bytes);
        } catch (IOException e) {
            fail(e);
        }
        return message;
    }

    public List<Message> getMessages() {
        return new ArrayList<>(messages);
    }

    public class Message extends OutputStream {

        private final CommunicationLog.Direction direction;
        private final CommunicationLog.TransportType transportType;
        private final CommunicationContext communicationContext;
        private final ByteArrayOutputStream outputStream;

        Message(CommunicationLog.Direction direction, CommunicationLog.TransportType transportType, CommunicationContext communicationContext) {
            this.direction = direction;
            this.transportType = transportType;
            this.communicationContext = communicationContext;
            this.outputStream = new ByteArrayOutputStream();
        }

        @Override
        public void write(int b) throws IOException {
            this.outputStream.write(b);
        }

        @Override
        public void close() throws IOException {
            this.outputStream.close();
        }

        public String getMessage() {
            return new String(this.outputStream.toByteArray(), StandardCharsets.UTF_8);
        }

        public CommunicationLog.Direction getDirection() {
            return direction;
        }
    }
}
