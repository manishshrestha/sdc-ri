package org.ieee11073.sdc.dpws.device;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.Service;
import org.ieee11073.sdc.dpws.service.HostedService;
import org.ieee11073.sdc.dpws.soap.exception.MarshallingException;
import org.ieee11073.sdc.dpws.soap.exception.TransportException;
import org.ieee11073.sdc.dpws.soap.interception.Interceptor;
import org.ieee11073.sdc.dpws.soap.wseventing.EventSource;
import org.ieee11073.sdc.dpws.soap.wseventing.model.WsEventingStatus;
import org.ieee11073.sdc.dpws.soap.RequestResponseServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Web Service base class.
 *
 * Provide event source if needed. To retrieve the event source, the instance has to be populated in advance by using
 * {@link Device#getHostingServiceAccess()} to get hosting service access, and then
 * {@link HostingServiceAccess#addHostedService(HostedService)} to add the service to a hosting service.
 *
 * Use this class as a server interceptor that will be registered at a suitable
 * {@link RequestResponseServer} instance within a {@link Device} instance when using
 * {@link HostingServiceAccess#addHostedService(HostedService)}.
 */
public abstract class WebService extends AbstractIdleService implements EventSourceAccess, Interceptor {
    private static final Logger LOG = LoggerFactory.getLogger(WebService.class);
    private EventSource eventSource;

    protected WebService() {
        eventSource = new EventSource() {
            @Override
            public Service startAsync() {
                return this;
            }

            @Override
            public boolean isRunning() {
                return false;
            }

            @Override
            public State state() {
                return null;
            }

            @Override
            public Service stopAsync() {
                return this;
            }

            @Override
            public void awaitRunning() {
            }

            @Override
            public void awaitRunning(long l, TimeUnit timeUnit) throws TimeoutException {
            }

            @Override
            public void awaitTerminated() {
            }

            @Override
            public void awaitTerminated(long l, TimeUnit timeUnit) throws TimeoutException {
            }

            @Override
            public Throwable failureCause() {
                return null;
            }

            @Override
            public void addListener(Listener listener, Executor executor) {
            }

            @Override
            public void sendNotification(String action, Object payload) {
                LOG.warn("No handler for notifications set yet. WebService has to be populated.");
            }

            @Override
            public void subscriptionEndToAll(WsEventingStatus status) {
                LOG.warn("No handler for notifications set yet. WebService has to be populated.");
            }
        };
    }

    @Override
    public void sendNotification(String action, Object payload) throws MarshallingException, TransportException {
        eventSource.sendNotification(action, payload);
    }

    @Override
    public void subscriptionEndToAll(WsEventingStatus status) throws TransportException {
        eventSource.subscriptionEndToAll(status);
    }

    void setEventSource(EventSource eventSource) {
        this.eventSource = eventSource;
    }

    protected void startUp() {
        eventSource.startAsync().awaitRunning();
    }

    protected void shutDown()  {
        eventSource.stopAsync().awaitTerminated();
    }
}
