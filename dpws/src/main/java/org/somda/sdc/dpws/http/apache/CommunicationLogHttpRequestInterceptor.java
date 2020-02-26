package org.somda.sdc.dpws.http.apache;

import com.google.inject.Inject;
import org.apache.http.*;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.HttpApplicationInfo;
import org.somda.sdc.dpws.soap.TransportInfo;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;

public class CommunicationLogHttpRequestInterceptor implements HttpRequestInterceptor {
    private static final Logger LOG = LoggerFactory.getLogger(CommunicationLogHttpRequestInterceptor.class);

    private final CommunicationLog commlog;

    @Inject
    CommunicationLogHttpRequestInterceptor(CommunicationLog communicationLog) {
        this.commlog = communicationLog;
    }

    @Override
    public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
        LOG.warn("Processing request");

        HttpHost target = (HttpHost) context.getAttribute(
                HttpCoreContext.HTTP_TARGET_HOST);

        if (!(request instanceof HttpEntityEnclosingRequest)) {
            LOG.warn("Interceptor cannot retrieve request entity!");
            return;
        }

        var entityRequest = (HttpEntityEnclosingRequest) request;
        HttpEntity oldMessageEntity = entityRequest.getEntity();

        var requestHttpApplicationInfo = new HttpApplicationInfo(
                ClientTransportBinding.allHeadersToMap(request.getAllHeaders())
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

        OutputStream commlogStream = commlog.logMessage(CommunicationLog.Direction.OUTBOUND, CommunicationLog.TransportType.HTTP,
                requestCommContext);

        entityRequest.setEntity(new CommunicationLogEntity(oldMessageEntity, commlogStream));


        LOG.warn("Processing request done");
    }
};
