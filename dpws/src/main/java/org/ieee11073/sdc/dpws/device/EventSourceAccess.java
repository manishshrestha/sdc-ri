package org.ieee11073.sdc.dpws.device;

import org.ieee11073.sdc.dpws.soap.exception.MarshallingException;
import org.ieee11073.sdc.dpws.soap.exception.TransportException;
import org.ieee11073.sdc.dpws.soap.wseventing.model.WsEventingStatus;

/**
 * Interface to access event source functionality provided by a {@link Device}.
 */
public interface EventSourceAccess {
    /**
     * Send a notification to all subscribers.
     *
     * @param action The action the notification complies with.
     * @param payload The message payload that is tried to be marshalled.
     * @throws TransportException   Any transport-related exception during processing. This will hinder the response from
     *                              being sent.
     * @throws MarshallingException Any exception that occurs during marshalling or unmarshalling of SOAP messages.
     */
    void sendNotification(String action, Object payload) throws MarshallingException, TransportException;

    /**
     * Send a subscription end message to all subscribers and shut down connected subscriptions.
     * @param status reason for ending the subscription
     * @throws TransportException   Any transport-related exception during processing. This will hinder the response from
     *                              being sent.
     */
    void subscriptionEndToAll(WsEventingStatus status) throws TransportException;
}
