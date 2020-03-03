package org.somda.sdc.dpws.soap.wseventing;

import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

/**
 * General WS-Eventing Subscription Manager information.
 */
public interface SubscriptionManager {
    String getSubscriptionId();

    LocalDateTime getExpiresTimeout();

    EndpointReferenceType getNotifyTo();

    Optional<EndpointReferenceType> getEndTo();

    Duration getExpires();

    EndpointReferenceType getSubscriptionManagerEpr();

    Collection<String> getActions();
}
