package org.ieee11073.sdc.dpws.service;

import org.ieee11073.sdc.dpws.model.ThisDeviceType;
import org.ieee11073.sdc.dpws.model.ThisModelType;
import org.ieee11073.sdc.dpws.soap.RequestResponseClient;

import javax.xml.namespace.QName;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Proxy interface for hosting services.
 */
public interface HostingServiceProxy extends RequestResponseClient {
    /**
     * Get address where to access the hosting service.
     * @return A resolvable URI (i.e., URL).
     */
    URI getEndpointReferenceAddress();

    /**
     * Get types of hosting service.
     * @return List of types, which classifies the hosting service.
     */
    List<QName> getTypes();

    /**
     * @return DPWS ThisModel information of the remote hosting service.
     */
    Optional<ThisModelType> getThisModel();

    /**
     * @return DPWS ThisDevice information of the remote hosting service.
     */
    Optional<ThisDeviceType> getThisDevice();

    /**
     * @return a list of all known hosted services.
     */
    Map<String, HostedServiceProxy> getHostedServices();

    URI getActiveXAddr();

    long getMetadataVersion();

    RequestResponseClient getRequestResponseClient();
}
