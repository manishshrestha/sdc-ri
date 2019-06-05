package org.ieee11073.sdc.dpws.client;

import org.ieee11073.sdc.common.event.AbstractEventMessage;

/**
 * Provide device proxy information that is delivered through a Hello message.
 */
public class DeviceEnteredMessage extends AbstractEventMessage<DiscoveredDevice> {
    public DeviceEnteredMessage(DiscoveredDevice payload) {
        super(payload);
    }
}
