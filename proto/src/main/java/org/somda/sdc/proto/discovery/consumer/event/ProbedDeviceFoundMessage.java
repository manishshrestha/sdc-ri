package org.somda.sdc.proto.discovery.consumer.event;

import org.somda.sdc.common.event.AbstractEventMessage;
import org.somda.sdc.dpws.client.DiscoveredDevice;
import org.somda.sdc.proto.model.discovery.DiscoveryTypes;
import org.somda.sdc.proto.model.discovery.Endpoint;

import java.util.Collection;

public class ProbedDeviceFoundMessage extends AbstractEventMessage<Collection<Endpoint>> {
    private final String discoveryId;

    public ProbedDeviceFoundMessage(Collection<Endpoint> payload, String discoveryId) {
        super(payload);
        this.discoveryId = discoveryId;
    }

    public String getDiscoveryId() {
        return discoveryId;
    }
}
