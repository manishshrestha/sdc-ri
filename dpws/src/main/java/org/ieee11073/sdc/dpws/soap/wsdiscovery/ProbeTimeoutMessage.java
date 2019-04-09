package org.ieee11073.sdc.dpws.soap.wsdiscovery;

import org.ieee11073.sdc.common.event.EventMessage;
import org.ieee11073.sdc.dpws.client.DiscoveryFilter;

/**
 * Message that signalizes the end of a probe phase.
 */
public class ProbeTimeoutMessage implements EventMessage {
    private final Integer probeMatchesCount;
    private final String probeRequestId;

    public ProbeTimeoutMessage(Integer probeMatchesCount, String probeRequestId) {
        this.probeMatchesCount = probeMatchesCount;
        this.probeRequestId = probeRequestId;
    }

    /**
     * Identifier that uniquely defines the probe request.
     *
     * The identifier is automatically generated at construction time of {@link DiscoveryFilter}
     * objects. Access it by using {@link DiscoveryFilter#getDiscoveryId()}.
     */
    public String getProbeRequestId() {
        return probeRequestId;
    }

    /**
     * Get amount of matched items.
     *
     * Note that the maximum amount of matched items can be limited.
     */
    public Integer getProbeMatchesCount() {
        return probeMatchesCount;
    }
}

