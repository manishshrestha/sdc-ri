package org.ieee11073.sdc.dpws.soap.wseventing.helper;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import org.ieee11073.sdc.dpws.soap.wseventing.SourceSubscriptionManager;
import org.ieee11073.sdc.dpws.soap.wseventing.event.SubscriptionAddedMessage;
import org.ieee11073.sdc.dpws.soap.wseventing.event.SubscriptionRemovedMessage;

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
     * Adds a subscription to the subscription registry.
     *
     * @param subscriptionManager the subscription manager to add to the registy.
     */
    public void addSubscription(SourceSubscriptionManager subscriptionManager) {
        subscriptionManagers.put(subscriptionManager.getSubscriptionId(), subscriptionManager);
        eventBus.post(new SubscriptionAddedMessage(subscriptionManager));
    }

    /**
     * Removes a subscription from the subscription registry.
     *
     * @param subscriptionId the identifier of the subscription to remove.
     * @return the removed {@link SourceSubscriptionManager} instance if found, otherwise {@link Optional#empty()}.
     */
    public Optional<SourceSubscriptionManager> removeSubscription(String subscriptionId) {
        SourceSubscriptionManager removedSub = subscriptionManagers.remove(subscriptionId);
        if (removedSub != null) {
            eventBus.post(new SubscriptionRemovedMessage(removedSub));
        }
        return Optional.ofNullable(removedSub);
    }

    /**
     * Gets a subscription from the subscription registry.
     *
     * @param subscriptionId the identifier of the subscription to retrieve.
     * @return the {@link SourceSubscriptionManager} instance if found, otherwise {@link Optional#empty()}.
     */
    public Optional<SourceSubscriptionManager> getSubscription(String subscriptionId) {
        return Optional.ofNullable(subscriptionManagers.get(subscriptionId));
    }

    /**
     * Returns a copied snapshot of all available subscription managers.
     *
     * @return all subscription managers as a copy.
     */
    public Map<String, SourceSubscriptionManager> getSubscriptions() {
        return new HashMap<>(subscriptionManagers);
    }

    /**
     * Registers an {@link EventBus} observer to enable tracking of subscription insertion and deletion.
     *
     * @param observer an observer with {@link com.google.common.eventbus.Subscribe} annotated methods and
     *                 the first argument of type {@link SubscriptionAddedMessage} or {@link SubscriptionRemovedMessage}.
     * @see EventBus#register(Object)
     */
    public void registerObserver(Object observer) {
        eventBus.register(observer);
    }

    /**
     * Removes an observer formerly registered via {@link #registerObserver(Object)}.
     *
     * @param observer the observer to unregister.
     * @see EventBus#unregister(Object)
     */
    public void unregisterObserver(Object observer) {
        eventBus.unregister(observer);
    }
}
