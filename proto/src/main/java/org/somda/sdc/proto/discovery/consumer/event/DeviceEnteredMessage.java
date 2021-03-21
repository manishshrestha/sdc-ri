package org.somda.sdc.proto.discovery.consumer.event;

import org.somda.sdc.common.event.AbstractEventMessage;
import org.somda.sdc.dpws.client.DiscoveredDevice;
import org.somda.protosdc.proto.model.discovery.DiscoveryTypes;
import org.somda.protosdc.proto.model.discovery.Endpoint;

public class DeviceEnteredMessage extends AbstractEventMessage<Endpoint> {
    public DeviceEnteredMessage(Endpoint payload) {
        super(payload);
    }
}
