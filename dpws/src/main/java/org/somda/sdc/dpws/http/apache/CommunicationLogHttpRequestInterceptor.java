package org.somda.sdc.dpws.http.apache;

import com.google.inject.name.Named;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.DpwsConfig;
import org.somda.sdc.dpws.http.apache.helper.ApacheClientHelper;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.HttpApplicationInfo;
import org.somda.sdc.dpws.soap.TransportInfo;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;

/**
 * Request interceptor which writes the outgoing request message and headers into the {@linkplain CommunicationLog}.
 */
public class CommunicationLogHttpRequestInterceptor implements HttpRequestInterceptor {
    private static final Logger LOG = LogManager.getLogger(CommunicationLogHttpRequestInterceptor.class);

    private final CommunicationLog commlog;
    private final Logger instanceLogger;

    CommunicationLogHttpRequestInterceptor(CommunicationLog communicationLog, String frameworkIdentifier) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.commlog = communicationLog;
    }

    @Override
    public void process(HttpRequest request, HttpContext context) {
        instanceLogger.debug("Processing request: {}", request.getRequestLine());

        HttpHost target = (HttpHost) context.getAttribute(
                HttpCoreContext.HTTP_TARGET_HOST);

        var requestHttpApplicationInfo = new HttpApplicationInfo(
                ApacheClientHelper.allHeadersToMultimap(request.getAllHeaders())
        );

        // collect information for TransportInfo
        var requestTransportInfo = new TransportInfo(
                target.getSchemeName(),
                null,
                null,
                target.getHostName(),
                target.getPort(),
                Collections.emptyList()
        );

        var requestCommContext = new CommunicationContext(requestHttpApplicationInfo, requestTransportInfo);

        OutputStream commlogStream = commlog.logMessage(
                CommunicationLog.Direction.OUTBOUND,
                CommunicationLog.TransportType.HTTP,
                requestCommContext);

        if (!(request instanceof HttpEntityEnclosingRequest)) {
            // GET doesn't have any entity, but still has headers to save
            instanceLogger.debug("Request doesn't have a body {}, closing stream", request.getRequestLine());
            try {
                commlogStream.close();
            } catch (IOException e) {
                // not totally harmful, nothing was inside the body
                LOG.error("Could not close empty output stream. {}", e.getMessage());
                LOG.trace("Could not close empty output stream.", e);
            }
            return;
        }

        var entityRequest = (HttpEntityEnclosingRequest) request;
        HttpEntity oldMessageEntity = entityRequest.getEntity();

        entityRequest.setEntity(new CommunicationLogEntity(oldMessageEntity, commlogStream));

        instanceLogger.debug("Processing request done: {}", request.getRequestLine());
    }
}
