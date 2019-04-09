package org.ieee11073.sdc.dpws.soap.wseventing.helper;

import org.ieee11073.sdc.common.event.AbstractEventMessage;
import org.ieee11073.sdc.dpws.soap.wseventing.SourceSubscriptionManager;

/**
 * Event message to signalize that a {@link SourceSubscriptionManager} instance was added to a
 * {@link SubscriptionRegistry} instance.
 */
public class SubscriptionAddedMessage extends AbstractEventMessage<SourceSubscriptionManager> {
    public SubscriptionAddedMessage(SourceSubscriptionManager payload) {
        super(payload);
    }
}
