package org.somda.sdc.proto.consumer;

import org.somda.sdc.proto.discovery.provider.TargetService;
import org.somda.sdc.proto.model.GetServiceGrpc;
import org.somda.sdc.proto.model.SetServiceGrpc;
import org.somda.sdc.proto.model.discovery.DiscoveryTypes;

import java.io.IOException;
import java.util.Optional;

public interface Consumer {


    void connect(DiscoveryTypes.Endpoint endpoint) throws IOException;

    void disconnect();

    Optional<GetServiceGrpc.GetServiceBlockingStub> getGetService();

    Optional<SetServiceGrpc.SetServiceBlockingStub> getSetService();
}
