package org.somda.sdc.dpws.http.jetty;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.DpwsConfig;
import org.somda.sdc.dpws.http.HttpException;
import org.somda.sdc.dpws.http.HttpHandler;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.HttpApplicationInfo;
import org.somda.sdc.dpws.soap.TransportInfo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


/**
 * {@linkplain AbstractHandler} implementation based on Jetty HTTP servers.
 */
public class JettyHttpServerHandler extends AbstractHandler {
    public static final String SERVER_HEADER_KEY = "X-Server";
    public static final String SERVER_HEADER_VALUE = "SDCri";

    private static final Logger LOG = LogManager.getLogger(JettyHttpServerHandler.class);

    private final String mediaType;
    private final HttpHandler handler;
    private final CommunicationLog communicationLog;
    private final Logger instanceLogger;
    private final boolean chunkedTransfer;

    @AssistedInject
    JettyHttpServerHandler(@Assisted Boolean expectTLS,
                           @Assisted String mediaType,
                           @Assisted HttpHandler handler,
                           CommunicationLog communicationLog,
                           @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier,
                           @Named(DpwsConfig.ENFORCE_HTTP_CHUNKED_TRANSFER) boolean chunkedTransfer) {
        this(mediaType, handler, communicationLog, frameworkIdentifier, chunkedTransfer);
    }

    @AssistedInject
    JettyHttpServerHandler(@Assisted String mediaType,
                           @Assisted HttpHandler handler,
                           CommunicationLog communicationLog,
                           @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier,
                           @Named(DpwsConfig.ENFORCE_HTTP_CHUNKED_TRANSFER) boolean chunkedTransfer) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.mediaType = mediaType;
        this.handler = handler;
        this.communicationLog = communicationLog;
        this.chunkedTransfer = chunkedTransfer;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        var transactionIdOpt = Optional.of(baseRequest.getAttribute(CommunicationLog.MessageType.REQUEST.name()));
        var transactionId = (String) transactionIdOpt.orElse("");

        instanceLogger.debug("Request to {}", request.getRequestURL());
        response.setStatus(HttpStatus.OK_200);
        response.setContentType(mediaType);
        response.setHeader(SERVER_HEADER_KEY, SERVER_HEADER_VALUE);

        var input = request.getInputStream();

        ByteArrayOutputStream tempOut = new ByteArrayOutputStream();

        var requestHttpApplicationInfo = new HttpApplicationInfo(
                JettyUtil.getRequestHeaders(request),
                transactionId,
                null
        );

        try {
            handler.handle(input, tempOut,
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

        } catch (HttpException e) {
            instanceLogger.warn("An HTTP exception occurred during HTTP request processing. {}", e.getMessage());
            instanceLogger.trace("An HTTP exception occurred during HTTP request processing", e);
            response.setStatus(e.getStatusCode());
            if (!e.getMessage().isEmpty()) {
                tempOut.write(e.getMessage().getBytes());
            }
        } finally {
            baseRequest.setHandled(true);
        }

        final byte[] tempOutValue = tempOut.toByteArray();

        if (this.chunkedTransfer) {
            response.setHeader("Transfer-Encoding", "chunked");
        } else {
            response.setHeader("Content-Length", String.valueOf(tempOutValue.length));
        }

        OutputStream output = response.getOutputStream();
        output.write(tempOutValue);

        try {
            input.close();
            output.flush();
            output.close();
        } catch (IOException e) {
            instanceLogger.error("Could not close input/output streams from incoming HTTP request to {}. Reason: {}",
                    request.getRequestURL(), e.getMessage());
            instanceLogger.trace("Could not close input/output streams from incoming HTTP request to {}",
                    request.getRequestURL(), e);
        }
    }

    /**
     * Static helper function to get X509 certificate information from an HTTP servlet.
     *
     * @param request   servlet request data.
     * @param expectTLS causes this function to return an empty list if set to false.
     * @return a list of {@link X509Certificate} containers.
     * @throws IOException in case the certificate information does not match the expected type, which is an array of
     *                     {@link X509Certificate}.
     * @deprecated this function is deprecated as it was supposed to be used internally only. The visibility of this
     * function will be degraded to package private with SDCri 2.0.
     */
    @Deprecated(since = "1.1.0", forRemoval = false)
    public static List<X509Certificate> getX509Certificates(HttpServletRequest request, boolean expectTLS)
            throws IOException {
        if (!expectTLS) {
            return Collections.emptyList();
        }

        var anonymousCertificates = request.getAttribute("javax.servlet.request.X509Certificate");
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
}
