package org.ieee11073.sdc.dpws.soap.wseventing.helper;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import org.ieee11073.sdc.dpws.soap.interception.InterceptorResult;
import org.ieee11073.sdc.dpws.soap.wseventing.SourceSubscriptionManager;
import org.ieee11073.sdc.dpws.soap.wseventing.WsEventingConfig;
import org.ieee11073.sdc.dpws.soap.wseventing.event.SubscriptionAddedMessage;
import org.ieee11073.sdc.dpws.soap.wseventing.event.SubscriptionRemovedMessage;
import org.ieee11073.sdc.dpws.soap.wseventing.model.Notification;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Worker to process notification network jobs by round-robin through all subscription managers.
 * <p>
 * Subscribe this notification worker to the {@link SubscriptionRegistry} in order to track subscription managers.
 */
public class NotificationWorker implements Runnable {
    private final EventSourceTransportManager evtSrcTransportManager;
    private final Duration notificationStaleDuration;
    private final Map<String, WorkerItem> workerItemMap;
    private final Condition condition;
    private final Lock lock;
    private boolean wakeUp;

    @AssistedInject
    NotificationWorker(@Assisted EventSourceTransportManager evtSrcTransportManager,
                       @Named(WsEventingConfig.NOTIFICATION_STALE_DURATION) Duration notificationStaleDuration) {
        this.evtSrcTransportManager = evtSrcTransportManager;
        this.notificationStaleDuration = notificationStaleDuration;
        this.wakeUp = false;
        this.workerItemMap = new ConcurrentHashMap<>();
        this.lock = new ReentrantLock();
        this.condition = lock.newCondition();
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            lock.lock();
            try {
                // If not woken up before, go waiting to be signaled
                if (!wakeUp) {
                    condition.await();
                }
                wakeUp = false;
            } catch (InterruptedException e) {
                break;
            } finally {
                lock.unlock();
            }

            // Someone has woken up the worker, start dispatching queued notifications
            dispatchNotifications();
        }
    }

    /**
     * Wakes up the dispatcher.
     * <p>
     * If the worker has nothing to do, i.e., every notification has been processed, then it falls into a sleep mode.
     * To ensure that the worker will do its work, call this function after at least one of the subscription managers
     * has a new job to process.
     */
    public void wakeUp() {
        lock.lock();
        try {
            wakeUp = true;
            condition.signal();
        } finally {
            lock.unlock();
        }
    }

    private void dispatchNotifications() {
        boolean hasQueuedNotifications;

        // Iterate as long as there are notifications left
        do {
            hasQueuedNotifications = false; // No notifications found to process

            // Get all available worker items
            ArrayList<WorkerItem> workerItems = new ArrayList<>(workerItemMap.values());
            for (WorkerItem workerItem : workerItems) {

                // If there is an item being processed
                if (workerItem.getProcessedItem().isPresent()) {
                    // Check if it is already done
                    if (workerItem.getProcessedItem().get().isDone()) {
                        // If done, process the next element in the worker item's queue
                        processNotificationQueue(workerItem);
                    }
                } else {
                    // If there was never an item being processed, process now
                    processNotificationQueue(workerItem);
                }

                // If there is an item left after processing, set loop condition variable to continue worker after
                // all subscriptions have been processed
                if (!workerItem.getSubscriptionManager().getNotificationQueue().isEmpty()) {
                    hasQueuedNotifications = true;
                }
            }
        } while (hasQueuedNotifications);
    }

    @Subscribe
    void onAddSubscription(SubscriptionAddedMessage msg) {
        workerItemMap.put(msg.getPayload().getSubscriptionId(), new WorkerItem(msg.getPayload()));
    }

    @Subscribe
    void onRemoveSubscription(SubscriptionRemovedMessage msg) {
        workerItemMap.remove(msg.getPayload().getSubscriptionId());
    }

    private void processNotificationQueue(WorkerItem workerItem) {
        // Loop through the worker item's queue
        // - poll item from queue
        // - continue as long as there are elements in the queue and the current polled element is stale
        // - if current polled element is not stale, start processing it and return
        while (!workerItem.getSubscriptionManager().getNotificationQueue().isEmpty()) {
            Notification notification = workerItem.getSubscriptionManager().getNotificationQueue().poll();
            if (!isNotificationStale(notification) && notification.getPayload() != null) {
                workerItem.setProcessedItem(evtSrcTransportManager.sendNotifyTo(workerItem.getSubscriptionManager(),
                        notification.getPayload()));
                return;
            }
        }
    }

    private boolean isNotificationStale(@Nullable Notification notification) {
        // A notification is stale when not null and notificationStaleDuration is not exceeded
        return notification != null &&
                notification.getConstructionTime()
                        .plus(notificationStaleDuration)
                        .compareTo(LocalDateTime.now()) < 0;
    }

    // Encapsulation of a subscription manager and an item that shall be processed by the worker
    private class WorkerItem {
        private final SourceSubscriptionManager subscriptionManager;
        private ListenableFuture<InterceptorResult> processedItem;

        WorkerItem(SourceSubscriptionManager subscriptionManager) {
            this.subscriptionManager = subscriptionManager;
            this.processedItem = null;
        }

        SourceSubscriptionManager getSubscriptionManager() {
            return subscriptionManager;
        }

        synchronized Optional<ListenableFuture<InterceptorResult>> getProcessedItem() {
            return Optional.ofNullable(processedItem);
        }

        synchronized void setProcessedItem(ListenableFuture<InterceptorResult> processedNotification) {
            this.processedItem = processedNotification;
        }
    }
}