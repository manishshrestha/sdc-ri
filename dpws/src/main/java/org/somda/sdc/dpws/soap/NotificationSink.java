package org.ieee11073.sdc.dpws.soap;

import org.ieee11073.sdc.dpws.soap.interception.InterceptorHandler;

/**
 * Interface for network bindings to invoke execution of incoming SOAP notification messages.
 */
public interface NotificationSink extends InterceptorHandler {
    /**
     * Starts processing of an incoming SOAP notification message.
     *
     * @param notification incoming request message.
     */
    void receiveNotification(SoapMessage notification);
}
