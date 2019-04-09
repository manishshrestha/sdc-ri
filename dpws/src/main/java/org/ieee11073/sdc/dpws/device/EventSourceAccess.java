package org.ieee11073.sdc.dpws.device;

import org.ieee11073.sdc.dpws.soap.exception.MarshallingException;
import org.ieee11073.sdc.dpws.soap.exception.TransportException;
import org.ieee11073.sdc.dpws.soap.wseventing.model.WsEventingStatus;

/**
 * Interface to access event source functionality provided by a {@link Device}.
 */
public interface EventSourceAccess {
    void sendNotification(String action, Object payload) throws MarshallingException, TransportException;

    void subscriptionEndToAll(WsEventingStatus status) throws TransportException;
}
