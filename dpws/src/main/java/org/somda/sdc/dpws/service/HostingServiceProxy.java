package org.somda.sdc.dpws.service;

import org.somda.sdc.dpws.model.ThisDeviceType;
import org.somda.sdc.dpws.model.ThisModelType;
import org.somda.sdc.dpws.soap.RequestResponseClient;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Hosting service proxy of a client.
 */
public interface HostingServiceProxy extends RequestResponseClient {
    /**
     * Gets address where to access the hosting service.
     *
     * @return a resolvable URI (i.e., URL).
     */
    String getEndpointReferenceAddress();

    /**
     * Gets types of the hosting service.
     *
     * @return list of types, which classifies the hosting service.
     */
    List<QName> getTypes();

    /**
     * Gets the DPWS ThisModel information.
     *
     * @return ThisModel information.
     */
    Optional<ThisModelType> getThisModel();

    /**
     * Gets the DPWS ThisDevice information.
     *
     * @return ThisDevice information.
     */
    Optional<ThisDeviceType> getThisDevice();

    /**
     * Gets all hosted service proxies.
     *
     * @return all hosted services.
     */
    Map<String, HostedServiceProxy> getHostedServices();

    /**
     * Gets the physical address that is actively being used to send requests.
     * <p>
     * A hosting service can have different physical addresses in order to be accessible.
     * The one that is returned with this function is the one that was used to initially resolve metadata (GetTransfer requests).
     *
     * @return the currently active EPR address.
     */
    String getActiveXAddr();

    /**
     * Gets the metadata version.
     * <p>
     * <em>Attention: this data might have come from an unreliable source.</em>
     *
     * todo DGr remove that function - the metadata version does not necessarily reflect the hosting service data
     *
     * @return the metadata version
     */
    long getMetadataVersion();

    RequestResponseClient getRequestResponseClient();
}
