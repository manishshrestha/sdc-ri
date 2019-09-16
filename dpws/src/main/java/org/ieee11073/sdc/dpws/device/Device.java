package org.ieee11073.sdc.dpws.device;

import com.google.common.util.concurrent.Service;

import javax.annotation.Nullable;

/**
 * Core class to create a device that exposes itself to the network.
 * <p>
 * In order to get a device up and running, perform the following steps
 * <ol>
 * <li>Configure the device appropriately by using {@link #setConfiguration(DeviceSettings)},
 * {@link #getDiscoveryAccess()} and {@link #getHostingServiceAccess()}.
 * <li>Use {@link #startAsync()} to start the device and send a WS-Discovery Hello.
 * <li>To stop the device, invoke {@link #stopAsync()}. This will send a WS-Discovery Bye.
 * </ol>
 */
public interface Device extends Service {
    /**
     * Injects device configuration to be used by the device.
     * <p>
     * The configuration can only be set before the services is started.
     * <p>
     * todo DGr create factory that accepts the settings on creation.
     *
     * @param deviceSettings the device settings of the device.
     */
    void setConfiguration(@Nullable DeviceSettings deviceSettings);

    /**
     * Gets access to WS-Discovery in order to configure types and scopes.
     *
     * @return discovery access.
     */
    DiscoveryAccess getDiscoveryAccess();

    /**
     * Gets access to Hosting Service metadata and Hosted Services.
     *
     * @return hosting service access.
     */
    HostingServiceAccess getHostingServiceAccess();
}
