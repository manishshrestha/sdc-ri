package org.somda.sdc.proto.discovery.consumer.event;

import org.somda.sdc.common.event.AbstractEventMessage;
import org.somda.protosdc.proto.model.discovery.DiscoveryTypes;
import org.somda.protosdc.proto.model.discovery.Endpoint;

public class DeviceLeftMessage extends AbstractEventMessage<Endpoint> {
    public DeviceLeftMessage(Endpoint payload) {
        super(payload);
    }
}
