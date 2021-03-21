package org.somda.sdc.proto.consumer;

import io.grpc.Channel;
import org.somda.protosdc.proto.model.GetServiceGrpc;
import org.somda.protosdc.proto.model.MdibReportingServiceGrpc;
import org.somda.protosdc.proto.model.SetServiceGrpc;
import org.somda.protosdc.proto.model.discovery.DeviceMetadata;
import org.somda.protosdc.proto.model.discovery.DiscoveryTypes;
import org.somda.protosdc.proto.model.discovery.Endpoint;

import java.io.IOException;
import java.util.Optional;

public interface Consumer {

    /**
     * Connect to the Provider at the given endpoint.
     *
     * @param endpoint to connect to
     * @throws IOException           on connection errors
     * @throws IllegalStateException if consumer is already connected
     */
    void connect(Endpoint endpoint) throws IOException;

    /**
     * Disconnect from the currently connected provider.
     */
    void disconnect();

    /**
     * @return get service stub if connected
     */
    Optional<GetServiceGrpc.GetServiceBlockingStub> getGetService();

    /**
     * @return set service stub if connected
     */
    Optional<SetServiceGrpc.SetServiceStub> getNonblockingSetService();

    /**
     * @return set service stub if connected
     */
    Optional<SetServiceGrpc.SetServiceBlockingStub> getBlockingSetService();

    Optional<MdibReportingServiceGrpc.MdibReportingServiceStub> getMdibReportingService();

    /**
     * @return channel to current provider if connected
     */
    Optional<Channel> getChannel();

    DeviceMetadata getMetadata();

    String getEprAddress();
}
