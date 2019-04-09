package org.ieee11073.sdc.dpws.soap.wseventing.factory;

import com.google.inject.assistedinject.Assisted;
import org.ieee11073.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.ieee11073.sdc.dpws.soap.wseventing.SinkSubscriptionManager;
import org.ieee11073.sdc.dpws.soap.wseventing.SourceSubscriptionManager;
import org.ieee11073.sdc.dpws.soap.wseventing.SubscriptionManager;

import javax.annotation.Nullable;
import java.time.Duration;

/**
 * Create {@link SubscriptionManager} instances for event source and sink side.
 */
public interface SubscriptionManagerFactory {
    /**
     * Create {@link SourceSubscriptionManager} instance.
     *
     * @param subscriptionManagerEpr Endpoint reference where to send subscription modification requests (GetStatus,
     *                               Renew, Unsubscribe).
     * @param expires                Expiration duration.
     * @param notifyTo               Endpoint reference where to send notifications to.
     * @param endTo                  Endpoint reference where to send end-to request to or null, if none is available.
     * @param subscriptionId         The subscription id for the subscription manager or null to auto-generate a unique id.
     */
    SourceSubscriptionManager createSourceSubscriptionManager(@Assisted("SubscriptionManager") EndpointReferenceType subscriptionManagerEpr,
                                                              @Assisted Duration expires,
                                                              @Assisted("NotifyTo") EndpointReferenceType notifyTo,
                                                              @Assisted("EntTo") @Nullable EndpointReferenceType endTo,
                                                              @Assisted("SubscriptionId") @Nullable String subscriptionId);

    /**
     * Create {@link SinkSubscriptionManager} instance.
     *
     * @param subscriptionManager The subscription manager the sink side is based on.
     */
    SinkSubscriptionManager createSinkSubscriptionManager(@Assisted SubscriptionManager subscriptionManager);
}
