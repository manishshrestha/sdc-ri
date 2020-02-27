package org.somda.sdc.dpws.http.jetty;

import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.component.Destroyable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

/**
 * {@linkplain HttpOutput.Interceptor} which logs messages to a stream
 */
public class CommunicationLogOutputInterceptor implements HttpOutput.Interceptor, Destroyable {
    private static final Logger LOG = LoggerFactory.getLogger(CommunicationLogOutputInterceptor.class);

    private final OutputStream commlogStream;
    private HttpOutput.Interceptor nextInterceptor;

    CommunicationLogOutputInterceptor(OutputStream commlogStream, HttpOutput.Interceptor nextInterceptor) {
        this.commlogStream = commlogStream;
        this.nextInterceptor = nextInterceptor;
    }

    @Override
    public void write(ByteBuffer content, boolean last, Callback callback) {
        int oldPosition = content.position();
        try {
            WritableByteChannel writableByteChannel = Channels.newChannel(commlogStream);
            writableByteChannel.write(content);
            commlogStream.write(content.array());
        } catch (IOException e) {
            LOG.error("Error while writing to commlog", e);
        }
        // rewind the bytebuffer we just went through
        content.position(oldPosition);
        nextInterceptor.write(content, last, callback);
    }

    @Override
    public HttpOutput.Interceptor getNextInterceptor() {
        return nextInterceptor;
    }

    @Override
    public boolean isOptimizedForDirectBuffers() {
        return false;
    }

    @Override
    public void destroy() {
        try {
            this.commlogStream.close();
        } catch (IOException e) {
            LOG.error("Error while closing commlog stream", e);
        }
    }
}
