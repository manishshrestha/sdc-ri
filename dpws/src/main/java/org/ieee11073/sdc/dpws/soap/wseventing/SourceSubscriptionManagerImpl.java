package org.ieee11073.sdc.dpws.soap.wseventing;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.ieee11073.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.ieee11073.sdc.dpws.soap.wseventing.model.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Default implementation of {@link SourceSubscriptionManager}.
 */
public class SourceSubscriptionManagerImpl extends AbstractIdleService implements SourceSubscriptionManager {
    private static final Logger LOG = LoggerFactory.getLogger(SourceSubscriptionManagerImpl.class);

    private final EndpointReferenceType notifyTo;
    private final EndpointReferenceType endTo;
    private LocalDateTime expiresTimeout;
    private final String subscriptionId;
    private Duration expires;
    private final EndpointReferenceType subscriptionManagerEpr;
    private final Lock expiresLock;
    private final BlockingQueue<Notification> notificationQueue;

    @AssistedInject
    SourceSubscriptionManagerImpl(@Assisted("SubscriptionManager") EndpointReferenceType subscriptionManagerEpr,
                                  @Assisted Duration expires,
                                  @Assisted("NotifyTo") EndpointReferenceType notifyTo,
                                  @Assisted("EntTo") @Nullable EndpointReferenceType endTo) {
        this(subscriptionManagerEpr, expires, notifyTo, endTo, null);
    }

    @AssistedInject
    SourceSubscriptionManagerImpl(@Assisted("SubscriptionManager") EndpointReferenceType subscriptionManagerEpr,
                                  @Assisted Duration expires,
                                  @Assisted("NotifyTo") EndpointReferenceType notifyTo,
                                  @Assisted("EntTo") @Nullable EndpointReferenceType endTo,
                                  @Assisted("SubscriptionId") @Nullable String subscriptionId) {
        this.subscriptionId = Optional.ofNullable(subscriptionId).orElse(UUID.randomUUID().toString());
        this.expiresTimeout = calculateTimeout(expires);
        this.expires = expires;
        this.endTo = endTo;
        this.notifyTo = notifyTo;
        this.subscriptionManagerEpr = subscriptionManagerEpr;
        this.expiresLock = new ReentrantLock();
        this.notificationQueue = new ArrayBlockingQueue<>(500); // todo make queue size configurable
    }

    @Override
    public String getSubscriptionId() {
        return subscriptionId;
    }

    @Override
    public LocalDateTime getExpiresTimeout() {
        expiresLock.lock();
        try {
            return expiresTimeout;
        } finally {
            expiresLock.unlock();
        }
    }

    @Override
    public EndpointReferenceType getNotifyTo() {
        return notifyTo;
    }

    @Override
    public EndpointReferenceType getEndTo() {
        return endTo;
    }

    @Override
    public Duration getExpires() {
        expiresLock.lock();
        try {
            return expires;
        } finally {
            expiresLock.unlock();
        }
    }

    @Override
    public EndpointReferenceType getSubscriptionManagerEpr() {
        return subscriptionManagerEpr;
    }


    @Override
    public void renew(Duration expires) {
        expiresLock.lock();
        try {
            this.expires = expires;
            this.expiresTimeout = calculateTimeout(expires);
        } finally {
            expiresLock.unlock();
        }
    }

    @Override
    public BlockingQueue<Notification> getNotificationQueue() {
        return notificationQueue;
    }

    private LocalDateTime calculateTimeout(Duration expires) {
        LocalDateTime t = LocalDateTime.now();
        return t.plus(expires);
    }

    @Override
    protected void startUp() {
        // void
    }

    @Override
    protected void shutDown() {
        // void
    }
}
