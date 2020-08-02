package org.somda.sdc.proto.provider.sco.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.proto.provider.sco.OperationInvokedEventSource;
import org.somda.sdc.proto.provider.sco.ScoController;
import org.somda.sdc.proto.provider.sco.SetService;

public interface SetServiceFactory {
    SetService createSetService(@Assisted ScoController scoController,
                                @Assisted OperationInvokedEventSource eventSource);
}
