package org.ieee11073.sdc.dpws.service.factory;

import com.google.inject.assistedinject.Assisted;
import org.ieee11073.sdc.dpws.service.HostedServiceProxy;
import org.ieee11073.sdc.dpws.service.HostedServiceTransportBinding;

/**
 * Factory to create {@link HostedServiceTransportBinding} instances.
 */
public interface HostedServiceTransportBindingFactory {
    /**
     * Create instance.
     *
     * @param hostedServiceProxy The hosted service to bind to.
     * @return transport binding for hosted service
     */
    HostedServiceTransportBinding createHostedServiceTransportBinding(@Assisted HostedServiceProxy hostedServiceProxy);
}
