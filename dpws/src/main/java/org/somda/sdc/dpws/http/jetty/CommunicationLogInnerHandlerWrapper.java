package org.somda.sdc.dpws.http.jetty;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.HttpApplicationInfo;
import org.somda.sdc.dpws.soap.TransportInfo;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;

/**
 * {@linkplain HandlerWrapper} which enables {@linkplain CommunicationLog} capabilities for requests and responses.
 * Inner Part that is called on the already decompressed Message.
 */
public class CommunicationLogInnerHandlerWrapper extends HandlerWrapper {
    public static final String MESSAGE_BODY_FROM_INNER_PART_AS_ATTRIBUTE_KEY =
        "MessageBody-From-CommLogInnerHandlerWrapper";
    public static final String MESSAGE_HEADERS_FROM_INNER_PART_AS_ATTRIBUTE_KEY =
        "MessageHeaders-From-CommLogInnerHandlerWrapper";
    private final CommunicationLog commLog;
    private final String frameworkIdentifier;

    CommunicationLogInnerHandlerWrapper(CommunicationLog commLog, String frameworkIdentifier) {
        this.frameworkIdentifier = frameworkIdentifier;
        this.commLog = commLog;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        String currentTransactionId = (String) baseRequest.getAttribute(CommunicationLog.MessageType.REQUEST.name());

        final ListMultimap<String, String> requestAppLevelHeaders = JettyUtil.getRequestHeaders(request);
        HttpFields requestNetLevelHeaders =
            (HttpFields) request.getAttribute(CommunicationLogOuterHandlerWrapper.NET_LEVEL_HEADERS_ATTRIBUTE);

        var requestAppLevelHttpApplicationInfo = new HttpApplicationInfo(
                requestAppLevelHeaders,
                currentTransactionId,
                baseRequest.getRequestURI()
        );
        var requestNetLevelHttpApplicationInfo = new HttpApplicationInfo(
            convertHeaders(requestNetLevelHeaders),
            currentTransactionId,
            baseRequest.getRequestURI()
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

        var requestAppLevelCommContext = new CommunicationContext(requestAppLevelHttpApplicationInfo, transportInfo);
        var requestNetLevelCommContext = new CommunicationContext(requestNetLevelHttpApplicationInfo, transportInfo);

        OutputStream appLevelCommLogStream = commLog.logMessage(
                CommunicationLog.Direction.INBOUND,
                CommunicationLog.TransportType.HTTP,
                CommunicationLog.MessageType.REQUEST,
                requestAppLevelCommContext,
                CommunicationLog.Level.APPLICATION);
        OutputStream netLevelCommLogStream = commLog.logRelatedMessage(
            appLevelCommLogStream,
            CommunicationLog.Direction.INBOUND,
            CommunicationLog.TransportType.HTTP,
            CommunicationLog.MessageType.REQUEST,
            requestNetLevelCommContext,
            CommunicationLog.Level.NETWORK);
        var out = baseRequest.getResponse().getHttpOutput();

        if (isGZipped(request)) {
            CommunicationLogInputInterceptor interceptor =
                (CommunicationLogInputInterceptor) baseRequest.
                    getAttribute(CommunicationLogOuterHandlerWrapper.REQUEST_CONTENT_INTERCEPTOR_IN_ATTRIBUTE_KEY);
            interceptor.setCommlogStreams(netLevelCommLogStream);
            // attach interceptor to log request
            baseRequest.getHttpInput()
                .addInterceptor(new CommunicationLogInputInterceptor(frameworkIdentifier, appLevelCommLogStream));
        } else {
            // attach interceptor to log request
            baseRequest.getHttpInput()
                .addInterceptor(
                    new CommunicationLogInputInterceptor(frameworkIdentifier,
                        appLevelCommLogStream, netLevelCommLogStream));
        }

        HttpOutput.Interceptor previousInterceptor = out.getInterceptor();

        try {
            // attach interceptor to log response
            var outInterceptor = new CommunicationLogOutputBufferInterceptor(
                previousInterceptor,
                frameworkIdentifier,
                response);

            out.setInterceptor(outInterceptor);

            // trigger request handling
            super.handle(target, baseRequest, request, response);

            request.setAttribute(MESSAGE_BODY_FROM_INNER_PART_AS_ATTRIBUTE_KEY,
                outInterceptor.getContents());
            request.setAttribute(MESSAGE_HEADERS_FROM_INNER_PART_AS_ATTRIBUTE_KEY,
                outInterceptor.getResponseHeaders());
        } finally {
            // reset interceptor if request not handled
            if (!baseRequest.isHandled() && !baseRequest.isAsyncStarted())
                out.setInterceptor(previousInterceptor);
        }
    }

    private ListMultimap<String, String> convertHeaders(HttpFields netLevelHeaders) {
        var result = ArrayListMultimap.<String, String>create();
        netLevelHeaders.forEach(field -> result.put(field.getName(), field.getValue()));
        return result;
    }

    private boolean isGZipped(@Nullable HttpServletRequest request) {
        if (request != null) {
            String header = request.getHeader("Content-Encoding");
            if (header == null) {
                header = request.getHeader("X-Content-Encoding");
            }
            if (header == null) {
                return false;
            }
            return header.contains("gzip");
        } else {
            return false;
        }
    }

}
