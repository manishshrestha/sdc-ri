package org.somda.sdc.dpws.http.apache;

import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.CommunicationLog;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

// TODO: this Interceptor seems to do nothing of value. Remove it.
/**
 * Request interceptor which captures the request message before it is compressed and passes this information on
 * to the {@linkplain CommunicationLogOuterHttpRequestInterceptor}, which writes the outgoing request message and headers into
 * the {@linkplain CommunicationLog}.
 */
public class CommunicationLogInnerHttpRequestInterceptor implements HttpRequestInterceptor {
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

        instanceLogger.debug("Processing request done: {}", request.getRequestLine());
    }
}
