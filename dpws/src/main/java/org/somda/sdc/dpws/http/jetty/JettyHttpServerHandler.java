package org.somda.sdc.dpws.http.jetty;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.http.HttpHandler;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.HttpApplicationInfo;
import org.somda.sdc.dpws.soap.TransportInfo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.*;


/**
 * {@linkplain AbstractHandler} implementation based on Jetty HTTP servers.
 */
public class JettyHttpServerHandler extends AbstractHandler {
    private static final Logger LOG = LoggerFactory.getLogger(JettyHttpServerHandler.class);
    public static final String SERVER_HEADER_KEY = "X-Server";
    public static final String SERVER_HEADER_VALUE = "SDCri";

    private final String mediaType;
    private final HttpHandler handler;
    private final CommunicationLog communicationLog;

    @AssistedInject
    JettyHttpServerHandler(@Assisted Boolean expectTLS,
                           @Assisted String mediaType,
                           @Assisted HttpHandler handler,
                           CommunicationLog communicationLog) {
        this(mediaType, handler, communicationLog);
    }

    @AssistedInject
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
        response.setStatus(HttpStatus.OK_200);
        response.setContentType(mediaType);
        response.setHeader(SERVER_HEADER_KEY, SERVER_HEADER_VALUE);

        var input = request.getInputStream();
        var output = response.getOutputStream();

        // collect information for HttpApplicationInfo
        Map<String, String> requestHeaderMap = new HashMap<>();
        request.getHeaderNames().asIterator().forEachRemaining(
                headerName -> requestHeaderMap.put(headerName, request.getHeader(headerName))
        );

        var requestHttpApplicationInfo = new HttpApplicationInfo(
                requestHeaderMap
        );

        try {
            handler.process(input, output,
                    new CommunicationContext(requestHttpApplicationInfo,
                            new TransportInfo(
                                    request.getScheme(),
                                    request.getLocalAddr(),
                                    request.getLocalPort(),
                                    request.getRemoteAddr(),
                                    request.getRemotePort(),
                                    getX509Certificates(request, baseRequest.isSecure())
                            )
                    )
            );

        } catch (Exception e) {
            LOG.error("An exception occurred during HTTP request processing: {}", e.getMessage());
            LOG.trace("An exception occurred during HTTP request processing", e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
            output.write(e.getMessage().getBytes());
            output.flush();
        } finally {
            baseRequest.setHandled(true);
        }
    }

    public static Collection<X509Certificate> getX509Certificates(HttpServletRequest request, boolean expectTLS) throws IOException {
        var anonymousCertificates = request.getAttribute("javax.servlet.request.X509Certificate");
        if (expectTLS) {
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
