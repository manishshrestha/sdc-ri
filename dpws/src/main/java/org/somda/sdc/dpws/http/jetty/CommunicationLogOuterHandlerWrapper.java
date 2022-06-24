package org.somda.sdc.dpws.http.jetty;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.HttpApplicationInfo;
import org.somda.sdc.dpws.soap.TransportInfo;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * {@linkplain HandlerWrapper} which enables extracting Headers and passing them to
 * {@linkplain CommunicationLogInnerHandlerWrapper}.
 * Outer Part that is called on the compressed Message.
 */
public class CommunicationLogOuterHandlerWrapper extends HandlerWrapper {
    public static final String REQUEST_CONTENT_INTERCEPTOR_IN_ATTRIBUTE_KEY =
        "Request-Content-Interceptor-From-Outer-Wrapper";
    public static final String NET_LEVEL_HEADERS_ATTRIBUTE = "NetLevelHeaderAttribute";
    private static final String TRANSACTION_ID_PREFIX_SERVER = "rrId:server:" + UUID.randomUUID() + ":";
    private static final AtomicLong TRANSACTION_ID = new AtomicLong(-1L);
    private final String frameworkIdentifier;
    private final CommunicationLog communicationLog;

    CommunicationLogOuterHandlerWrapper(CommunicationLog communicationLog,
                                        String frameworkIdentifier) {
        this.communicationLog = communicationLog;
        this.frameworkIdentifier = frameworkIdentifier;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {

        var currentTransactionId = TRANSACTION_ID_PREFIX_SERVER + TRANSACTION_ID.incrementAndGet();
        baseRequest.setAttribute(CommunicationLog.MessageType.REQUEST.name(), currentTransactionId);

        final HttpFields netLevelHeaders = baseRequest.getHttpFields();
        baseRequest.setAttribute(NET_LEVEL_HEADERS_ATTRIBUTE, netLevelHeaders);

        final CommunicationLogInputInterceptor inputInterceptor =
            new CommunicationLogInputInterceptor(frameworkIdentifier);
        baseRequest.getHttpInput().addInterceptor(inputInterceptor);
        baseRequest.setAttribute(REQUEST_CONTENT_INTERCEPTOR_IN_ATTRIBUTE_KEY, inputInterceptor);

        final HttpOutput out = baseRequest.getResponse().getHttpOutput();
        final HttpOutput.Interceptor previousInterceptor = out.getInterceptor();

        final var outInterceptor = new CommunicationLogOutputBufferInterceptor(
            previousInterceptor,
            frameworkIdentifier,
            response);

        out.setInterceptor(outInterceptor);

        // trigger request handling
        super.handle(target, baseRequest, request, response);

        final Object responseBody = request.getAttribute(
            CommunicationLogInnerHandlerWrapper.MESSAGE_BODY_FROM_INNER_PART_AS_ATTRIBUTE_KEY);
        final HashMap<String, Collection<String>> responseAppLevelHeaders = (HashMap<String, Collection<String>>)
            request.getAttribute(
                CommunicationLogInnerHandlerWrapper.MESSAGE_HEADERS_FROM_INNER_PART_AS_ATTRIBUTE_KEY);

        // NOTE: we log the headers extracted in the CommunicationLogOutputBufferInterceptor
        //       because this is the only place where the Content-Length Header can be extracted
        //       while it still contains the correct length of the decompressed body.
        var responseAppLevelHttpApplicationInfo = new HttpApplicationInfo(
            extractHeadersFromMap(responseAppLevelHeaders),
            currentTransactionId,
            null
        );
        final byte[] responseNetLevelBody = outInterceptor.getContents();
        final ListMultimap<String, String> responseNetLevelHeaders = extractHeadersFromResponse(response);
        // NOTE: in case of a gzipped Response, Jetty omits the Content-Length Header at this point, but
        // adds it later (it is present on the network). For this reason we add it manually.
        responseNetLevelHeaders.put("Content-Length", Integer.toString(responseNetLevelBody.length));
        var responseNetLevelHttpApplicationInfo = new HttpApplicationInfo(
            responseNetLevelHeaders,
            currentTransactionId,
            null
        );

        // collect information for TransportInfo
        var requestCertificates = JettyHttpServerHandler.getX509Certificates(request, baseRequest.isSecure());
        var transportInfo = new TransportInfo(
            request.getScheme(),
            request.getLocalAddr(),
            request.getLocalPort(),
            request.getRemoteAddr(),
            request.getRemotePort(),
            requestCertificates
        );

        var responseAppLevelCommContext = new CommunicationContext(responseAppLevelHttpApplicationInfo, transportInfo);
        var responseNetLevelCommContext = new CommunicationContext(responseNetLevelHttpApplicationInfo, transportInfo);

        final OutputStream appLevelCommLogStream = communicationLog.logMessage(
            CommunicationLog.Direction.OUTBOUND,
            CommunicationLog.TransportType.HTTP,
            CommunicationLog.MessageType.RESPONSE,
            responseAppLevelCommContext,
            CommunicationLog.Level.APPLICATION
        );
        final OutputStream netLevelCommLogStream = communicationLog.logMessage(
            CommunicationLog.Direction.OUTBOUND,
            CommunicationLog.TransportType.HTTP,
            CommunicationLog.MessageType.RESPONSE,
            responseNetLevelCommContext,
            CommunicationLog.Level.NETWORK
        );

        if (responseBody != null) {
            appLevelCommLogStream.write((byte[]) responseBody);
            appLevelCommLogStream.close();
            final String contentEncodingResponseHeader2 = response.getHeader("Content-Encoding");
            if (contentEncodingResponseHeader2 != null && contentEncodingResponseHeader2.contains("gzip")) {
                netLevelCommLogStream.write(responseNetLevelBody);
            } else {
                netLevelCommLogStream.write((byte[]) responseBody);
            }
            netLevelCommLogStream.close();
        }
    }

    private ListMultimap<String, String> extractHeadersFromMap(
        @Nullable HashMap<String, Collection<String>> map) {
        ListMultimap<String, String> result = ArrayListMultimap.create();
        if (map != null) {
            for (String headerName : map.keySet()) {
                for (String value : map.get(headerName)) {
                    result.put(headerName.toLowerCase(), value);
                }
            }
        }
        return result;
    }

    private ListMultimap<String, String> extractHeadersFromResponse(HttpServletResponse response) {
        var result = ArrayListMultimap.<String, String>create();
        for (String headerName: response.getHeaderNames()) {
            result.put(headerName, response.getHeader(headerName));
        }
        return result;
    }
}
