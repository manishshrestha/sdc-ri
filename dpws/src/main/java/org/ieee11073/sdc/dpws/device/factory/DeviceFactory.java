package org.ieee11073.sdc.dpws.device.factory;

import com.google.inject.assistedinject.Assisted;
import org.ieee11073.sdc.dpws.device.Device;
import org.ieee11073.sdc.dpws.device.DeviceSettings;

/**
 *
 */
public interface DeviceFactory {
    Device createDevice(@Assisted DeviceSettings deviceSettings);
}
