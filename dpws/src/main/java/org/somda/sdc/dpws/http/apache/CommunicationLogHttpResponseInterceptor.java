package org.somda.sdc.dpws.http.apache;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.http.apache.helper.ApacheClientHelper;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.HttpApplicationInfo;
import org.somda.sdc.dpws.soap.TransportInfo;

import java.io.OutputStream;
import java.util.Collections;

/**
 * Response interceptor which writes the incoming response message and headers into the {@linkplain CommunicationLog}.
 */
public class CommunicationLogHttpResponseInterceptor implements HttpResponseInterceptor {
    private static final Logger LOG = LogManager.getLogger(CommunicationLogHttpResponseInterceptor.class);

    private final CommunicationLog commlog;
    private final Logger instanceLogger;

    CommunicationLogHttpResponseInterceptor(CommunicationLog communicationLog, String frameworkIdentifier) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.commlog = communicationLog;
    }

    @Override
    public void process(HttpResponse response, HttpContext context) {
        instanceLogger.debug("Processing response");

        HttpHost target = (HttpHost) context.getAttribute(
                HttpCoreContext.HTTP_TARGET_HOST);

        HttpEntity oldMessageEntity = response.getEntity();

        var requestHttpApplicationInfo = new HttpApplicationInfo(
                ApacheClientHelper.allHeadersToMultimap(response.getAllHeaders())
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
                CommunicationLog.Direction.INBOUND,
                CommunicationLog.TransportType.HTTP,
                CommunicationLog.MessageType.RESPONSE,
                requestCommContext);

        response.setEntity(new CommunicationLogEntity(oldMessageEntity, commlogStream));

        instanceLogger.debug("Processing response done");
    }
}
