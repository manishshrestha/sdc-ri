package org.ieee11073.sdc.dpws.soap.wseventing.helper;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import org.ieee11073.sdc.dpws.soap.wseventing.SourceSubscriptionManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe provision of a set of a subscription managers plus tracking mechanism.
 */
public class SubscriptionRegistry {
    private final EventBus eventBus;
    private final Map<String, SourceSubscriptionManager> subscriptionManagers;

    @Inject
    SubscriptionRegistry(EventBus eventBus) {
        this.eventBus = eventBus;
        this.subscriptionManagers = new ConcurrentHashMap<>();
    }

    /**
     * Add a subscription to the subscription registry.
     */
    public void addSubscription(SourceSubscriptionManager subMan) {
        subscriptionManagers.put(subMan.getSubscriptionId(), subMan);
        eventBus.post(new SubscriptionAddedMessage(subMan));
    }

    /**
     * Remove a subscription from the subscription registry.
     *
     * @param subscriptionId Identifier of the subscription to remove.
     * @return The removed {@link SourceSubscriptionManager} instance if found, otherwise {@link Optional#empty()}.
     */
    public Optional<SourceSubscriptionManager> removeSubscription(String subscriptionId) {
        SourceSubscriptionManager removedSub = subscriptionManagers.remove(subscriptionId);
        if (removedSub != null) {
            eventBus.post(new SubscriptionRemovedMessage(removedSub));
        }
        return Optional.ofNullable(removedSub);
    }

    /**
     * Get a subscription from the subscription registry.
     *
     * @param subscriptionId Identifier of the subscription to retrieve.
     * @return The {@link SourceSubscriptionManager} instance if found, otherwise {@link Optional#empty()}.
     */
    public Optional<SourceSubscriptionManager> getSubscription(String subscriptionId) {
        return Optional.ofNullable(subscriptionManagers.get(subscriptionId));
    }

    /**
     * Return a copied snapshot of all available subscription managers.
     */
    public Map<String, SourceSubscriptionManager> getSubscriptions() {
        return new HashMap<>(subscriptionManagers);
    }

    /**
     * Register an {@link EventBus} observer to enable tracking of subscription insertion and deletion.
     *
     * @param observer An observer with annotated {@link com.google.common.eventbus.Subscribe} annotated methods and
     *                 the first argument of type {@link SubscriptionAddedMessage} or
     *                 {@link SubscriptionRemovedMessage}.
     * @see EventBus#register(Object)
     */
    public void registerObserver(Object observer) {
        eventBus.register(observer);
    }

    /**
     * Remove an observer formerly registered via {@link #registerObserver(Object)}.
     *
     * @see EventBus#unregister(Object)
     */
    public void unregisterObserver(Object observer) {
        eventBus.unregister(observer);
    }
}
