package org.somda.sdc.proto.provider;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import io.grpc.BindableService;
import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.proto.discovery.provider.TargetService;
import org.somda.sdc.proto.discovery.provider.factory.TargetServiceFactory;
import org.somda.sdc.proto.model.common.CommonTypes;
import org.somda.sdc.proto.model.service.Metadata;
import org.somda.sdc.proto.model.service.MetadataServiceGrpc;
import org.somda.sdc.proto.server.Server;
import org.somda.sdc.proto.server.guice.ServerImplFactory;

import javax.xml.namespace.QName;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class ProviderImpl extends AbstractIdleService implements Provider {
    private static final Logger LOG = LogManager.getLogger();

    private final TargetServiceFactory targetServiceFactory;
    private final Server server;
    private final Logger instanceLogger;
    private final ProviderSettings providerSettings;
    private final String epr;
    private final TargetService targetService;
    private final List<QName> types;

    @AssistedInject
    ProviderImpl(
        @Assisted ProviderSettings providerSettings,
        TargetServiceFactory targetServiceFactory,
        ServerImplFactory serverFactory,
        SoapUtil soapUtil,
        @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier
    ) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.targetServiceFactory = targetServiceFactory;
        this.providerSettings = providerSettings;
        this.server = serverFactory.create(providerSettings);
        this.epr = soapUtil.createRandomUuidUri();
        this.targetService = targetServiceFactory.create(epr);
        this.types = new ArrayList<>();
    }

    @Override
    protected void startUp() throws Exception {
        instanceLogger.info(
            "Starting gRPC Provider {} at address {} with epr {}",
            providerSettings.getProviderName(),
            providerSettings.getNetworkAddress(),
            epr
        );
        // the metadata service is always required
        server.registerService(new MetadataServicer(providerSettings, types));
        server.startAsync().awaitRunning();

        var xAddr = String.format("%s:%s", providerSettings.getNetworkAddress().getHostName(), providerSettings.getNetworkAddress().getPort());
        targetService.updateXAddrs(List.of(xAddr));
        targetService.startAsync().awaitRunning();
    }

    @Override
    protected void shutDown() throws Exception {
        instanceLogger.info(
            "Stopping gRPC Provider {} at address {}",
            providerSettings.getProviderName(),
            providerSettings.getNetworkAddress()
        );
        targetService.stopAsync().awaitTerminated();
        server.stopAsync().awaitTerminated();
    }

    @Override
    public String getEpr() {
        return epr;
    }

    @Override
    public InetSocketAddress getAddress() {
        return server.getAddress();
    }

    @Override
    public void addService(final QName serviceType, final BindableService service) {
        this.server.registerService(service);
        // since registering throws if unavailable, we can safely add the QName
        this.types.add(serviceType);
    }

    static class MetadataServicer extends MetadataServiceGrpc.MetadataServiceImplBase {

        private final ProviderSettings providerSettings;
        private final List<QName> types;

        MetadataServicer(ProviderSettings providerSettings, List<QName> types) {
            this.providerSettings = providerSettings;
            this.types = types;
        }

        @Override
        public void getMetadata(final Metadata.GetMetadataRequest request, final StreamObserver<Metadata.GetMetadataResponse> responseObserver) {
            var response = Metadata.GetMetadataResponse.newBuilder()
                .setDeviceName(providerSettings.getProviderName());

            types.forEach( type ->
                response.addServices(CommonTypes.QName.newBuilder()
                    .setNamespace(type.getNamespaceURI())
                    .setLocalName(type.getLocalPart())
                    .build()
                ).build()
            );

            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        }
    }
}
