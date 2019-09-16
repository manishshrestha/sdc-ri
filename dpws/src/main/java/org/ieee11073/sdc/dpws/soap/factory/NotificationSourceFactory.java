package org.ieee11073.sdc.dpws.soap.factory;

import com.google.inject.assistedinject.Assisted;
import org.ieee11073.sdc.dpws.soap.NotificationSource;
import org.ieee11073.sdc.dpws.soap.interception.NotificationCallback;

/**
 * Factory to create notification clients (source side).
 */
public interface NotificationSourceFactory {
    /**
     * Creates a notification source.
     *
     * @param callback a callback that is invoked after all interceptors are visited and the notification is about to
     *                 be transmitted to an event sink.
     * @return a new {@link NotificationSource}.
     */
    NotificationSource createNotificationSource(@Assisted NotificationCallback callback);
}
