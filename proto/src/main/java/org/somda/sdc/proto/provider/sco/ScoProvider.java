package org.somda.sdc.proto.provider.sco;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.proto.common.ProtoConstants;
import org.somda.sdc.proto.model.common.QName;
import org.somda.sdc.proto.provider.sco.factory.ScoControllerFactory;
import org.somda.sdc.proto.provider.sco.factory.SetServiceFactory;

public class ScoProvider implements BindableService {
    private final ScoController scoController;
    private final SetService setService;

    @Inject
    ScoProvider(@Assisted LocalMdibAccess localMdibAccess,
                ScoControllerFactory scoControllerFactory,
                SetServiceFactory setServiceFactory,
                OperationInvokedEventSource operationInvokedEventSource) {
        this.scoController = scoControllerFactory.createScoController(operationInvokedEventSource,
                localMdibAccess);
        this.setService = setServiceFactory.createSetService(scoController, operationInvokedEventSource);
    }

    public ScoController getScoController() {
        return scoController;
    }

    public QName getServiceType() {
        return ProtoConstants.SET_SERVICE_QNAME;
    }

    @Override
    public ServerServiceDefinition bindService() {
        return setService.bindService();
    }
}
