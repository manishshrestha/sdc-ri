package org.ieee11073.sdc.glue.provider;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.ieee11073.sdc.biceps.provider.access.LocalMdibAccess;
import org.ieee11073.sdc.dpws.device.Device;
import org.ieee11073.sdc.glue.provider.sco.OperationInvocationReceiver;
import org.ieee11073.sdc.glue.provider.services.HighPriorityServices;
import org.ieee11073.sdc.glue.provider.services.factory.ServicesFactory;

import java.util.Collection;

/**
 * SDC provider device interface.
 * <p>
 * The purpose of the {@linkplain SdcDevice} is to provide SDC data on the network.
 */
public class SdcDevice {
    private final Device dpwsDevice;
    private final LocalMdibAccess mdibAccess;
    private final HighPriorityServices highPriorityServices;

    @AssistedInject
    SdcDevice(@Assisted Device dpwsDevice,
              @Assisted LocalMdibAccess mdibAccess,
              @Assisted Collection<OperationInvocationReceiver> operationInvocationReceivers,
              ServicesFactory servicesFactory) {
        this.dpwsDevice = dpwsDevice;
        this.mdibAccess = mdibAccess;
        this.highPriorityServices = servicesFactory.createHighPriorityServices(mdibAccess);

        operationInvocationReceivers.forEach(receiver -> addOperationInvocationReceiver(receiver));

        setupHostedServices();
    }

    private void setupHostedServices() {
        // dpwsDevice.get
    }

    private void addOperationInvocationReceiver(OperationInvocationReceiver receiver) {
        highPriorityServices.addOperationInvocationReceiver(receiver);
    }
}
