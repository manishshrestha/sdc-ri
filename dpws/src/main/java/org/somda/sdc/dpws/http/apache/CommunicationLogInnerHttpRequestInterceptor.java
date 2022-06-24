package org.somda.sdc.dpws.http.apache;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.CommunicationLog;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Request interceptor which captures the request message before it is compressed and passes this information on
 * to the {@linkplain CommunicationLogOuterHttpRequestInterceptor}, which writes the outgoing request message and
 * headers into the {@linkplain CommunicationLog}.
 */
public class CommunicationLogInnerHttpRequestInterceptor implements HttpRequestInterceptor {
    public static final String APP_LEVEL_HEADERS_ATTRIBUTE = "AppLevelHeadersAttribute";
    private static final Logger LOG = LogManager.getLogger(CommunicationLogInnerHttpRequestInterceptor.class);
    private static final String TRANSACTION_ID_PREFIX_CLIENT = "rrId:client:" + UUID.randomUUID() + ":";
    private static final AtomicLong TRANSACTION_ID = new AtomicLong(-1L);

    private final Logger instanceLogger;

    CommunicationLogInnerHttpRequestInterceptor(String frameworkIdentifier) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
    }

    @Override
    public void process(HttpRequest request, HttpContext context) {
        instanceLogger.debug("Processing request: {}", request.getRequestLine());

        var currentTransactionId = TRANSACTION_ID_PREFIX_CLIENT + TRANSACTION_ID.incrementAndGet();
        context.setAttribute(CommunicationLog.MessageType.REQUEST.name(), currentTransactionId);

        // capture the Application Level Headers and pass them to the CommunicationLogOuterHttpRequestInterceptor
        // for logging.
        final Header[] applicationLevelHeaders = request.getAllHeaders();
        context.setAttribute(APP_LEVEL_HEADERS_ATTRIBUTE, applicationLevelHeaders);

        instanceLogger.debug("Processing request done: {}", request.getRequestLine());
    }
}
