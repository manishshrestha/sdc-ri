package org.ieee11073.sdc.dpws.service.factory;

import com.google.inject.assistedinject.Assisted;
import org.ieee11073.sdc.dpws.model.ThisDeviceType;
import org.ieee11073.sdc.dpws.model.ThisModelType;
import org.ieee11073.sdc.dpws.service.HostedServiceProxy;
import org.ieee11073.sdc.dpws.service.HostingService;
import org.ieee11073.sdc.dpws.service.HostingServiceProxy;
import org.ieee11073.sdc.dpws.soap.RequestResponseClient;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.WsDiscoveryTargetService;

import javax.annotation.Nullable;
import javax.xml.namespace.QName;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Factory to create {@link HostingService} instances.
 */
public interface HostingServiceFactory {
    /**
     * Creates a hosting service.
     *
     * @param targetService WS-Discovery target service information used by the hosting service.
     * @return a {@link HostingService} instance for targetService.
     */
    HostingService createHostingService(@Assisted WsDiscoveryTargetService targetService);

    /**
     * Creates a hosting service proxy.
     *
     * @param endpointReferenceAddress endpoint reference address of the hosting service (unique device identifier).
     * @param types list of types of the hosting service.
     * @param thisDevice ThisDevice information of the hosting service.
     * @param thisModel ThisModel information of the hosting service.
     * @param hostedServices map of service ids to hosted service proxies.
     * @param metadataVersion metadata version used for hosting service. todo DGr remove version, bc unreliable info
     * @param requestResponseClient request-response client to be used to access the hosting service.
     * @param activeXAddr physical address that was used to retrieve the hosting service proxy information.
     * @return a {@link HostingServiceProxy} instance.
     */
    HostingServiceProxy createHostingServiceProxy(@Assisted("eprAddress") URI endpointReferenceAddress,
                                                  @Assisted List<QName> types,
                                                  @Assisted @Nullable ThisDeviceType thisDevice,
                                                  @Assisted @Nullable ThisModelType thisModel,
                                                  @Assisted Map<String, HostedServiceProxy> hostedServices,
                                                  @Assisted long metadataVersion,
                                                  @Assisted RequestResponseClient requestResponseClient,
                                                  @Assisted("activeXAddr") URI activeXAddr);
}
