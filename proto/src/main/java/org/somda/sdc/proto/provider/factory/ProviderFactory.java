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
     * @return a new {@link Provider}.
     * Use {@link Provider#startAsync()} in order to start exposing the device on the network.
     */
    Provider create(@Assisted String eprAddress,
                    @Assisted ProviderSettings providerSettings);
}
