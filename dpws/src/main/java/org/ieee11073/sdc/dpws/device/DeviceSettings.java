package org.ieee11073.sdc.dpws.device;

import org.ieee11073.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;

import java.net.URI;
import java.util.List;

/**
 * Settings used in the setup process of a device.
 */
public interface DeviceSettings {
    /**
     * The unique and persisted endpoint reference (EPR) of the device.
     */
    EndpointReferenceType getEndpointReference();

    /**
     * Bindings that are used to make the device accessible from network.
     */
    List<URI> getHostingServiceBindings();
}
