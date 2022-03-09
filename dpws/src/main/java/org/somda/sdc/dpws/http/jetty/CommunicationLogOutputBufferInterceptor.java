package org.somda.sdc.dpws.http.jetty;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.util.Callback;
import org.somda.sdc.common.logging.InstanceLogger;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

/**
 * {@linkplain HttpOutput.Interceptor} which logs messages into a buffer.
 */
public class CommunicationLogOutputBufferInterceptor implements HttpOutput.Interceptor {
    private static final Logger LOG = LogManager.getLogger(CommunicationLogOutputBufferInterceptor.class);

    private final Logger instanceLogger;
    private ByteArrayOutputStream bufferStream;
    private final HttpOutput.Interceptor nextInterceptor;

    CommunicationLogOutputBufferInterceptor(HttpOutput.Interceptor nextInterceptor,
                                            String frameworkIdentifier) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.nextInterceptor = nextInterceptor;
        this.bufferStream = null;
    }

    /***
     * Closes the underlying communication log stream if present.
     */
    public void close() {
        try {
            if (bufferStream != null) {
                bufferStream.close();
            }
        } catch (IOException e) {
            instanceLogger.error("Error while closing communication log stream", e);
        }
    }

    @Override
    public void write(ByteBuffer content, boolean last, Callback callback) {
        // get commlog handle
        if (this.bufferStream == null) {
            this.bufferStream = new ByteArrayOutputStream();
        }

        int oldPosition = content.position();
        try {
            WritableByteChannel writableByteChannel = Channels.newChannel(bufferStream);
            writableByteChannel.write(content);
            if (last) {
                close();
            }
        } catch (IOException e) {
            instanceLogger.error("Error while writing to commlog", e);
        }

        // rewind the byte buffer we just went through
        content.position(oldPosition);
        nextInterceptor.write(content, last, callback);
    }

    public byte[] getContents() {
        if (this.bufferStream != null) {
            return this.bufferStream.toByteArray();
        }
        return new byte[0];
    }

    @Override
    public HttpOutput.Interceptor getNextInterceptor() {
        return nextInterceptor;
    }

}
