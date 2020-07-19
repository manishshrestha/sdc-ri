package org.somda.sdc.proto.discovery.consumer.event;

import org.somda.sdc.common.event.EventMessage;

public class DeviceProbeTimeoutMessage implements EventMessage {
    private final Integer foundDevicesCount;
    private final String discoveryId;

    public DeviceProbeTimeoutMessage(Integer foundDevicesCount, String discoveryId) {
        this.foundDevicesCount = foundDevicesCount;
        this.discoveryId = discoveryId;
    }

    public String getDiscoveryId() {
        return discoveryId;
    }

    public Integer getFoundDevicesCount() {
        return foundDevicesCount;
    }
}
