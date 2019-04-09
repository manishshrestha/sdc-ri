package org.ieee11073.sdc.dpws;

import org.ieee11073.sdc.dpws.model.LocalizedStringType;
import org.ieee11073.sdc.dpws.model.ObjectFactory;
import org.ieee11073.sdc.dpws.model.ThisDeviceType;

import java.util.List;

/**
 * Convenient class to build DPWS' ThisDevice.
 */
public class ThisDeviceBuilder {
    private final ThisDeviceType thisDevice;

    public ThisDeviceBuilder() {
        thisDevice = (new ObjectFactory()).createThisDeviceType();
    }

    public ThisDeviceBuilder(List<LocalizedStringType> friendlyName) {
        this();
        thisDevice.setFriendlyName(friendlyName);
    }

    public ThisDeviceBuilder setFriendlyName(List<LocalizedStringType> friendlyName) {
        thisDevice.setFriendlyName(friendlyName);
        return this;
    }

    public ThisDeviceBuilder setFirmwareVersion(String firmwareVersion) {
        thisDevice.setFirmwareVersion(firmwareVersion);
        return this;
    }

    public ThisDeviceBuilder setSerialNumber(String serialNumber) {
        thisDevice.setSerialNumber(serialNumber);
        return this;
    }

    public ThisDeviceType get() {
        return thisDevice;
    }
}
