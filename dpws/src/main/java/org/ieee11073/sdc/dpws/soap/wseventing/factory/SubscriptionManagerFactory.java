package org.ieee11073.sdc.dpws.soap.wseventing.factory;

import com.google.inject.assistedinject.Assisted;
import org.ieee11073.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.ieee11073.sdc.dpws.soap.wseventing.SinkSubscriptionManager;
import org.ieee11073.sdc.dpws.soap.wseventing.SourceSubscriptionManager;
import org.ieee11073.sdc.dpws.soap.wseventing.SubscriptionManager;

import javax.annotation.Nullable;
import java.time.Duration;

/**
 * Creates {@link SubscriptionManager} instances for event source and sink side.
 */
public interface SubscriptionManagerFactory {
    /**
     * Creates an {@link SourceSubscriptionManager} instance.
     *
     * @param subscriptionManagerEpr endpoint reference where to send subscription modification requests (GetStatus,
     *                               Renew, Unsubscribe).
     * @param expires                expiration duration.
     * @param notifyTo               endpoint reference where to send notifications to.
     * @param endTo                  endpoint reference where to send end-to request to or null, if none is available.
     * @param subscriptionId         the subscription id for the subscription manager or null to auto-generate a unique id.
     * @return the instance.
     */
    SourceSubscriptionManager createSourceSubscriptionManager(@Assisted("SubscriptionManager") EndpointReferenceType subscriptionManagerEpr,
                                                              @Assisted Duration expires,
                                                              @Assisted("NotifyTo") EndpointReferenceType notifyTo,
                                                              @Assisted("EntTo") @Nullable EndpointReferenceType endTo,
                                                              @Assisted("SubscriptionId") @Nullable String subscriptionId);

    /**
     * Creates a {@link SinkSubscriptionManager} instance.
     *
     * @param subscriptionManager the subscription manager the sink side is based on.
     * @return the instance.
     */
    SinkSubscriptionManager createSinkSubscriptionManager(@Assisted SubscriptionManager subscriptionManager);
}
