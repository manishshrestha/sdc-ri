package org.somda.sdc.proto.consumer.sco.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.dpws.service.HostedServiceProxy;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.proto.consumer.sco.ScoController;
import org.somda.sdc.proto.model.SetServiceGrpc;

import javax.annotation.Nullable;

public interface ScoControllerFactory {
    ScoController create(@Assisted SetServiceGrpc.SetServiceBlockingStub setServiceProxy);
}
