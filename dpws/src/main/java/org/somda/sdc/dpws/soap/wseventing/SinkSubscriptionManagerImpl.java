package org.somda.sdc.dpws.soap.wseventing;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.dpws.soap.wseventing.helper.SubscriptionManagerBase;
import org.somda.sdc.dpws.soap.wseventing.model.FilterType;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Default implementation of {@link SinkSubscriptionManager}.
 */
public class SinkSubscriptionManagerImpl implements SinkSubscriptionManager {
    private final SubscriptionManagerBase delegate;

    @AssistedInject
    SinkSubscriptionManagerImpl(@Assisted("SubscriptionManager") EndpointReferenceType subscriptionManagerEpr,
                                @Assisted Duration expires,
                                @Assisted("NotifyTo") EndpointReferenceType notifyTo,
                                @Assisted("EndTo") EndpointReferenceType endTo,
                                @Assisted("Filter") FilterType filter) {
        final var subscriptionId = UUID.randomUUID().toString();
        this.delegate = new SubscriptionManagerBase(
                notifyTo, endTo, subscriptionId, expires, subscriptionManagerEpr, filter);
    }

    @Override
    public String getSubscriptionId() {
        return delegate.getSubscriptionId();
    }

    @Override
    public LocalDateTime getExpiresTimeout() {
        return delegate.getExpiresTimeout();
    }

    @Override
    public EndpointReferenceType getNotifyTo() {
        return delegate.getNotifyTo();
    }

    @Override
    public Optional<EndpointReferenceType> getEndTo() {
        return delegate.getEndTo();
    }

    @Override
    public Duration getExpires() {
        return delegate.getExpires();
    }

    @Override
    public EndpointReferenceType getSubscriptionManagerEpr() {
        return delegate.getSubscriptionManagerEpr();
    }

    @Override
    public Collection<String> getActions() {
        return delegate.getActions();
    }

    @Override
    public void renew(Duration expires) {
        delegate.renew(expires);
    }
}
