package org.ieee11073.sdc.dpws.soap.wseventing.factory;

import org.ieee11073.sdc.dpws.soap.wseventing.helper.EventSourceTransportManager;
import org.ieee11073.sdc.dpws.soap.wseventing.helper.NotificationWorker;

/**
 * Creates {@link NotificationWorker} instances.
 */
public interface NotificationWorkerFactory {
    /**
     * Creates a new notification worker.
     *
     * @param eventSourceTransportManager the transport manager used to deliver notifications.
     * @return a new {@link NotificationWorker} instance.
     */
    NotificationWorker createNotificationWorker(EventSourceTransportManager eventSourceTransportManager);
}
