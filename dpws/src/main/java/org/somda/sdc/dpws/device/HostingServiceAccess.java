package org.somda.sdc.dpws.device;

import org.somda.sdc.dpws.model.ThisDeviceType;
import org.somda.sdc.dpws.model.ThisModelType;
import org.somda.sdc.dpws.service.HostedService;

/**
 * Interface to access the hosting service provided by a {@link Device}.
 * <p>
 * <i>Important note: there is no support to remove or alter hosted services that have been added before.</i>
 */
public interface HostingServiceAccess {
    /**
     * Adds the ThisModel definition.
     *
     * @param thisDevice the ThisModel information to set.
     */
    void setThisDevice(ThisDeviceType thisDevice);

    /**
     * Updates the ThisModel definition.
     *
     * @param thisModel the ThisModel information to set.
     */
    void setThisModel(ThisModelType thisModel);

    /**
     * Adds a hosted service definition.
     * <p>
     * <i>Attention: there is currently no mechanism that verifies if the hosted service was added before!</i>
     *
     * @param hostedService the hosted service definition to add.
     */
    void addHostedService(HostedService hostedService);
}
