package org.ieee11073.sdc.dpws.device;

import org.ieee11073.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;

import java.net.URI;
import java.util.List;

/**
 * \todo Define and implement DeviceSettings
 */
public interface DeviceSettings {
    EndpointReferenceType getEndpointReference();
    List<URI> getHostingServiceBindings();
}
