package org.somda.sdc.proto.provider;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.dpws.device.DeviceSettings;
import org.somda.sdc.glue.provider.SdcDevicePlugin;
import org.somda.sdc.glue.provider.sco.OperationInvocationReceiver;

import java.util.Collection;

public class SdcDeviceImpl {
    @Inject
    SdcDeviceImpl(@Assisted DeviceSettings deviceSettings,
                  @Assisted LocalMdibAccess mdibAccess,
                  @Assisted("operationInvocationReceivers")
                          Collection<OperationInvocationReceiver> operationInvocationReceivers,
                  @Assisted("plugins") Collection<SdcDevicePlugin> plugins
                  ) {
    }
}
