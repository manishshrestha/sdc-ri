package org.ieee11073.sdc.dpws.client.event;

import org.ieee11073.sdc.common.event.AbstractEventMessage;
import org.ieee11073.sdc.dpws.client.DiscoveredDevice;

/**
 * Provide {@link DiscoveredDevice} object that is delivered through a ProbeMatches message for a certain Probe
 * request.
 */
public class ProbedDeviceFoundMessage extends AbstractEventMessage<DiscoveredDevice> {
    private final String discoveryId;

    public ProbedDeviceFoundMessage(DiscoveredDevice payload, String discoveryId) {
        super(payload);
        this.discoveryId = discoveryId;
    }

    /**
     * @return discovery identifier of the discovery process that the discovered device relates to.
     */
    public String getDiscoveryId() {
        return discoveryId;
    }
}
