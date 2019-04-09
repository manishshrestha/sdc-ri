package org.ieee11073.sdc.dpws.soap.wseventing.factory;

import org.ieee11073.sdc.dpws.soap.wseventing.helper.EventSourceTransportManager;
import org.ieee11073.sdc.dpws.soap.wseventing.helper.NotificationWorker;

/**
 * Create {@link NotificationWorker} instances.
 */
public interface NotificationWorkerFactory {
    /**
     * @param eventSourceTransportManager Used to deliver notifications.
     */
    NotificationWorker createNotificationWorker(EventSourceTransportManager eventSourceTransportManager);
}
