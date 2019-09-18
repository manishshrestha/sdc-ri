package org.ieee11073.sdc.dpws.soap.wsdiscovery.factory;

import com.google.inject.assistedinject.Assisted;
import org.ieee11073.sdc.dpws.soap.NotificationSource;
import org.ieee11073.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.WsDiscoveryTargetService;

/**
 * Factory to create WS-Discovery target services.
 */
public interface WsDiscoveryTargetServiceFactory {
    /**
     * @param targetServiceEpr   the target service endpoint reference.
     * @param notificationSource the source where to send Hello and Bye messages from.
     * @return the instance.
     * @see <a href="http://docs.oasis-open.org/ws-dd/discovery/1.1/os/wsdd-discovery-1.1-spec-os.html#_Toc234231810">Endpoint References</a>
     */
    WsDiscoveryTargetService createWsDiscoveryTargetService(@Assisted EndpointReferenceType targetServiceEpr,
                                                            @Assisted NotificationSource notificationSource);
}
