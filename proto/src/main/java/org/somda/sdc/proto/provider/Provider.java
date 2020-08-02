package org.somda.sdc.proto.provider;

import com.google.common.util.concurrent.Service;
import io.grpc.BindableService;
import org.somda.sdc.proto.discovery.provider.TargetService;
import org.somda.sdc.proto.model.common.CommonTypes;
import org.somda.sdc.proto.model.common.QName;

import java.net.InetSocketAddress;

@SuppressWarnings("UnstableApiUsage")
public interface Provider extends Service, TargetService {
    /**
     * Add a service to the provider.
     * <p>
     * Services can only be added before startup.
     *
     * @param serviceType of the new service
     * @param service     the new service
     * @throws IllegalStateException if registering a service on running provider
     */
    void addService(QName serviceType, BindableService service);

    /**
     * Retrieves the address the server is running on
     *
     * @return an address
     * @throws IllegalStateException if the server isn't running and therefore has no address
     */
    InetSocketAddress getAddress();
}
