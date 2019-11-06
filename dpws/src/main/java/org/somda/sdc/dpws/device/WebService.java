package org.somda.sdc.dpws.device;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.Service;
import org.somda.sdc.dpws.service.HostedService;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.dpws.soap.interception.Interceptor;
import org.somda.sdc.dpws.soap.wseventing.EventSource;
import org.somda.sdc.dpws.soap.wseventing.model.WsEventingStatus;
import org.somda.sdc.dpws.soap.RequestResponseServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Web Service base class.
 * <p>
 * The Web Service is a server interceptor to process incoming requests of a certain Web Service.
 * Moreover, the Web Service base class is capable of providing an event source to send notifications if needed.
 * <p>
 * The event source is only set if a hosted service has been registered at the Web Service. The hosted service can be
 * registered by first getting the hosting service access followed by adding a hosted service:
 * <ol>
 * <li>{@link Device#getHostingServiceAccess()} to get hosting service access, and then
 * <li>{@link HostingServiceAccess#addHostedService(HostedService)} to add the service to a hosting service.
 * </ol>
 * Use this class as a server interceptor when calling {@link HostingServiceAccess#addHostedService(HostedService)}.
 */
public abstract class WebService extends AbstractIdleService implements EventSourceAccess, Interceptor {
    private static final Logger LOG = LoggerFactory.getLogger(WebService.class);
    private EventSource eventSource;

    /**
     * Default constructor that initializes a non-functioning event source stub.
     */
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
            public void awaitRunning(long l, TimeUnit timeUnit) {
            }

            @Override
            public void awaitTerminated() {
            }

            @Override
            public void awaitTerminated(long l, TimeUnit timeUnit) {
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
    public void sendNotification(String action, Object payload) {
        eventSource.sendNotification(action, payload);
    }

    @Override
    public void subscriptionEndToAll(WsEventingStatus status) {
        eventSource.subscriptionEndToAll(status);
    }

    /**
     * Allows to set the event source from outside.
     * <p>
     * Method is not thread-safe and should only be invoked by the {@link Device} implementation.
     * todo DGr find better design to avoid overwriting during runtime.
     *
     * @param eventSource the event source to inject.
     */
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
