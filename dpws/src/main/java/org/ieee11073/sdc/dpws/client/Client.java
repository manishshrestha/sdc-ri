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
 *
 * Important note: in case the client does not work, did you start the {@link org.ieee11073.sdc.dpws.DpwsFramework}?
 */
public interface Client extends Service {
    /**
     * Subscribe to discovery events.
     *
     * Discovery events are fired after {@link #probe(DiscoveryFilter)} calls as well as through Hello and Bye
     * messages sent by any devices.
     */
    void registerDiscoveryObserver(DiscoveryObserver observer);

    /**
     * Unsubscribe from discovery events.
     */
    void unregisterDiscoveryObserver(DiscoveryObserver observer);

    /**
     * Probe for devices.
     *
     * This method synchronously sends a WS-Discovery Probe settled with given filter data. All parties that subscribed
     * to event messages by using {@link #registerDiscoveryObserver(DiscoveryObserver)}, will be notified on found
     * devices and probe ending.
     *
     * @param discoveryFilter Types and scopes the discovery process shall filter against.
     */
    void probe(DiscoveryFilter discoveryFilter) throws TransportException;

    /**
     * Send a directed probe to a specific physical address.
     *
     * This method is an asynchronous unidirectional call; the result will not be notified to any subscribed parties.
     *
     * @param xAddr Device's physical address.
     */
    ListenableFuture<ProbeMatchesType> directedProbe(URI xAddr);

    /**
     * Resolve physical addresses (XAddrs) of a device.
     *
     * This method is an asynchronous unidirectional call; the result will not be notified to any subscribed parties.
     *
     * @param eprAddress Endpoint reference address of the device to resolve.
     */
    ListenableFuture<DiscoveredDevice> resolve(URI eprAddress);

    /**
     * Connect to a hosting service by using {@link DiscoveredDevice}.
     *
     * This function requires a fully populated {@link DiscoveredDevice} including XAddrs.
     *
     * By saying connect, this method resolves a hosting service by using WS-TransferGet and hosted service information
     * by using WS-MetadataExchange.
     *
     * @param discoveredDevice A fully populated {@link DiscoveredDevice}.
     */
    ListenableFuture<HostingServiceProxy> connect(DiscoveredDevice discoveredDevice);

    /**
     * Connect to a hosting service by using an EPR address.
     *
     * Shortcut for first doing a {@link #resolve(URI)} followed by a {@link #connect(DiscoveredDevice)}.
     *
     * @param eprAddress EPR address of a device.
     */
    ListenableFuture<HostingServiceProxy> connect(URI eprAddress);
}
