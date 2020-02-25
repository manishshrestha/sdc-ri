package org.somda.sdc.dpws.soap.wseventing.helper;

import org.somda.sdc.common.util.AutoLock;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.dpws.soap.wseventing.SubscriptionManager;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Helper class that collects data shared between source and sink subscription managers.
 */
public class SubscriptionManagerBase implements SubscriptionManager {

    private final EndpointReferenceType notifyTo;
    private final EndpointReferenceType endTo;
    private LocalDateTime expiresTimeout;
    private final String subscriptionId;
    private Duration expires;
    private final EndpointReferenceType subscriptionManagerEpr;
    private final Lock expiresLock;

    public SubscriptionManagerBase(EndpointReferenceType notifyTo,
                                   @Nullable EndpointReferenceType endTo,
                                   String subscriptionId,
                                   Duration expires,
                                   EndpointReferenceType subscriptionManagerEpr) {
        this.notifyTo = notifyTo;
        this.endTo = endTo;
        this.expiresTimeout = calculateTimeout(expires);
        this.subscriptionId = subscriptionId;
        this.expires = expires;
        this.subscriptionManagerEpr = subscriptionManagerEpr;
        this.expiresLock = new ReentrantLock();
    }

    @Override
    public String getSubscriptionId() {
        return subscriptionId;
    }

    @Override
    public LocalDateTime getExpiresTimeout() {
        try (AutoLock ignored = AutoLock.lock(expiresLock)){
            return expiresTimeout;
        }
    }

    @Override
    public EndpointReferenceType getNotifyTo() {
        return notifyTo;
    }

    @Override
    public Optional<EndpointReferenceType> getEndTo() {
        return Optional.ofNullable(endTo);
    }

    @Override
    public Duration getExpires() {
        try (AutoLock ignored = AutoLock.lock(expiresLock)){
            return expires;
        }
    }

    @Override
    public EndpointReferenceType getSubscriptionManagerEpr() {
        return subscriptionManagerEpr;
    }

    public void renew(Duration expires) {
        try (AutoLock ignored = AutoLock.lock(expiresLock)){
            this.expires = expires;
            this.expiresTimeout = calculateTimeout(expires);
        }
    }

    private LocalDateTime calculateTimeout(Duration expires) {
        LocalDateTime t = LocalDateTime.now();
        return t.plus(expires);
    }
}
