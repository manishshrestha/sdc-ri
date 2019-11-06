package org.somda.sdc.dpws.soap.interception;

import org.somda.sdc.dpws.soap.SoapMessage;

/**
 * Object passed to interceptor to provide a SOAP notification message.
 */
public class NotificationObject implements InterceptorCallbackType {
    private final SoapMessage notification;

    public NotificationObject(SoapMessage notification) {
        this.notification = notification;
    }

    public SoapMessage getNotification() {
        return notification;
    }

}
