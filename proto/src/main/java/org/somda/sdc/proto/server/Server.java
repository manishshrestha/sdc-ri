package org.somda.sdc.proto.server;

import com.google.common.util.concurrent.Service;
import io.grpc.BindableService;

import java.net.InetSocketAddress;

@SuppressWarnings("UnstableApiUsage")
public interface Server extends Service {

    /**
     * Registers a service, only allowed before startup.
     *
     * @param service to register with the server.
     * @throws IllegalStateException if registering a service on running server
     */
    void registerService(BindableService service);

    /**
     * Retrieves the address the server is running on
     * @return an address
     * @throws IllegalStateException if the server isn't running and therefore has no address
     */
    InetSocketAddress getAddress();
}
