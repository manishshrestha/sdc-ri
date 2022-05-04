package org.somda.sdc.dpws.http.jetty;

import com.google.common.collect.ListMultimap;
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

        final ListMultimap<String, String> requestHeaders = JettyUtil.getRequestHeaders(request);
        final Object contentEncodingHeaderFromAttribute = baseRequest.getAttribute(
            CommunicationLogOuterHandlerWrapper.CONTENT_ENCODING_HEADER_PASSED_IN_ATTRIBUTE_KEY);
        if (requestHeaders.get("Content-Encoding").isEmpty()
            && contentEncodingHeaderFromAttribute != null) {
            requestHeaders.put("Content-Encoding", (String) contentEncodingHeaderFromAttribute);
        }

        var requestHttpApplicationInfo = new HttpApplicationInfo(
                requestHeaders,
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

        var requestCommContext = new CommunicationContext(requestHttpApplicationInfo, transportInfo);

        OutputStream input = commLog.logMessage(
                CommunicationLog.Direction.INBOUND,
                CommunicationLog.TransportType.HTTP,
                CommunicationLog.MessageType.REQUEST,
                requestCommContext,
                CommunicationLog.Level.APPLICATION);
        // TODO: add a network-level logMessage.
        var out = baseRequest.getResponse().getHttpOutput();

        // attach interceptor to log request
        baseRequest.getHttpInput().addInterceptor(new CommunicationLogInputInterceptor(input, frameworkIdentifier));

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

}
