package org.ieee11073.sdc.dpws.soap.wseventing;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.ieee11073.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Default implementation of {@link SinkSubscriptionManager}.
 */
public class SinkSubscriptionManagerImpl implements SinkSubscriptionManager {

    private final SubscriptionManager subscriptionManager;

    private final AtomicBoolean autoRenew;

    @AssistedInject
    SinkSubscriptionManagerImpl(@Assisted SubscriptionManager subscriptionManager) {
        this.subscriptionManager = subscriptionManager;
        this.autoRenew = new AtomicBoolean(false);
    }

    @Override
    public String getSubscriptionId() {
        return subscriptionManager.getSubscriptionId();
    }

    @Override
    public LocalDateTime getExpiresTimeout() {
        return subscriptionManager.getExpiresTimeout();
    }

    @Override
    public EndpointReferenceType getNotifyTo() {
        return subscriptionManager.getNotifyTo();
    }

    @Override
    public EndpointReferenceType getEndTo() {
        return subscriptionManager.getEndTo();
    }

    @Override
    public Duration getExpires() {
        return subscriptionManager.getExpires();
    }

    @Override
    public EndpointReferenceType getSubscriptionManagerEpr() {
        return subscriptionManager.getSubscriptionManagerEpr();
    }

    @Override
    public void renew(Duration expires) {
        subscriptionManager.renew(expires);
    }

    @Override
    public boolean isAutoRenewEnabled() {
        return autoRenew.get();
    }

    @Override
    public void setAutoRenewEnabled(boolean enabled) {
        autoRenew.set(enabled);
    }
}
