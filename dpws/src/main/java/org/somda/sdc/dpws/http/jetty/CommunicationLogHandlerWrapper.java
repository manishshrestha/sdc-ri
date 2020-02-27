package org.somda.sdc.dpws.http.jetty;

import com.google.inject.Inject;
import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.HttpApplicationInfo;
import org.somda.sdc.dpws.soap.TransportInfo;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * {@linkplain HandlerWrapper} which enables {@linkplain CommunicationLog} capabilities for requests and responses
 */
public class CommunicationLogHandlerWrapper extends HandlerWrapper {

    private final CommunicationLog commLog;
    private final boolean expectTLS;

    @Inject
    CommunicationLogHandlerWrapper(CommunicationLog commLog, boolean expectTLS) {
        this.commLog = commLog;
        this.expectTLS = expectTLS;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        // collect information for HttpApplicationInfo
        Map<String, String> requestHeaderMap = new HashMap<>();
        request.getHeaderNames().asIterator().forEachRemaining(
                headerName -> requestHeaderMap.put(headerName, request.getHeader(headerName))
        );

        var requestHttpApplicationInfo = new HttpApplicationInfo(
                requestHeaderMap
        );

        // collect information for TransportInfo
        var requestCertificates = JettyHttpServerHandler.getX509Certificates(request, expectTLS);
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
                requestCommContext);
        var out = baseRequest.getResponse().getHttpOutput();

        // attach interceptor to log request
        baseRequest.getHttpInput().addInterceptor(new CommunicationLogInputInterceptor(input));

        HttpOutput.Interceptor previousInterceptor = out.getInterceptor();
        try (ByteArrayOutputStream outputMessage = new ByteArrayOutputStream()) {
            // attach interceptor to log response
            new CommunicationLogOutputInterceptor(outputMessage, previousInterceptor);

            // trigger request handling
            super.handle(target, baseRequest, request, response);

            Map<String, String> responseHeaderMap = new HashMap<>();
            request.getHeaderNames().asIterator().forEachRemaining(
                    headerName -> responseHeaderMap.put(headerName, request.getHeader(headerName))
            );

            var responseHttpApplicationInfo = new HttpApplicationInfo(
                    responseHeaderMap
            );

            var responseCommContext = new CommunicationContext(responseHttpApplicationInfo, transportInfo);

            outputMessage.writeTo(commLog.logMessage(
                    CommunicationLog.Direction.OUTBOUND,
                    CommunicationLog.TransportType.HTTP,
                    responseCommContext));
        } finally {
            // reset interceptor if request not handled
            if (!baseRequest.isHandled() && !baseRequest.isAsyncStarted())
                out.setInterceptor(previousInterceptor);
        }
    }
}
