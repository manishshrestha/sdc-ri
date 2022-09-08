package org.somda.sdc.dpws.soap;

import org.somda.sdc.dpws.soap.interception.InterceptorHandler;

/**
 * Interface for network bindings to invoke execution of incoming SOAP notification messages.
 */
public interface NotificationSink extends InterceptorHandler {
    /**
     * Starts processing of an incoming SOAP notification message.
     *
     * @param notification         incoming request message.
     * @param communicationContext transport and application layer information (addresses, security, etc).
     */
    void receiveNotification(SoapMessage notification, CommunicationContext communicationContext);
}
