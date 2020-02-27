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
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


/**
 * {@linkplain AbstractHandler} implementation based on Jetty HTTP servers.
 */
public class JettyHttpServerHandler extends AbstractHandler {
    private static final Logger LOG = LoggerFactory.getLogger(JettyHttpServerHandler.class);

    private final String mediaType;
    private final HttpHandler handler;
    private final CommunicationLog communicationLog;
    private final Boolean expectTLS;

    @Inject
    JettyHttpServerHandler(@Assisted Boolean expectTLS,
                           @Assisted String mediaType,
                           @Assisted HttpHandler handler,
                           CommunicationLog communicationLog) {
        this.mediaType = mediaType;
        this.handler = handler;
        this.communicationLog = communicationLog;
        this.expectTLS = expectTLS;
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

        try (OutputStream output = communicationLog.logMessage(
                CommunicationLog.Direction.OUTBOUND,
                CommunicationLog.TransportType.HTTP,
                request.getRemoteHost(), request.getRemotePort(), response.getOutputStream())) {

            try {

                handler.process(input, output,
                        new TransportInfo(
                                request.getScheme(),
                                request.getLocalAddr(),
                                request.getLocalPort(),
                                request.getRemoteAddr(),
                                request.getRemotePort(),
                                getX509Certificates(request)));

            } catch (TransportException | MarshallingException | ClassCastException e) {
                LOG.error("", e);
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
                output.write(e.getMessage().getBytes());
                output.flush();
            } finally {
                baseRequest.setHandled(true);
            }
        }
    }

    private Collection<X509Certificate> getX509Certificates(HttpServletRequest request) throws IOException {
        var anonymousCertificates = request.getAttribute("javax.servlet.request.X509Certificate");
        if (this.expectTLS) {
            if (anonymousCertificates == null) {
                LOG.error("Certificate information is missing from HTTP request data");
                throw new IOException("Certificate information is missing from HTTP request data");
            } else {
                if (anonymousCertificates instanceof X509Certificate[]) {
                    return List.of((X509Certificate[]) anonymousCertificates);
                } else {
                    LOG.error("Certificate information is of an unexpected type: {}", anonymousCertificates.getClass());
                    throw new IOException(String.format("Certificate information is of an unexpected type: %s",
                            anonymousCertificates.getClass()));
                }
            }
        }
        return Collections.emptyList();

    }
}
