package org.somda.sdc.dpws.http.jetty;

import org.eclipse.jetty.server.HttpInput;
import org.eclipse.jetty.util.component.Destroyable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

public class CommunicationLogInputInterceptor implements HttpInput.Interceptor, Destroyable {
    private static final Logger LOG = LoggerFactory.getLogger(CommunicationLogInputInterceptor.class);

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
        try {
            WritableByteChannel writableByteChannel = Channels.newChannel(commlogStream);
            writableByteChannel.write(content.getByteBuffer());
            // reset the bytebuffer we just went through
            content.getByteBuffer().rewind();
        } catch (IOException e) {
            LOG.error("Error while writing to communication log stream", e);
        }
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
