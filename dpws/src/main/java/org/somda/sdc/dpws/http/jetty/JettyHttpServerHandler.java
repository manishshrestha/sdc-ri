package org.somda.sdc.dpws.http.jetty;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.http.HttpHandler;
import org.somda.sdc.dpws.soap.TransportInfo;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.TransportException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * {@linkplain AbstractHandler} implementation based on Jetty HTTP servers.
 */
public class JettyHttpServerHandler extends AbstractHandler {
    private static final Logger LOG = LoggerFactory.getLogger(JettyHttpServerHandler.class);

    private final String mediaType;
    private final HttpHandler handler;
    private final CommunicationLog communicationLog;

    @Inject
    JettyHttpServerHandler(@Assisted String mediaType,
                           @Assisted HttpHandler handler,
                           CommunicationLog communicationLog) {
        this.mediaType = mediaType;
        this.handler = handler;
        this.communicationLog = communicationLog;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {

        LOG.debug("Request to {}", request.getRequestURL());

        InputStream input = communicationLog.logMessage(
                CommunicationLog.Direction.INBOUND,
                CommunicationLog.TransportType.HTTP,
                request.getRemoteHost(), request.getRemotePort(), request.getInputStream());

        response.setStatus(HttpStatus.OK_200);
        response.setContentType(mediaType);

        OutputStream output = communicationLog.logMessage(
                CommunicationLog.Direction.OUTBOUND,
                CommunicationLog.TransportType.HTTP,
                request.getRemoteHost(), request.getRemotePort(), response.getOutputStream());

        try {

            handler.process(input, output,
                    new TransportInfo(request.getScheme(), request.getLocalAddr(), request.getLocalPort()));

        } catch (TransportException | MarshallingException | ClassCastException e) {
            LOG.error("", e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
            output.write(e.getMessage().getBytes());
            output.flush();
            output.close();
        } finally {
            baseRequest.setHandled(true);
        }


    }
}
