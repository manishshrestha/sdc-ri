package org.somda.sdc.dpws.http.jetty;

import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.component.Destroyable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
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

    private final ByteArrayOutputStream bufferStream;
    private boolean writeFinished;
    private boolean commlogWritten;
    private OutputStream commlogStream;
    private HttpOutput.Interceptor nextInterceptor;

    CommunicationLogOutputInterceptor(HttpOutput.Interceptor nextInterceptor) {
        this.nextInterceptor = nextInterceptor;
        this.bufferStream = new ByteArrayOutputStream();
        this.commlogStream = null;
        this.commlogWritten = false;
        this.writeFinished = false;
    }

    /**
     * Set final destination output stream which handler will write to on destruction
     *
     * @param commlogStream an output stream
     */
    public void setCommlogStream(OutputStream commlogStream) {
        this.commlogStream = commlogStream;
        writeCommlogStream();
    }

    @Override
    public void write(ByteBuffer content, boolean last, Callback callback) {
        if (content == null) {
            nextInterceptor.write(content, last, callback);
            return;
        }
        int oldPosition = content.position();
        try {
            WritableByteChannel writableByteChannel = Channels.newChannel(bufferStream);
            writableByteChannel.write(content);
        } catch (IOException e) {
            LOG.error("Error while writing to commlog", e);
        }
        // rewind the bytebuffer we just went through
        content.position(oldPosition);
        nextInterceptor.write(content, last, callback);
        if (last) {
            writeFinished = true;
            writeCommlogStream();
        }
    }

    synchronized private void writeCommlogStream() {
        if (writeFinished && this.commlogStream != null && !this.commlogWritten) {
            this.commlogWritten = true;
            try {
                bufferStream.writeTo(this.commlogStream);
                this.commlogStream.close();
            } catch (IOException e) {
                LOG.error("Error while writing to commlog stream", e);
            }
        }
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
        long endTime = System.currentTimeMillis() + 1000;
        while (this.commlogStream == null || endTime > System.currentTimeMillis()) {
            // wait for commlogStream to be set
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // don't care
            }
        }
        // ensure write is done
        writeCommlogStream();

        try {
            this.bufferStream.close();
            if (this.commlogStream != null) {
                this.commlogStream.close();
            }
        } catch (IOException e) {
            LOG.error("Error while closing commlog stream", e);
        }
    }
}
