package org.somda.sdc.dpws.http.apache;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.http.apache.helper.ApacheClientHelper;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.HttpApplicationInfo;
import org.somda.sdc.dpws.soap.TransportInfo;

import java.io.IOException;
import java.io.OutputStream;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Request interceptor which writes the outgoing request message and headers into the {@linkplain CommunicationLog}.
 */
public class CommunicationLogOuterHttpRequestInterceptor implements HttpRequestInterceptor {
    private static final Logger LOG = LogManager.getLogger(CommunicationLogOuterHttpRequestInterceptor.class);
    private static final String TRANSACTION_ID_PREFIX_CLIENT = "rrId:client:" + UUID.randomUUID() + ":";
    private static final AtomicLong TRANSACTION_ID = new AtomicLong(-1L);

    private final CommunicationLog commlog;
    private final Logger instanceLogger;
    private final List<X509Certificate> certificates;
    private ExtractingEntity extractingEntity;

    CommunicationLogOuterHttpRequestInterceptor(CommunicationLog communicationLog, String frameworkIdentifier,
                                                List<X509Certificate> certificateList) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.commlog = communicationLog;
        this.certificates = certificateList;
    }

    @Override
    public void process(HttpRequest request, HttpContext context) {
        instanceLogger.debug("Processing request: {}", request.getRequestLine());

        HttpHost target = (HttpHost) context.getAttribute(
            HttpCoreContext.HTTP_TARGET_HOST);

        var currentTransactionId = TRANSACTION_ID_PREFIX_CLIENT + TRANSACTION_ID.incrementAndGet();
        context.setAttribute(CommunicationLog.MessageType.REQUEST.name(), currentTransactionId);
        var requestNetLevelHttpApplicationInfo = new HttpApplicationInfo(
            ApacheClientHelper.allHeadersToMultimap(request.getAllHeaders()),
            currentTransactionId,
            request.getRequestLine().getUri()
        );
        final Header[] appLevelHeaders =
            (Header[]) context.getAttribute(CommunicationLogInnerHttpRequestInterceptor.APP_LEVEL_HEADERS_ATTRIBUTE);
        var requestAppLevelHttpApplicationInfo = new HttpApplicationInfo(
            ApacheClientHelper.allHeadersToMultimap(appLevelHeaders),
            currentTransactionId,
            request.getRequestLine().getUri()
        );

        // collect information for TransportInfo
        var requestTransportInfo = new TransportInfo(
            target.getSchemeName(),
            null,
            null,
            target.getHostName(),
            target.getPort(),
            certificates
        );

        var requestNetLevelCommContext =
            new CommunicationContext(requestNetLevelHttpApplicationInfo, requestTransportInfo);
        var requestAppLevelCommContext =
            new CommunicationContext(requestAppLevelHttpApplicationInfo, requestTransportInfo);

        OutputStream appLevelCommlogStream = commlog.logMessage(
            CommunicationLog.Direction.OUTBOUND,
            CommunicationLog.TransportType.HTTP,
            CommunicationLog.MessageType.REQUEST,
            requestAppLevelCommContext,
            CommunicationLog.Level.APPLICATION);

        OutputStream netLevelCommlogStream = commlog.logRelatedMessage(
            appLevelCommlogStream,
            CommunicationLog.Direction.OUTBOUND,
            CommunicationLog.TransportType.HTTP,
            CommunicationLog.MessageType.REQUEST,
            requestNetLevelCommContext,
            CommunicationLog.Level.NETWORK);

        if (request instanceof HttpEntityEnclosingRequest) {
            var entityRequest = (HttpEntityEnclosingRequest) request;
            HttpEntity oldMessageEntity = entityRequest.getEntity();
            if (isGzipped(request)) {
                entityRequest.setEntity(new CommunicationLogEntity(oldMessageEntity, netLevelCommlogStream));
                if (this.extractingEntity != null) {
                    this.extractingEntity.setExtractInto(appLevelCommlogStream);
                }
            } else {
                entityRequest
                    .setEntity(
                        new CommunicationLogEntity(
                            oldMessageEntity,
                            appLevelCommlogStream,
                            netLevelCommlogStream));
            }
        } else {
            // GET doesn't have any entity, but still has headers to save
            instanceLogger.debug("Request doesn't have a body {}, closing stream", request.getRequestLine());
            try {
                appLevelCommlogStream.close();
                if (netLevelCommlogStream != null) {
                    netLevelCommlogStream.close();
                }
            } catch (IOException e) {
                // not totally harmful, nothing was inside the body
                LOG.warn("Could not close empty output stream. {}", e.getMessage());
                LOG.trace("Could not close empty output stream.", e);
            }
            return;
        }

        instanceLogger.debug("Processing request done: {}", request.getRequestLine());
    }

    /**
     * Determines whether or not a request is Gzipped by inspecting its headers.
     * @param request the request in question.
     * @return true, when the given Request contains the Header "Content-Encoding: gzip"
     */
    private boolean isGzipped(HttpRequest request) {
        final Header[] contentEncodingHeaders = request.getHeaders("Content-Encoding");
        for (Header contentEncodingHeader : contentEncodingHeaders) {
            if (contentEncodingHeader.getValue().contains("gzip")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Set the ExtractingEntity.
     * @param entity ExtractingEntity to pass the Application-Level CommlogStream into.
     */
    public void setExtractingEntity(ExtractingEntity entity) {
        this.extractingEntity = entity;
    }
}
