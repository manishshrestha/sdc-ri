package org.somda.sdc.dpws.http.jetty;

import org.eclipse.jetty.server.HttpInput;
import org.eclipse.jetty.util.component.Destroyable;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

/**
 * {@linkplain HttpInput.Interceptor} which logs messages to a stream.
 */
public class CommunicationLogInputInterceptor implements HttpInput.Interceptor, Destroyable {
    private static final Logger LOG = LogManager.getLogger(CommunicationLogInputInterceptor.class);

    private final OutputStream commlogStream;

    CommunicationLogInputInterceptor(OutputStream commlogStream) {
        this.commlogStream = commlogStream;
    }

    @Override
    public HttpInput.Content readFrom(HttpInput.Content content) {
        if (content == null) {
            // why is this a thing?
            return null;
        }

        int oldPosition = content.getByteBuffer().position();
        try {
            WritableByteChannel writableByteChannel = Channels.newChannel(commlogStream);
            writableByteChannel.write(content.getByteBuffer());
            if (content instanceof HttpInput.EofContent) {
                commlogStream.close();
            }
        } catch (IOException e) {
            LOG.error("Error while writing to communication log stream", e);
        }

        // rewind the bytebuffer we just went through
        content.getByteBuffer().position(oldPosition);
        return new HttpInput.Content(content.getByteBuffer());
    }

    @Override
    public void destroy() {
        try {
            commlogStream.close();
        } catch (IOException e) {
            LOG.error("Error while closing communication log stream", e);
        }
    }
}
