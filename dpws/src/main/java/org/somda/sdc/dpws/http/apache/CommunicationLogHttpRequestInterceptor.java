package org.somda.sdc.dpws.http.apache;

import com.google.inject.Inject;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.HttpApplicationInfo;
import org.somda.sdc.dpws.soap.TransportInfo;

import java.io.ByteArrayOutputStream;
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
        var oldMessageEntity = entityRequest.getEntity();
        var messageData = oldMessageEntity.getContent();
        var os = new ByteArrayOutputStream();
        messageData.transferTo(os);

        // reset attached payload
        var requestEntity = new ByteArrayEntity(os.toByteArray());
        entityRequest.setEntity(requestEntity);

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

        try(OutputStream commlogStream = commlog.logMessage(CommunicationLog.Direction.OUTBOUND, CommunicationLog.TransportType.HTTP,
                requestCommContext, OutputStream.nullOutputStream())) {
            os.writeTo(commlogStream);
        }
        LOG.warn("Processing request done");
    }
};
