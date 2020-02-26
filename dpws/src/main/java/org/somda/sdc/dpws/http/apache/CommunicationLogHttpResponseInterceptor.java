package org.somda.sdc.dpws.http.apache;

import com.google.inject.Inject;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.HttpApplicationInfo;
import org.somda.sdc.dpws.soap.TransportInfo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;

public class CommunicationLogHttpResponseInterceptor implements HttpResponseInterceptor {
    private static final Logger LOG = LoggerFactory.getLogger(CommunicationLogHttpResponseInterceptor.class);

    private final CommunicationLog commlog;

    @Inject
    CommunicationLogHttpResponseInterceptor(CommunicationLog communicationLog) {
        this.commlog = communicationLog;
    }

    @Override
    public void process(HttpResponse response, HttpContext context) throws HttpException, IOException {
        LOG.warn("Processing response");

        HttpHost target = (HttpHost) context.getAttribute(
                HttpCoreContext.HTTP_TARGET_HOST);

        HttpEntity oldMessageEntity = response.getEntity();

        var requestHttpApplicationInfo = new HttpApplicationInfo(
                ClientTransportBinding.allHeadersToMap(response.getAllHeaders())
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

        OutputStream commlogStream = commlog.logMessage(CommunicationLog.Direction.INBOUND, CommunicationLog.TransportType.HTTP,
                requestCommContext, OutputStream.nullOutputStream());

        response.setEntity(new CommunicationLogEntity(oldMessageEntity, commlogStream));

        LOG.warn("Processing response done");
    }
}
