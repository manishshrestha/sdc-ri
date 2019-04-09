package org.ieee11073.sdc.dpws.client;

import org.ieee11073.sdc.common.event.AbstractEventMessage;

import java.net.URI;

/**
 * Provide device proxy information that is delivered through a Bye message or a watchdog timeout.
 */
public class DeviceLeftMessage extends AbstractEventMessage<URI> {
    private final TriggerType triggerType;

    public DeviceLeftMessage(URI payload, TriggerType triggerType) {
        super(payload);
        this.triggerType = triggerType;
    }

    public TriggerType getTriggerType() {
        return triggerType;
    }

    public enum TriggerType {
        BYE,
        WATCHDOG
    }
}
