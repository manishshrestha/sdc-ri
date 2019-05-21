package org.ieee11073.sdc.dpws.soap.wseventing;

import com.google.common.util.concurrent.Service;
import org.ieee11073.sdc.dpws.soap.wseventing.model.Notification;

import java.util.concurrent.BlockingQueue;

/**
 * Subscription manager to indicate usage on event source side.
 */
public interface SourceSubscriptionManager extends SubscriptionManager, Service {
    /**
     * Get notfication queue where notifications are temporarily saved until ready to send over network.
     */
    BlockingQueue<Notification> getNotificationQueue();
}
