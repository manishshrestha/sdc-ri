package org.ieee11073.sdc.dpws.service;

import org.ieee11073.sdc.common.event.EventMessage;
import org.ieee11073.sdc.dpws.helper.PeerInformation;
import org.ieee11073.sdc.dpws.model.HostedServiceType;
import org.ieee11073.sdc.dpws.soap.RequestResponseClient;

import java.net.URI;

/**
 * Changeset on changes of hosted service metadata.
 */
public class HostedServiceMetadataChangeMessage implements EventMessage {
    private final HostedServiceType typeBefore;
    private final HostedServiceType typeAfter;
    private final RequestResponseClient requestResponseClient;
    private final URI activeEprAddress;

    public HostedServiceMetadataChangeMessage(HostedServiceType typeBefore,
                                              HostedServiceType typeAfter,
                                              RequestResponseClient requestResponseClient,
                                              URI activeEprAddress) {
        this.typeBefore = typeBefore;
        this.typeAfter = typeAfter;
        this.requestResponseClient = requestResponseClient;
        this.activeEprAddress = activeEprAddress;
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

    public URI getActiveEprAddress() {
        return activeEprAddress;
    }
}
