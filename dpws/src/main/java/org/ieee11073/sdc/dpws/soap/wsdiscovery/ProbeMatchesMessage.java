package org.ieee11073.sdc.dpws.soap.wsdiscovery;

import org.ieee11073.sdc.common.event.AbstractEventMessage;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.model.ProbeMatchesType;

/**
 * ProbeMatches event message.
 */
public class ProbeMatchesMessage extends AbstractEventMessage<ProbeMatchesType> {
    private final String probeRequestId;

    public ProbeMatchesMessage(String probeRequestId, ProbeMatchesType payload) {
        super(payload);
        this.probeRequestId = probeRequestId;
    }

    /**
     * Get identifier to identify the probe request the probe matches response relates to.
     */
    public String getProbeRequestId() {
        return probeRequestId;
    }
}
