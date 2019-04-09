package org.ieee11073.sdc.dpws.soap.wseventing;

import com.google.common.util.concurrent.Service;
import org.ieee11073.sdc.dpws.soap.interception.Interceptor;
import org.ieee11073.sdc.dpws.soap.wseventing.model.WsEventingStatus;

/**
 * Interface to provide WS-Eventing event source functions.
 */
public interface EventSource extends Interceptor, Service {
    /**
     * Send a notification to all event sinks.
     *
     * @param operation The notification operation.
     * @param messageAction The action URI that identifies the payload.
     * @param payload A JAXB element or JAXB generated class object to transport.
     */
    void sendNotification(String operation, String messageAction, Object payload);

    /**
     * Send a subscription end to all event sinks.
     *
     * @param status The subscription end reason.
     */
    void subscriptionEndToAll(WsEventingStatus status);
}
