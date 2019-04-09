package org.ieee11073.sdc.dpws.soap;

import org.ieee11073.sdc.dpws.soap.interception.InterceptorHandler;

/**
 * Interface for network bindings to invoke execution of incoming SOAP notification messages.
 */
public interface NotificationSink extends InterceptorHandler {
    /**
     * Start processing of an incoming SOAP notification message.
     *
     * @param notification Incoming request message.
     */
    void receiveNotification(SoapMessage notification);
}
