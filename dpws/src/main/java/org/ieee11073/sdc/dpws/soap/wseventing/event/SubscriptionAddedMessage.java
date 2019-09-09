package org.ieee11073.sdc.dpws.soap.wseventing.event;

import org.ieee11073.sdc.common.event.AbstractEventMessage;
import org.ieee11073.sdc.dpws.soap.wseventing.SourceSubscriptionManager;
import org.ieee11073.sdc.dpws.soap.wseventing.helper.SubscriptionRegistry;

/**
 * Event message to signalize that a {@link SourceSubscriptionManager} instance was added to a
 * {@link SubscriptionRegistry} instance.
 */
public class SubscriptionAddedMessage extends AbstractEventMessage<SourceSubscriptionManager> {
    public SubscriptionAddedMessage(SourceSubscriptionManager payload) {
        super(payload);
    }
}
