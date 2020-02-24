package org.somda.sdc.dpws.soap.interception;

import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.TransportInfo;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Object passed to interceptor to provide a SOAP notification message.
 */
public class NotificationObject implements InterceptorCallbackType {
    private final SoapMessage notification;
    private final TransportInfo transportInfo;

    public NotificationObject(SoapMessage notification) {
        this.notification = notification;
        this.transportInfo = null;
    }

    public NotificationObject(SoapMessage notification, @Nullable TransportInfo transportInfo) {
        this.notification = notification;
        this.transportInfo = transportInfo;
    }

    public SoapMessage getNotification() {
        return notification;
    }

    /**
     * Returns the transport information attached to this notification.
     *
     * @return the {@link TransportInfo} object or {@linkplain Optional#empty()} if no transport information is available.
     * The latter case typically happens for notifications on the way from the client to the network as at this point
     * no connection information is available.
     * Once the notification was received by a server, transport information can be attached to this object and
     * non-existence is then a potential error.
     */
    public Optional<TransportInfo> getTransportInfo() {
        return Optional.ofNullable(transportInfo);
    }
}
