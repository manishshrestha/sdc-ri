package org.somda.sdc.proto.provider;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import io.grpc.BindableService;
import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.participant.AbstractState;
import org.somda.sdc.biceps.model.participant.MdibVersion;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.glue.provider.SdcDevicePlugin;
import org.somda.sdc.glue.provider.sco.OperationInvocationReceiver;
import org.somda.sdc.proto.discovery.provider.TargetService;
import org.somda.sdc.proto.discovery.provider.factory.TargetServiceFactory;
import org.somda.sdc.proto.model.common.LocalizedString;
import org.somda.sdc.proto.model.common.QName;
import org.somda.sdc.proto.model.discovery.DeviceMetadata;
import org.somda.sdc.proto.model.discovery.GetMetadataRequest;
import org.somda.sdc.proto.model.discovery.GetMetadataResponse;
import org.somda.sdc.proto.model.discovery.HostedService;
import org.somda.sdc.proto.model.discovery.MetadataServiceGrpc;
import org.somda.sdc.proto.server.Server;
import org.somda.sdc.proto.server.guice.ServerImplFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class ProviderImpl extends AbstractIdleService implements Provider {
    private static final Logger LOG = LogManager.getLogger();

    private final TargetServiceFactory targetServiceFactory;
    private final Server server;
    private final Logger instanceLogger;
    private final ProviderSettings providerSettings;
    private final String eprAddress;
    private final TargetService targetService;
    private final List<QName> types;
    private final LocalMdibAccess mdibAccess;
    private final Collection<OperationInvocationReceiver> operationInvocationReceivers;
    private final Collection<SdcDevicePlugin> plugins;

    @AssistedInject
    ProviderImpl(@Assisted String eprAddress,
                 @Assisted LocalMdibAccess mdibAccess,
                 @Assisted ProviderSettings providerSettings,
                 @Assisted("operationInvocationReceivers")
                         Collection<OperationInvocationReceiver> operationInvocationReceivers,
                 @Assisted("plugins") Collection<SdcDevicePlugin> plugins,
                 @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier,
                 TargetServiceFactory targetServiceFactory,
                 ServerImplFactory serverFactory,
                 SoapUtil soapUtil
    ) {
        this.eprAddress = eprAddress;
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.mdibAccess = mdibAccess;
        this.operationInvocationReceivers = operationInvocationReceivers;
        this.plugins = plugins;
        this.targetServiceFactory = targetServiceFactory;
        this.providerSettings = providerSettings;
        this.server = serverFactory.create(providerSettings);
        this.targetService = targetServiceFactory.create(this.eprAddress);
        this.types = new ArrayList<>();
    }

    @Override
    protected void startUp() throws Exception {
        instanceLogger.info(
                "Starting gRPC Provider {} at address {} with epr {}",
                providerSettings.getProviderName(),
                providerSettings.getNetworkAddress(),
                eprAddress
        );
        // the metadata service is always required
        server.registerService(new MetadataService(providerSettings, types));
        server.startAsync().awaitRunning();

        var xAddr = String.format("%s:%s", server.getAddress().getHostName(), server.getAddress().getPort());
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
        return eprAddress;
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

    @Override
    public String getEprAddress() {
        return getEpr();
    }

    @Override
    public LocalMdibAccess getLocalMdibAccess() {
        return mdibAccess;
    }

    @Override
    public Collection<OperationInvocationReceiver> getOperationInvocationReceivers() {
        return operationInvocationReceivers;
    }

    @Override
    public State getServiceState() {
        return state();
    }

    @Override
    public <T extends AbstractState> void sendPeriodicStateReport(final List<T> states, final MdibVersion mdibVersion) {
        // TODO
    }

    @Override
    public TargetService getTargetService() {
        return targetService;
    }

    static class MetadataService extends MetadataServiceGrpc.MetadataServiceImplBase {

        private final ProviderSettings providerSettings;
        private final List<QName> types;

        MetadataService(ProviderSettings providerSettings, List<QName> types) {
            this.providerSettings = providerSettings;
            this.types = types;
        }

        @Override
        public void getMetadata(GetMetadataRequest request,
                                StreamObserver<GetMetadataResponse> responseObserver) {
            var response = GetMetadataResponse.newBuilder();
            var metadata = DeviceMetadata.newBuilder()
                    .setFriendlyName(
                            LocalizedString.newBuilder()
                                    .setValue(providerSettings.getProviderName())
                                    .setLocale("en_US")
                    );

            types.forEach(type -> {
                response.addHostedService(
                        HostedService.newBuilder()
                                .setType(type)
                                .build()
                );
            });

            response.setMetadata(metadata);
            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        }
    }
}
