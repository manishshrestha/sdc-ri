package org.somda.sdc.dpws.http.jetty;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.HttpApplicationInfo;
import org.somda.sdc.dpws.soap.TransportInfo;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * {@linkplain HandlerWrapper} which enables extracting Headers and passing them to {@linkplain CommunicationLogInnerHandlerWrapper}.
 * Outer Part that is called on the compressed Message.
 */
public class CommunicationLogOuterHandlerWrapper extends HandlerWrapper {
    private static final String TRANSACTION_ID_PREFIX_SERVER = "rrId:server:" + UUID.randomUUID() + ":";
    private static final AtomicLong TRANSACTION_ID = new AtomicLong(-1L);
    public static final String CONTENT_ENCODING_HEADER_PASSED_IN_ATTRIBUTE_KEY = "Content-Encoding-Header-From-Extractor";
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

        final String contentEncodingHeader = baseRequest.getHeader("Content-Encoding");

        baseRequest.setAttribute(CONTENT_ENCODING_HEADER_PASSED_IN_ATTRIBUTE_KEY, contentEncodingHeader);

        // trigger request handling
        super.handle(target, baseRequest, request, response);

        final Object messageBody = request.getAttribute(
            CommunicationLogInnerHandlerWrapper.MESSAGE_BODY_FROM_COMM_LOG_HANDLER_WRAPPER_AS_ATTRIBUTE_KEY);
        if (messageBody != null) {
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
                responseHeaderMap,
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

            var responseCommContext = new CommunicationContext(responseHttpApplicationInfo, transportInfo);

            final OutputStream outputStream = communicationLog.logMessage(
                CommunicationLog.Direction.OUTBOUND,
                CommunicationLog.TransportType.HTTP,
                CommunicationLog.MessageType.RESPONSE,
                responseCommContext
            );
            outputStream.write((byte[])messageBody);
            outputStream.close();
        }
    }
}
