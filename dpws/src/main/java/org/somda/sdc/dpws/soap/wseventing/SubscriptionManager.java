package org.somda.sdc.dpws.soap.wseventing;

import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;

import java.time.Duration;
import java.time.LocalDateTime;
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

    /**
     * Resets the expires duration.
     * <p>
     * This will also affect {@link #getExpiresTimeout()}.
     *
     * @param expires the duration to reset.
     */
    void renew(Duration expires);
}
