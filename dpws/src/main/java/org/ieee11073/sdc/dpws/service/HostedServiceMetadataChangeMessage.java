package org.ieee11073.sdc.dpws.service;

import org.ieee11073.sdc.common.event.EventMessage;
import org.ieee11073.sdc.dpws.helper.PeerInformation;
import org.ieee11073.sdc.dpws.model.HostedServiceType;
import org.ieee11073.sdc.dpws.soap.RequestResponseClient;

/**
 * Changeset on changes of hosted service metadata.
 */
public class HostedServiceMetadataChangeMessage implements EventMessage {
    private final HostedServiceType typeBefore;
    private final HostedServiceType typeAfter;
    private final RequestResponseClient requestResponseClient;
    private final PeerInformation peerInformation;

    public HostedServiceMetadataChangeMessage(HostedServiceType typeBefore,
                                              HostedServiceType typeAfter,
                                              RequestResponseClient requestResponseClient,
                                              PeerInformation peerInformation) {
        this.typeBefore = typeBefore;
        this.typeAfter = typeAfter;
        this.requestResponseClient = requestResponseClient;
        this.peerInformation = peerInformation;
    }

    public HostedServiceType getTypeAfter() {
        return typeAfter;
    }

    public HostedServiceType getTypeBefore() {
        return typeBefore;
    }

    public RequestResponseClient getRequestResponseClient() {
        return requestResponseClient;
    }

    public PeerInformation getPeerInformation() {
        return peerInformation;
    }
}
