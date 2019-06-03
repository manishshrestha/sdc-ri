package org.ieee11073.sdc.dpws.service;

import org.ieee11073.sdc.dpws.helper.PeerInformation;
import org.ieee11073.sdc.dpws.model.HostedServiceType;
import org.ieee11073.sdc.dpws.soap.RequestResponseClient;

import java.net.URI;

/**
 * {@link HostedServiceProxy} with capability to update information.
 */
public interface WritableHostedServiceProxy extends HostedServiceProxy {
    void updateProxyInformation(HostedServiceType type,
                                RequestResponseClient requestResponseClient,
                                URI activeEprAddress);
}
