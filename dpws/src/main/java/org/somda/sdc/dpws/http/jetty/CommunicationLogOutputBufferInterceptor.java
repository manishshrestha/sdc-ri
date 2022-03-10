package org.somda.sdc.dpws.http.jetty;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.util.Callback;
import org.somda.sdc.common.logging.InstanceLogger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * {@linkplain HttpOutput.Interceptor} which logs messages into a buffer.
 */
public class CommunicationLogOutputBufferInterceptor implements HttpOutput.Interceptor {
    private static final Logger LOG = LogManager.getLogger(CommunicationLogOutputBufferInterceptor.class);

    private final Logger instanceLogger;
    private final HttpServletResponse response;
    private ByteArrayOutputStream bufferStream;
    private final HttpOutput.Interceptor nextInterceptor;
    private HashMap<String, Collection<String>> lastResponseHeaders;

    CommunicationLogOutputBufferInterceptor(HttpOutput.Interceptor nextInterceptor,
                                            String frameworkIdentifier,
                                            HttpServletResponse response) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.nextInterceptor = nextInterceptor;
        this.bufferStream = null;
        this.response = response;
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

        // capture the headers as well
        lastResponseHeaders = getHeadersFromResponse(this.response);

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

    public Map<String, Collection<String>> getResponseHeaders() {
        return lastResponseHeaders;
    }

    private HashMap<String, Collection<String>> getHeadersFromResponse(HttpServletResponse response) {
        HashMap<String, Collection<String>> result = new HashMap<>();
        for (String headerName : response.getHeaderNames()) {
            final Collection<String> value = response.getHeaders(headerName);
            result.put(headerName, value);
        }
        return result;
    }

    @Override
    public HttpOutput.Interceptor getNextInterceptor() {
        return nextInterceptor;
    }

}
