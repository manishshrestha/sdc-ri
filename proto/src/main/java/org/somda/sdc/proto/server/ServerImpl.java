package org.somda.sdc.proto.server;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;
import io.grpc.BindableService;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.ClientAuth;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.crypto.CryptoConfig;
import org.somda.sdc.dpws.crypto.CryptoSettings;
import org.somda.sdc.proto.crypto.CryptoUtil;
import org.somda.sdc.proto.provider.ProviderSettings;
import org.somda.sdc.proto.guice.GrpcConfig;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class ServerImpl extends AbstractIdleService implements Server {
    public static Logger LOG = LogManager.getLogger(ServerImpl.class);
    private final Logger instanceLogger;
    private final boolean insecure;
    private final List<BindableService> services;
    private final CryptoSettings cryptoSettings;
    private final ProviderSettings providerSettings;
    private io.grpc.Server server;

    @Inject
    ServerImpl(
        @Assisted ProviderSettings providerSettings,
        @Nullable @Named(CryptoConfig.CRYPTO_SETTINGS) CryptoSettings cryptoSettings,
        @Named(GrpcConfig.GRPC_SERVER_INSECURE) boolean insecure,
        @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier
    ) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.insecure = insecure;
        this.cryptoSettings = cryptoSettings;
        this.services = new ArrayList<>();
        this.providerSettings = providerSettings;
    }

    @Override
    protected void startUp() throws Exception {
        var serverBuilder = NettyServerBuilder.forAddress(providerSettings.getNetworkAddress());
        if (!insecure) {
            // load crypto stuff
            var keyStoreManagerFactory = CryptoUtil.loadKeyStore(cryptoSettings);
            var trustStoreManagerFactory = CryptoUtil.loadTrustStore(cryptoSettings);
            if (keyStoreManagerFactory.isEmpty() || trustStoreManagerFactory.isEmpty()) {
                instanceLogger.error("Either keystore or truststore is empty");
                throw new Exception("Could not load crypto");
            }

            serverBuilder.sslContext(GrpcSslContexts.configure(
                SslContextBuilder.forServer(keyStoreManagerFactory.get())
                    .trustManager(trustStoreManagerFactory.get())
                )
                    .clientAuth(ClientAuth.REQUIRE)
                    .build()
            );
        }
        services.forEach(serverBuilder::addService);
        server = serverBuilder.build().start();
    }

    @Override
    protected void shutDown() throws Exception {
        if (server != null) {
            server.shutdown().awaitTermination();
        }
    }

    @Override
    public InetSocketAddress getAddress() {
        if (!isRunning()) {
            throw new IllegalStateException("Server not yet running.");
        }
        var address = providerSettings.getNetworkAddress().getHostName();
        var port = server.getPort();
        return new InetSocketAddress(address, port);
    }

    @Override
    public void registerService(final BindableService service) {
        if (isRunning()) {
            throw new IllegalStateException("Adding services during runtime is unsupported");
        }
        instanceLogger.info("Adding service {} to gRPC server", service.getClass().getSimpleName());
        services.add(service);
    }
}
