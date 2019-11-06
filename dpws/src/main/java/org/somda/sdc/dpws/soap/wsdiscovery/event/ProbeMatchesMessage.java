package org.ieee11073.sdc.dpws.soap.wsdiscovery.event;

import org.ieee11073.sdc.common.event.AbstractEventMessage;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.model.ProbeMatchesType;

/**
 * ProbeMatches event message.
 */
public class ProbeMatchesMessage extends AbstractEventMessage<ProbeMatchesType> {
    private final String probeRequestId;

    /**
     * Constructor.
     *
     * @param probeRequestId the identifier to match to a probe request.
     * @param payload the payload that encloses ProbeMatches data.
     */
    public ProbeMatchesMessage(String probeRequestId, ProbeMatchesType payload) {
        super(payload);
        this.probeRequestId = probeRequestId;
    }

    /**
     * Gets the identifier that relates a probe request and response to each other.
     *
     * @return the probe request identifier.
     */
    public String getProbeRequestId() {
        return probeRequestId;
    }
}
