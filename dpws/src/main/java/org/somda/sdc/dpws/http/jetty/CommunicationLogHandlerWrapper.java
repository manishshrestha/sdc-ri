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
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

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

        // log request
        // collect information for HttpApplicationInfo
        Map<String, String> headerMap = new HashMap<>();
        request.getHeaderNames().asIterator().forEachRemaining(
                headerName -> headerMap.put(headerName, request.getHeader(headerName))
        );

        var requestHttpApplicationInfo = new HttpApplicationInfo(
                headerMap
        );

        // collect information for TransportInfo
        var requestCertificates = JettyHttpServerHandler.getX509Certificates(request, expectTLS);
        var requestTransportInfo = new TransportInfo(
                request.getScheme(),
                request.getLocalAddr(),
                request.getLocalPort(),
                request.getRemoteAddr(),
                request.getRemotePort(),
                requestCertificates
        );

        var requestCommContext = new CommunicationContext(requestHttpApplicationInfo, requestTransportInfo);

        try (OutputStream input = commLog.logMessage(
                CommunicationLog.Direction.INBOUND,
                CommunicationLog.TransportType.HTTP,
                requestCommContext);
             OutputStream output = commLog.logMessage(
                     CommunicationLog.Direction.OUTBOUND,
                     CommunicationLog.TransportType.HTTP,
                     requestCommContext, response.getOutputStream())) {
            var out = baseRequest.getResponse().getHttpOutput();

            baseRequest.getHttpInput().addInterceptor(new CommunicationLogInputInterceptor(input));

            // log response handling
            HttpOutput.Interceptor previousInterceptor = out.getInterceptor();
            new CommunicationLogOutputInterceptor(output, previousInterceptor);

            try {
                super.handle(target, baseRequest, request, response);
            } finally {
                // reset interceptor if request not handled
                if (!baseRequest.isHandled() && !baseRequest.isAsyncStarted())
                    out.setInterceptor(previousInterceptor);
            }
        }


    }
}
