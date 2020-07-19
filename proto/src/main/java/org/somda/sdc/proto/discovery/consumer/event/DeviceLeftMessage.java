package org.somda.sdc.proto.discovery.consumer.event;

import org.somda.sdc.common.event.AbstractEventMessage;
import org.somda.sdc.proto.model.discovery.DiscoveryTypes;

public class DeviceLeftMessage extends AbstractEventMessage<DiscoveryTypes.Endpoint> {
    public DeviceLeftMessage(DiscoveryTypes.Endpoint payload) {
        super(payload);
    }
}
