package org.somda.sdc.proto.consumer.sco.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.proto.consumer.sco.helper.OperationInvocationDispatcher;

public interface OperationInvocationDispatcherFactory {
    OperationInvocationDispatcher createOperationInvocationDispatcher(
            @Assisted HostingServiceProxy hostingServiceProxy);
}
