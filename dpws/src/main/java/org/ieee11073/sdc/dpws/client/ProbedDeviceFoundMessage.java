package org.ieee11073.sdc.dpws.client;

import org.ieee11073.sdc.common.event.AbstractEventMessage;

/**
 * Provide {@link DeviceProxy} object that is delivered through a ProbeMatches message for a certain discovery process.
 */
public class ProbedDeviceFoundMessage extends AbstractEventMessage<DeviceProxy> {
    private final String discoveryId;

    public ProbedDeviceFoundMessage(DeviceProxy payload, String discoveryId) {
        super(payload);
        this.discoveryId = discoveryId;
    }

    /**
     * Get discovery identifier of the discovery process that the found device proxy relates to.
     */
    public String getDiscoveryId() {
        return discoveryId;
    }
}
