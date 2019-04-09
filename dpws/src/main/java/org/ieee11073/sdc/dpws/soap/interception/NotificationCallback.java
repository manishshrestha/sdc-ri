package org.ieee11073.sdc.dpws.soap.interception;

import org.ieee11073.sdc.dpws.soap.SoapMessage;
import org.ieee11073.sdc.dpws.soap.exception.MarshallingException;
import org.ieee11073.sdc.dpws.soap.exception.TransportException;

/**
 * Callback for network bindings to invoke notification push.
 */
public interface NotificationCallback {
    /**
     * Client is requested to invoke a notification push on the network.
     *
     * @param notification The notification to push.
     */
    void onNotification(SoapMessage notification) throws TransportException, MarshallingException;
}
