package org.ieee11073.sdc.dpws.service.factory;

import com.google.inject.assistedinject.Assisted;
import org.ieee11073.sdc.dpws.service.HostedService;
import org.ieee11073.sdc.dpws.service.HostedServiceInterceptor;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.WsDiscoveryTargetService;

/**
 * Factory to create {@link HostedServiceInterceptor} instances.
 */
public interface HostedServiceInterceptorFactory {
    /**
     * @param hostedService hosted service to create interceptor for
     * @param targetService matching {@link WsDiscoveryTargetService}
     * @return interceptor for given hosted service.
     */
    HostedServiceInterceptor createHostedServiceInterceptor(@Assisted HostedService hostedService,
                                                            @Assisted WsDiscoveryTargetService targetService);
}
