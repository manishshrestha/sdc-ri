package org.ieee11073.sdc.dpws.client;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Service;
import org.ieee11073.sdc.dpws.service.HostingServiceProxy;

import java.net.URI;

/**
 * Core class to access DPWS client functionality.
 *
 * Before access to client functions can be granted, make sure to start the client service by using
 * {@link #startAsync()}. Use {@link #stopAsync()} to stop the client service.
 */
public interface Client extends Service {
    /**
     * Subscribe to discovery events.
     *
     * Discovery events are invoked through {@link #probe(DiscoveryFilter)} calls as well as through Hello and Bye
     * messages send by any devices.
     */
    void registerDiscoveryObserver(DiscoveryObserver observer);

    /**
     * Unsubscribe from discovery events.
     */
    void unregisterDiscoveryObserver(DiscoveryObserver observer);

    /**
     * Probe for devices.
     *
     * This method is an asynchronous call. All parties that subscribed to event messages by using
     * {@link #registerDiscoveryObserver(DiscoveryObserver)}, will be notified on found devices and probe ending.
     *
     * @param discoveryFilter Types and scopes the discovery process shall filter against.
     */
    void probe(DiscoveryFilter discoveryFilter);

    /**
     * Connect to a hosting service.
     *
     * By saying connect, this method resolves hosting service and hosted service information,
     * which is afterwards available for usage. The hosting service and hosted service information is stored in an
     * internal registry and is updated automatically when a new metadata version is detected.
     *
     * Devices have to be disconnected manually by calling {@link #disconnectHostingService(URI)}. By doing so, no
     * metadata changes are tracked anymore, plus the hosting service and hosted service information is removed from
     * the internal registry.
     *
     * If configured, connecting to a device also starts a watchdog. The watchdog sends a {@link DeviceLeftMessage}
     * with {@link DeviceLeftMessage.TriggerType#WATCHDOG}. Configure with
     *
     * - {@link ClientConfig#ENABLE_WATCHDOG}
     * - {@link ClientConfig#WATCHDOG_PERIOD}
     *
     * \todo implement automatic update on device metadata changes
     * @param deviceProxy
     * @return
     */
    ListenableFuture<HostingServiceProxy> connectHostingService(DeviceProxy deviceProxy);

    /**
     * Use this function to disconnect a device with a specific device UUID.
     *
     * By saying disconnect, the hosting service and hosted service information is removed from the internal registry
     * and metadata updates as well as the watchdog is not considered anymore.
     *
     * @param deviceUuid UUID of the device to remove.
     */
    void disconnectHostingService(URI deviceUuid);
}
