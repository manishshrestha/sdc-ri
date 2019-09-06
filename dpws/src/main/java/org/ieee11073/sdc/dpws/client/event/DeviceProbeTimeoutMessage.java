package org.ieee11073.sdc.dpws.client.event;

import org.ieee11073.sdc.common.event.EventMessage;

/**
 * Indicate probe end of a certain discovery process.
 */
public class DeviceProbeTimeoutMessage implements EventMessage {
    private final Integer foundDevicesCount;
    private final String discoveryId;

    public DeviceProbeTimeoutMessage(Integer foundDevicesCount, String discoveryId) {
        this.foundDevicesCount = foundDevicesCount;
        this.discoveryId = discoveryId;
    }

    /**
     * Get discovery identifier of the discovery process that has ended.
     */
    public String getDiscoveryId() {
        return discoveryId;
    }

    /**
     * Get amount of devices that were found for the discovery phase that matches this{@link #getDiscoveryId()}.
     */
    public Integer getFoundDevicesCount() {
        return foundDevicesCount;
    }
}
