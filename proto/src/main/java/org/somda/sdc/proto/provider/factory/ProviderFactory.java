package org.somda.sdc.proto.provider.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.glue.provider.SdcDevice;
import org.somda.sdc.glue.provider.SdcDevicePlugin;
import org.somda.sdc.glue.provider.sco.OperationInvocationReceiver;
import org.somda.sdc.proto.provider.Provider;
import org.somda.sdc.proto.provider.ProviderSettings;

import java.util.Collection;

public interface ProviderFactory {
    /**
     * Creates a new {@linkplain Provider} instance.
     *
     * @param eprAddress                   the unique and persisted endpoint reference (EPR) of the device
     * @param providerSettings             settings, e.g. the network interface the device shall bind to
     * @param mdibAccess                   the MDIB to be exposed on the network
     * @param operationInvocationReceivers callback interceptors for incoming set service requests.
     * @param plugins                      {@link org.somda.sdc.glue.provider.plugin.SdcRequiredTypesAndScopes} if the
     *                                     collection is empty or custom plugins to run on start up and shut down in the
     *                                     order given by this collection.
     *                                     Make sure plugins are independent to each other.
     *                                     Also make sure that at least the
     *                                     {@link org.somda.sdc.glue.provider.plugin.SdcRequiredTypesAndScopes}
     *                                     or a functionally equivalent plugin is loaded.
     *                                     See {@link SdcDevicePlugin} for more details.
     * @return a new {@link SdcDevice}.
     * Use {@link SdcDevice#startAsync()} in order to start exposing the device on the network.
     */
    Provider create(@Assisted String eprAddress,
                    @Assisted ProviderSettings providerSettings);
}
