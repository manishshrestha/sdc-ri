package org.ieee11073.sdc.dpws.soap.wseventing;

import com.google.common.util.concurrent.Service;
import org.ieee11073.sdc.dpws.soap.wseventing.model.Notification;

import java.util.concurrent.BlockingQueue;

/**
 * Subscription manager interface that is used by event sources.
 */
public interface SourceSubscriptionManager extends SubscriptionManager, Service {
    /**
     * Gets the notification queue where notifications are temporarily stored until ready to be send over the network.
     *
     * @return a blocking queue with notifications.
     */
    BlockingQueue<Notification> getNotificationQueue();
}
