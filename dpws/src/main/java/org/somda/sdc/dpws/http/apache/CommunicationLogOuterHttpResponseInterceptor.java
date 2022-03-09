package org.somda.sdc.dpws.http.apache;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.CommunicationLog;
import java.util.Optional;

/**
 * Response interceptor which writes the incoming response message and headers into the {@linkplain CommunicationLog}.
 * Outer Part - called before the Response has been decompressed.
 */
public class CommunicationLogOuterHttpResponseInterceptor implements HttpResponseInterceptor {
    public static final String CONTENT_ENCODING_HEADER_FROM_OUTER_PART_KEY = "Content-Encoding-Header-From-Outer-Part";
    private static final Logger LOG = LogManager.getLogger(CommunicationLogOuterHttpResponseInterceptor.class);

    private final CommunicationLog commlog;
    private final Logger instanceLogger;

    CommunicationLogOuterHttpResponseInterceptor(CommunicationLog communicationLog, String frameworkIdentifier) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.commlog = communicationLog;
    }

    @Override
    public void process(HttpResponse response, HttpContext context) {
        instanceLogger.debug("Processing response");

        HttpHost target = (HttpHost) context.getAttribute(
                HttpCoreContext.HTTP_TARGET_HOST);

        HttpEntity oldMessageEntity = response.getEntity();

        var currentTransactionOpt = Optional.of(context.getAttribute(CommunicationLog.MessageType.REQUEST.name()));
        var currentTransactionId = (String) currentTransactionOpt.orElse("");

        final Header contentEncodingHeader = response.getLastHeader("Content-Encoding");
        context.setAttribute(CONTENT_ENCODING_HEADER_FROM_OUTER_PART_KEY, contentEncodingHeader);
    }
}
