package org.ieee11073.sdc.dpws.soap.wseventing;

import org.ieee11073.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * General WS-Eventing Subscription Manager information.
 */
public interface SubscriptionManager {
    String getSubscriptionId();

    LocalDateTime getExpiresTimeout();

    EndpointReferenceType getNotifyTo();

    EndpointReferenceType getEndTo();

    Duration getExpires();

    EndpointReferenceType getSubscriptionManagerEpr();

    void renew(Duration expires);
}
