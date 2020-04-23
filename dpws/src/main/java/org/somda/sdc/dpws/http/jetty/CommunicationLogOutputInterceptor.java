package org.somda.sdc.dpws.http.jetty;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.HttpApplicationInfo;
import org.somda.sdc.dpws.soap.TransportInfo;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

/**
 * {@linkplain HttpOutput.Interceptor} which logs messages to a stream.
 */
public class CommunicationLogOutputInterceptor implements HttpOutput.Interceptor {
    private static final Logger LOG = LoggerFactory.getLogger(CommunicationLogOutputInterceptor.class);

    private final HttpChannel channel;
    private final CommunicationLog communicationLog;
    private final TransportInfo transportInfo;
    private OutputStream commlogStream;
    private HttpOutput.Interceptor nextInterceptor;

    CommunicationLogOutputInterceptor(HttpChannel channel,
                                      HttpOutput.Interceptor nextInterceptor,
                                      CommunicationLog communicationLog,
                                      TransportInfo transportInfo) {
        this.channel = channel;
        this.communicationLog = communicationLog;
        this.nextInterceptor = nextInterceptor;
        this.transportInfo = transportInfo;
        this.commlogStream = null;
    }

    @Override
    public void write(ByteBuffer content, boolean last, Callback callback) {
        if (content == null) {
            nextInterceptor.write(content, last, callback);
            return;
        }

        // get commlog handle
        if (this.commlogStream == null) {
            this.commlogStream = getCommlogStream();
        }

        int oldPosition = content.position();
        try {
            WritableByteChannel writableByteChannel = Channels.newChannel(commlogStream);
            writableByteChannel.write(content);
            if (last) {
                commlogStream.close();
            }
        } catch (IOException e) {
            LOG.error("Error while writing to commlog", e);
        }

        // rewind the byte buffer we just went through
        content.position(oldPosition);
        nextInterceptor.write(content, last, callback);
    }

    private OutputStream getCommlogStream() {
        var response = channel.getResponse();
        ListMultimap<String, String> responseHeaderMap = ArrayListMultimap.create();
        response.getHeaderNames().stream()
                .map(String::toLowerCase)
                // filter duplicates which occur because of capitalization
                .distinct()
                .forEach(
                        headerName -> {
                            var headers = response.getHeaders(headerName);
                            headers.forEach(header ->
                                    responseHeaderMap.put(headerName, header)
                            );
                        }
                );

        var responseHttpApplicationInfo = new HttpApplicationInfo(
                responseHeaderMap
        );

        var responseCommContext = new CommunicationContext(responseHttpApplicationInfo, transportInfo);

        return communicationLog.logMessage(
                CommunicationLog.Direction.OUTBOUND,
                CommunicationLog.TransportType.HTTP,
                responseCommContext);
    }

    @Override
    public HttpOutput.Interceptor getNextInterceptor() {
        return nextInterceptor;
    }

    @Override
    public boolean isOptimizedForDirectBuffers() {
        return false;
    }
}
