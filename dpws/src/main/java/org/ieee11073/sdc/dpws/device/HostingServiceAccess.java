package org.ieee11073.sdc.dpws.device;

import org.ieee11073.sdc.dpws.model.ThisDeviceType;
import org.ieee11073.sdc.dpws.model.ThisModelType;
import org.ieee11073.sdc.dpws.service.HostedService;

/**
 * Interface to access the hosting service provided by a {@link Device}.
 */
public interface HostingServiceAccess {
    void setThisDevice(ThisDeviceType thisDevice);
    void setThisModel(ThisModelType thisModel);
    void addHostedService(HostedService hostedService);
}
