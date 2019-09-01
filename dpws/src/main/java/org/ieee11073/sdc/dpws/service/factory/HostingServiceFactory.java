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
     * Create a hosting service.
     *
     * @param targetService WS-Discovery target service information used by the hosting service.
     * @return a {@link HostingService} instance for the {@code targetService}
     */
    HostingService createHostingService(@Assisted WsDiscoveryTargetService targetService);

    /**
     *
     * @param endpointReferenceAddress endpoint reference address for hosting service
     * @param types List of types for hosting service
     * @param thisDevice {@link ThisDeviceType} for hosting service
     * @param thisModel {@link ThisModelType} for hosting service
     * @param hostedServices Map of service ids to hosted service proxies.
     * @param metadataVersion metadata version used for hosting service
     * @param requestResponseClient {@link RequestResponseClient} to use for hosting service
     * @param activeXAddr Physical address where to reach the hosting service proxy
     * @return a {@link HostingServiceProxy} instance
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
