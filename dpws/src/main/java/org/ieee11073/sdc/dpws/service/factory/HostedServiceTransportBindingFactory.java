package org.ieee11073.sdc.dpws.service.factory;

import com.google.inject.assistedinject.Assisted;
import org.ieee11073.sdc.dpws.service.HostedServiceProxy;
import org.ieee11073.sdc.dpws.service.HostedServiceMetadataChangeMessage;
import org.ieee11073.sdc.dpws.service.HostedServiceTransportBinding;

/**
 * Factory to create {@link HostedServiceTransportBinding} instances.
 */
public interface HostedServiceTransportBindingFactory {
    /**
     * Create instance.
     *
     * Subscribe this instance to receive {@link HostedServiceMetadataChangeMessage},
     * otherwise metadata updates during runtime will not be considered by this transport binding.
     *
     * @param hostedServiceProxy The hosted service where to bind to.
     */
    HostedServiceTransportBinding createHostedServiceTransportBinding(@Assisted HostedServiceProxy hostedServiceProxy);
}
