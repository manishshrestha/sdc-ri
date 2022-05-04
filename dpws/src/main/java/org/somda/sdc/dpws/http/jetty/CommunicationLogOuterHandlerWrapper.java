package org.somda.sdc.dpws.http.jetty;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.HttpApplicationInfo;
import org.somda.sdc.dpws.soap.TransportInfo;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.lang.String;

/**
 * {@linkplain HandlerWrapper} which enables extracting Headers and passing them to
 * {@linkplain CommunicationLogInnerHandlerWrapper}.
 * Outer Part that is called on the compressed Message.
 */
public class CommunicationLogOuterHandlerWrapper extends HandlerWrapper {
    public static final String CONTENT_ENCODING_HEADER_PASSED_IN_ATTRIBUTE_KEY =
        "Content-Encoding-Header-From-Extractor";
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

        final String contentEncodingHeader = baseRequest.getHeader("Content-Encoding");

        baseRequest.setAttribute(CONTENT_ENCODING_HEADER_PASSED_IN_ATTRIBUTE_KEY, contentEncodingHeader);

        final HttpOutput out = baseRequest.getResponse().getHttpOutput();
        final HttpOutput.Interceptor previousInterceptor = out.getInterceptor();

        final var outInterceptor = new CommunicationLogOutputBufferInterceptor(
            previousInterceptor,
            frameworkIdentifier,
            response);

        out.setInterceptor(outInterceptor);

        // trigger request handling
        super.handle(target, baseRequest, request, response);

        final Object messageBody = request.getAttribute(
            CommunicationLogInnerHandlerWrapper.MESSAGE_BODY_FROM_INNER_PART_AS_ATTRIBUTE_KEY);
        final HashMap<String, Collection<String>> responseHeaders = (HashMap<String, Collection<String>>)
            request.getAttribute(
            CommunicationLogInnerHandlerWrapper.MESSAGE_HEADERS_FROM_INNER_PART_AS_ATTRIBUTE_KEY);
        if (messageBody != null) {
            ListMultimap<String, String> responseHeaderMap = ArrayListMultimap.create();
            // NOTE: we log the headers extracted in the CommunicationLogOutputBufferInterceptor
            //       because this is the only place where the Content-Length Header can be extracted
            //       while it still contains the correct length of the decompressed body.
            //       However, we need to add the Content-Encoding Header at this place because it is
            //       set in the GzipHandler.
            // TODO: As stated in the above note, the combination of these Headers and the uncompressed body
            //       (also extracted in the CommunicationLogOutputBufferInterceptor) is inconsistent. Change
            //       this by logging 2 versions of both Requests and Responses:
            //       a network version and an uncompressed version.
            if (responseHeaders != null) {
                for (String headerName : responseHeaders.keySet()) {
                    for (String value : responseHeaders.get(headerName)) {
                        responseHeaderMap.put(headerName.toLowerCase(), value);
                    }
                }
            }
            // add the Content-Encoding Header to the headers from the Inner Part as it is set in the GzipHandler
            final String contentEncodingResponseHeader = response.getHeader("Content-Encoding");
            if (contentEncodingResponseHeader != null) {
                responseHeaderMap.put("Content-Encoding", contentEncodingResponseHeader);
            }

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
                responseCommContext,
                CommunicationLog.Level.APPLICATION
            );
            // TODO: add a network-level logMessage.
            // NOTE: the gzipped content of the low-level response can at this point be extracted from the outInterceptor.
            // outInterceptor.getContents();

            outputStream.write((byte[]) messageBody);
            outputStream.close();
        }
    }
}
