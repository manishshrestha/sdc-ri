package org.somda.sdc.dpws.http.jetty;

import org.eclipse.jetty.server.HttpInput;
import org.eclipse.jetty.util.component.Destroyable;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.somda.sdc.common.logging.InstanceLogger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

/**
 * {@linkplain HttpInput.Interceptor} which logs messages to a stream.
 */
public class CommunicationLogInputInterceptor implements HttpInput.Interceptor, Destroyable {
    private static final Logger LOG = LogManager.getLogger(CommunicationLogInputInterceptor.class);

    private OutputStream[] commlogStreams;
    private final Logger instanceLogger;

    CommunicationLogInputInterceptor(String frameworkIdentifier, OutputStream... commlogStreams) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.commlogStreams = commlogStreams;
    }

    /**
     * Set the list of OutputStreams.
     * @param commlogStreams list of OutputStreams to log the Messages into
     */
    public void setCommlogStreams(OutputStream... commlogStreams) {
        this.commlogStreams = commlogStreams;
    }

    @Override
    public HttpInput.Content readFrom(@Nullable HttpInput.Content content) {
        if (content == null) {
            // why is this a thing?
            return null;
        }

        try {
            if (content.isSpecial()) {
                if (content.isEof()) {
                    for (OutputStream commLogStream : commlogStreams) {
                        commLogStream.close();
                    }
                    instanceLogger.trace("EOF received, commlog stream closed");
                } else if (content.getError() != null) {
                    for (OutputStream commLogStream : commlogStreams) {
                        commLogStream.close();
                    }
                    instanceLogger.debug("Commlog stream closed, jetty reported error ", content.getError());
                }
                // don't do anything about other special types
            } else {
                int oldPosition = content.getByteBuffer().position();

                for (OutputStream commLogStream : commlogStreams) {
                    WritableByteChannel writableByteChannel = Channels.newChannel(commLogStream);
                    writableByteChannel.write(content.getByteBuffer());
                    // rewind the bytebuffer we just went through
                    content.getByteBuffer().position(oldPosition);
                }
            }
        } catch (IOException e) {
            instanceLogger.error("Error while writing to communication log stream", e);
        }

        return content;
    }

    @Override
    public void destroy() {
        try {
            for (OutputStream commLogStream : commlogStreams) {
                commLogStream.close();
            }
        } catch (IOException e) {
            instanceLogger.error("Error while closing communication log stream", e);
        }
    }
}
