package org.ieee11073.sdc.dpws.soap.wseventing.helper;

import org.ieee11073.sdc.common.event.AbstractEventMessage;
import org.ieee11073.sdc.dpws.soap.wseventing.SourceSubscriptionManager;

/**
 * Event message to signalize that a {@link SourceSubscriptionManager} instance was removed from a
 * {@link SubscriptionRegistry} instance.
 */
public class SubscriptionRemovedMessage extends AbstractEventMessage<SourceSubscriptionManager> {
    public SubscriptionRemovedMessage(SourceSubscriptionManager payload) {
        super(payload);
    }
}
