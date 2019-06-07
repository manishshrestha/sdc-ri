package org.ieee11073.sdc.dpws.device;

import org.ieee11073.sdc.dpws.model.ThisDeviceType;
import org.ieee11073.sdc.dpws.model.ThisModelType;
import org.ieee11073.sdc.dpws.service.HostedService;

/**
 * Interface to access the hosting service provided by a {@link Device}.
 *
 * There is currently no support do remove or alter hosted services that have been added before.
 */
public interface HostingServiceAccess {
    /**
     * Add ThisModel definition.
     */
    void setThisDevice(ThisDeviceType thisDevice);

    /**
     * Update ThisModel definition.
     */
    void setThisModel(ThisModelType thisModel);

    /**
     * Add hosted service definition.
     *
     * Attention: there is currently no mechanism to verify if the hosted service was added before!
     */
    void addHostedService(HostedService hostedService);
}
