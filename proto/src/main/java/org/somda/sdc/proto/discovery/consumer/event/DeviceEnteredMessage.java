package org.somda.sdc.proto.discovery.consumer.event;

import org.somda.sdc.common.event.AbstractEventMessage;
import org.somda.sdc.dpws.client.DiscoveredDevice;
import org.somda.sdc.proto.model.discovery.DiscoveryTypes;

public class DeviceEnteredMessage extends AbstractEventMessage<DiscoveryTypes.Endpoint> {
    public DeviceEnteredMessage(DiscoveryTypes.Endpoint payload) {
        super(payload);
    }
}
