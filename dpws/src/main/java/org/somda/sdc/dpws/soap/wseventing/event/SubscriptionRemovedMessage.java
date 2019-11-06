package org.ieee11073.sdc.dpws.soap.wseventing.event;

import org.ieee11073.sdc.common.event.AbstractEventMessage;
import org.ieee11073.sdc.dpws.soap.wseventing.SourceSubscriptionManager;
import org.ieee11073.sdc.dpws.soap.wseventing.helper.SubscriptionRegistry;

/**
 * Event message to signalize that a {@link SourceSubscriptionManager} instance was removed from a
 * {@link SubscriptionRegistry} instance.
 */
public class SubscriptionRemovedMessage extends AbstractEventMessage<SourceSubscriptionManager> {
    /**
     * Constructor.
     *
     * @param payload the subscription manager that has been removed.
     */
    public SubscriptionRemovedMessage(SourceSubscriptionManager payload) {
        super(payload);
    }
}
