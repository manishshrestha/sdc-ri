package org.ieee11073.sdc.dpws.service;

import org.ieee11073.sdc.dpws.helper.PeerInformation;
import org.ieee11073.sdc.dpws.model.HostedServiceType;
import org.ieee11073.sdc.dpws.soap.RequestResponseClient;

import java.net.URI;

/**
 * Proxy interface for hosted services.
 */
public interface HostedServiceProxy extends RequestResponseClient {
    /**
     * HostedServiceType definition.
     *
     * Method returns a copy of the internal information.
     */
    HostedServiceType getType();

    RequestResponseClient getRequestResponseClient();

    URI getActiveEprAddress();

    void registerMetadataChangeObserver(HostedServiceMetadataObserver observer);
    void unregisterMetadataChangeObserver(HostedServiceMetadataObserver observer);
}
