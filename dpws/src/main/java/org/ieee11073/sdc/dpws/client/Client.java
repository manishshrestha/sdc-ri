package org.ieee11073.sdc.dpws.client;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Service;
import org.ieee11073.sdc.dpws.service.HostingServiceProxy;
import org.ieee11073.sdc.dpws.soap.exception.TransportException;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.model.ProbeMatchesType;

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
    void probe(DiscoveryFilter discoveryFilter) throws TransportException;

    /**
     *
     * Send a directed probe to a speecific physical address.
     * This method is an asynchronous call; the result will not be notified to subscribed parties.
     *
     * @param xAddr Physical device's address.
     */
    ListenableFuture<ProbeMatchesType> directedProbe(URI xAddr);


    /**
     * Resolve physical addresses (XAddrs) of a device.
     *
     * This method is an asynchronous call. All parties that subscribed to event messages by using
     * {@link #registerDiscoveryObserver(DiscoveryObserver)}, will be notified on resolved devices and resolve ending.
     *
     * @param eprAddress Endpoint reference address of the device to resolve.
     */
    ListenableFuture<DiscoveredDevice> resolve(URI eprAddress);

    /**
     * Connect to a hosting service by using {@link DiscoveredDevice}.
     *
     * This function requires a fully populated {@link DiscoveredDevice} including XAddrs.
     *
     * By saying connect, this method resolves hosting service and hosted service information,
     * which is afterwards available for usage.
     *
     * If configured, connecting to a device also starts a watchdog. The watchdog sends a {@link DeviceLeftMessage}
     * with {@link DeviceLeftMessage.TriggerType#WATCHDOG}. Configure with
     *
     * - {@link ClientConfig#ENABLE_WATCHDOG}
     * - {@link ClientConfig#WATCHDOG_PERIOD}
     *
     * \todo implement automatic update on device metadata changes
     * @param discoveredDevice A fully populated {@link DiscoveredDevice}.
     */
    ListenableFuture<HostingServiceProxy> connect(DiscoveredDevice discoveredDevice);

    /**
     * Connect to a hosting service by using an EPR address.
     *
     * Internally, {@linkplain #connect(URI)} resolves the device and invoked {@link #connect(DiscoveredDevice)}
     * to get hosting service data.
     *
     * @param eprAddress EPR address of a device.
     */
    ListenableFuture<HostingServiceProxy> connect(URI eprAddress);
}
