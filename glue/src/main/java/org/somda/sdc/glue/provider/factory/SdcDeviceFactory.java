package org.somda.sdc.glue.provider.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.dpws.device.Device;
import org.somda.sdc.dpws.device.DeviceSettings;
import org.somda.sdc.glue.provider.SdcDevice;
import org.somda.sdc.glue.provider.sco.OperationInvocationReceiver;

import java.util.Collection;

/**
 * Factory to create SDC compatible devices.
 */
public interface SdcDeviceFactory {
    /**
     * Creates a new {@linkplain SdcDevice} instance.
     *
     * @param deviceSettings               the DPPWS device settings to use.
     * @param mdibAccess                   the MDIB to be exposed on the network.
     * @param operationInvocationReceivers callback interceptors for incoming set service requests.
     * @return a new {@link SdcDevice}. Use {@link Device#startAsync()} in order to start exposing the device on the network.
     */
    SdcDevice createSdcDevice(@Assisted DeviceSettings deviceSettings,
                              @Assisted LocalMdibAccess mdibAccess,
                              @Assisted Collection<OperationInvocationReceiver> operationInvocationReceivers);
}
