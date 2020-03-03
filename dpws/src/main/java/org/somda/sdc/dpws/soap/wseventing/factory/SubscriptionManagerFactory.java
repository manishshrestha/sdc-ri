package org.somda.sdc.dpws.soap.wseventing.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.dpws.soap.wseventing.SinkSubscriptionManager;
import org.somda.sdc.dpws.soap.wseventing.SourceSubscriptionManager;
import org.somda.sdc.dpws.soap.wseventing.SubscriptionManager;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Collection;

/**
 * Creates {@link SubscriptionManager} instances for event source and sink side.
 */
public interface SubscriptionManagerFactory {
    /**
     * Creates a {@link SourceSubscriptionManager} instance.
     *
     * @param subscriptionManagerEpr endpoint reference where to request subscription modification requests (GetStatus,
     *                               Renew, Unsubscribe).
     * @param expires                expiration duration.
     * @param notifyTo               endpoint reference where to send notifications to.
     * @param endTo                  endpoint reference where to send end-to request to or null if none is available.
     * @param subscriptionId         the subscription id for the subscription manager.
     * @return a new {@link SourceSubscriptionManager} instance.
     */
    SourceSubscriptionManager createSourceSubscriptionManager(@Assisted("SubscriptionManager") EndpointReferenceType subscriptionManagerEpr,
                                                              @Assisted Duration expires,
                                                              @Assisted("NotifyTo") EndpointReferenceType notifyTo,
                                                              @Assisted("EndTo") @Nullable EndpointReferenceType endTo,
                                                              @Assisted("SubscriptionId") String subscriptionId,
                                                              @Assisted("Actions") Collection<String> actions);

    /**
     * Creates a {@link SinkSubscriptionManager} instance.
     * <p>
     * A subscription id is assigned automatically.
     *
     * @param subscriptionManagerEpr
     * @param expires
     * @param notifyTo
     * @param endTo
     * @return a new {@link SinkSubscriptionManager} instance.
     */
    SinkSubscriptionManager createSinkSubscriptionManager(@Assisted("SubscriptionManager") EndpointReferenceType subscriptionManagerEpr,
                                                          @Assisted Duration expires,
                                                          @Assisted("NotifyTo") EndpointReferenceType notifyTo,
                                                          @Assisted("EndTo") EndpointReferenceType endTo,
                                                          @Assisted("Actions") Collection<String> actions);
}
