package org.ieee11073.sdc.dpws.soap.factory;

import com.google.inject.assistedinject.Assisted;
import org.ieee11073.sdc.dpws.soap.NotificationSource;
import org.ieee11073.sdc.dpws.soap.interception.NotificationCallback;

/**
 * Factory to create notification clients (source side).
 */
public interface NotificationSourceFactory {
    /**
     * @param callback Callback that shall be invoked after all interceptors on are visited and the notification shall
     *                 be transmitted to AbstractEventMessage sink.
     */
    NotificationSource createNotificationSource(@Assisted NotificationCallback callback);
}
