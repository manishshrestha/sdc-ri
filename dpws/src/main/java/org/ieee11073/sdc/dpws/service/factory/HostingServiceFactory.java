package org.ieee11073.sdc.dpws.service.factory;

import com.google.inject.assistedinject.Assisted;
import org.ieee11073.sdc.dpws.helper.PeerInformation;
import org.ieee11073.sdc.dpws.model.ThisDeviceType;
import org.ieee11073.sdc.dpws.model.ThisModelType;
import org.ieee11073.sdc.dpws.service.HostingService;
import org.ieee11073.sdc.dpws.service.WritableHostedServiceProxy;
import org.ieee11073.sdc.dpws.service.WritableHostingServiceProxy;
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
     */
    HostingService createHostingService(@Assisted WsDiscoveryTargetService targetService);

    /**
     *
     * @param endpointReferenceAddress
     * @param types
     * @param thisDevice
     * @param thisModel
     * @param hostedServices Map of service ids to hosted service proxies.
     * @param metadataVersion
     * @param requestResponseClient
     * @param peerInformation
     * @return
     */
    WritableHostingServiceProxy createHostingServiceProxy(@Assisted URI endpointReferenceAddress,
                                                          @Assisted List<QName> types,
                                                          @Assisted @Nullable ThisDeviceType thisDevice,
                                                          @Assisted @Nullable ThisModelType thisModel,
                                                          @Assisted Map<URI, WritableHostedServiceProxy> hostedServices,
                                                          @Assisted long metadataVersion,
                                                          @Assisted RequestResponseClient requestResponseClient,
                                                          @Assisted PeerInformation peerInformation);
}
