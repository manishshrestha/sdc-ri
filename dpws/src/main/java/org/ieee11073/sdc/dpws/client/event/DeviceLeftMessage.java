package org.ieee11073.sdc.dpws.client.event;

import org.ieee11073.sdc.common.event.AbstractEventMessage;

import java.net.URI;

/**
 * Provide device proxy information that is delivered through a Bye message or a watchdog timeout.
 */
public class DeviceLeftMessage extends AbstractEventMessage<URI> {
    private final TriggeredBy triggeredBy;

    public DeviceLeftMessage(URI payload, TriggeredBy triggeredBy) {
        super(payload);
        this.triggeredBy = triggeredBy;
    }

    public TriggeredBy getTriggeredBy() {
        return triggeredBy;
    }

    public enum TriggeredBy {
        BYE,
        WATCHDOG
    }
}
