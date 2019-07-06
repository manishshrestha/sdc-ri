package org.ieee11073.sdc.dpws.device;

import com.google.common.util.concurrent.Service;

import javax.annotation.Nullable;

/**
 * Core class to create a device that exposes itself to the network.
 *
 * First, configure the device appropriately by using {@link #setConfiguration(DeviceSettings)},
 * {@link #getDiscoveryAccess()}, and {@link #getHostingServiceAccess()}. Afterwards, use {@link #startAsync()} to
 * start the device and send a WS-Discovery Hello. To stop the device, invoke {@link #stopAsync()}. This will send a
 * WS-Discovery Bye.
 */
public interface Device extends Service {
    /**
     * Inject device configuration to be used by the device.
     */
    void setConfiguration(@Nullable DeviceSettings deviceSettings);

    /**
     * Get access to WS-Discovery to configure types and scopes.
     */
    DiscoveryAccess getDiscoveryAccess();

    /**
     * Get access to Hosting Service metadata and Hosted Services.
     */
    HostingServiceAccess getHostingServiceAccess();
}
