package org.somda.sdc.dpws.client.event;

import org.somda.sdc.common.event.AbstractEventMessage;

/**
 * Provides device proxy information that is delivered through a Bye message or a watchdog timeout.
 */
public class DeviceLeftMessage extends AbstractEventMessage<String> {
    private final TriggeredBy triggeredBy;

    public DeviceLeftMessage(String payload, TriggeredBy triggeredBy) {
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
