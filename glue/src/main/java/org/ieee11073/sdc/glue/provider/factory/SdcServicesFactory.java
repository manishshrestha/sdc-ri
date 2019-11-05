package org.ieee11073.sdc.glue.provider.factory;

import com.google.inject.assistedinject.Assisted;
import org.ieee11073.sdc.biceps.provider.access.LocalMdibAccess;
import org.ieee11073.sdc.dpws.device.Device;
import org.ieee11073.sdc.glue.provider.SdcServices;
import org.ieee11073.sdc.glue.provider.sco.OperationInvocationReceiver;

import java.util.Collection;

/**
 * Factory to create SDC compatible devices.
 */
public interface SdcServicesFactory {
    /**
     * Creates a new {@linkplain SdcServices} instance.
     *
     * @param dpwsDevice                   the DPPWS device where to add BICEPS services.
     * @param mdibAccess                   the MDIB to be exposed on the network.
     * @param operationInvocationReceivers callback interceptors for incoming set service requests.
     * @return a new {@link SdcServices}. Use {@link Device#startAsync()} in order to start exposing the device on the network.
     */
    SdcServices createSdcServices(@Assisted Device dpwsDevice,
                                  @Assisted LocalMdibAccess mdibAccess,
                                  @Assisted Collection<OperationInvocationReceiver> operationInvocationReceivers);
}
