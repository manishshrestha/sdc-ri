package org.ieee11073.sdc.dpws.client.helper;

import com.google.inject.Inject;
import org.ieee11073.sdc.dpws.service.WritableHostingServiceProxy;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Provide thread-safe access to {@link WritableHostingServiceProxy} instances.
 */
public class HostingServiceRegistry {
    private final Map<URI, WritableHostingServiceProxy> hostingServices;
    private final Lock lock;

    @Inject
    HostingServiceRegistry() {
        this.lock = new ReentrantLock();
        this.hostingServices = new HashMap<>();
    }

    /**
     * Register a new hosting service object or update if it already exist.
     *
     * @param hostingServiceProxy The object to insert or update.
     * @return The hosting service that was inserted or updated. If the hosting service was not inserted,
     * hostingServiceProxy is inserted an returned. An update will overwrite any information of the
     * hosting service that is already stored in the registry and returns the entry that was updated.
     */
    public WritableHostingServiceProxy registerOrUpdate(WritableHostingServiceProxy hostingServiceProxy) {
        lock.lock();
        try {
            URI hspUri = hostingServiceProxy.getEndpointReferenceAddress();
            WritableHostingServiceProxy hspFromRegistry = hostingServices.get(hspUri);
            if (hspFromRegistry == null) {
                hostingServices.put(hspUri, hostingServiceProxy);
                return hostingServiceProxy;
            } else {
                hspFromRegistry.updateProxyInformation(hostingServiceProxy);
                return hspFromRegistry;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Remove hosting service with given EPR address.
     *
     * @param eprAddress The URI that uniquely identifies the hosting service
     * @return {@link Optional#empty()} if no hosting service was removed, otherwise the removed instance.
     */
    public Optional<WritableHostingServiceProxy> unregister(URI eprAddress) {
        lock.lock();
        try {
            return Optional.ofNullable(hostingServices.remove(eprAddress));
        } finally {
            lock.unlock();
        }
    }

    /**
     * Resolve hosting service with given EPR address.
     * @param eprAddress The EPR address to search for.
     * @return The hosting service or @{@link Optional#empty()} if not found.
     */
    public Optional<WritableHostingServiceProxy> get(URI eprAddress) {
        lock.lock();
        try {
            return Optional.ofNullable(hostingServices.get(eprAddress));
        } finally {
            lock.unlock();
        }
    }
}
