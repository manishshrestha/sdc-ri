package org.somda.sdc.dpws.http.jetty;

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
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * {@linkplain HandlerWrapper} which enables {@linkplain CommunicationLog} capabilities for requests and responses.
 */
public class CommunicationLogHandlerWrapper extends HandlerWrapper {
    private static final String TRANSACTION_ID_PREFIX_SERVER = "rrId:server:" + UUID.randomUUID() + ":";
    private static final AtomicLong TRANSACTION_ID = new AtomicLong(-1L);

    private final CommunicationLog commLog;
    private final String frameworkIdentifier;

    CommunicationLogHandlerWrapper(CommunicationLog commLog, String frameworkIdentifier) {
        this.frameworkIdentifier = frameworkIdentifier;
        this.commLog = commLog;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        var currentTransactionId = TRANSACTION_ID_PREFIX_SERVER + TRANSACTION_ID.incrementAndGet();
        baseRequest.setAttribute(CommunicationLog.MessageType.REQUEST.name(), currentTransactionId);

        var requestHttpApplicationInfo = new HttpApplicationInfo(
                JettyUtil.getRequestHeaders(request),
                currentTransactionId,
                request.getRequestURL().toString()
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
                requestCommContext);
        var out = baseRequest.getResponse().getHttpOutput();

        // attach interceptor to log request
        var inputInterceptor = new CommunicationLogInputInterceptor(input, frameworkIdentifier);
        baseRequest.getHttpInput().addInterceptor(inputInterceptor);

        HttpOutput.Interceptor previousInterceptor = out.getInterceptor();

        CommunicationLogOutputInterceptor outInterceptor = null;
        try {
            // attach interceptor to log response
            outInterceptor = new CommunicationLogOutputInterceptor(
                baseRequest.getHttpChannel(),
                previousInterceptor,
                commLog,
                transportInfo,
                frameworkIdentifier,
                currentTransactionId
            );

            out.setInterceptor(outInterceptor);

            // trigger request handling
            super.handle(target, baseRequest, request, response);
        } finally {
            // TODO: Jetty 11 does not call destroy on input interceptors anymore, which is why we have to call it
            //  manually here. To retain the order of close operations, the output interceptor is closed here as well,
            //  even though it does work correctly. Fix this one once Jetty changes this behavior.
            //  See https://github.com/eclipse/jetty.project/issues/7280 
            inputInterceptor.destroy();
            if (outInterceptor != null) {
                outInterceptor.close();
            }

            // reset interceptor if request not handled
            if (!baseRequest.isHandled() && !baseRequest.isAsyncStarted())
                out.setInterceptor(previousInterceptor);
        }
    }
}
