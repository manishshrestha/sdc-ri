package org.somda.sdc.dpws.http.apache;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.http.apache.helper.ApacheClientHelper;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.HttpApplicationInfo;
import org.somda.sdc.dpws.soap.TransportInfo;

import java.io.OutputStream;
import java.util.Collections;

/**
 * Request interceptor which writes the outgoing request message and headers into the {@linkplain CommunicationLog}.
 */
public class CommunicationLogHttpRequestInterceptor implements HttpRequestInterceptor {
    private static final Logger LOG = LogManager.getLogger(CommunicationLogHttpRequestInterceptor.class);

    private final CommunicationLog commlog;

    CommunicationLogHttpRequestInterceptor(CommunicationLog communicationLog) {
        this.commlog = communicationLog;
    }

    @Override
    public void process(HttpRequest request, HttpContext context) {
        LOG.debug("Processing request: {}", request.getRequestLine());

        HttpHost target = (HttpHost) context.getAttribute(
                HttpCoreContext.HTTP_TARGET_HOST);

        if (!(request instanceof HttpEntityEnclosingRequest)) {
            LOG.warn("Interceptor cannot retrieve request entity for request {}", request.getRequestLine());
            return;
        }

        var entityRequest = (HttpEntityEnclosingRequest) request;
        HttpEntity oldMessageEntity = entityRequest.getEntity();

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

        entityRequest.setEntity(new CommunicationLogEntity(oldMessageEntity, commlogStream));

        LOG.debug("Processing request done: {}", request.getRequestLine());
    }
}
