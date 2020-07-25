package org.somda.sdc.proto.consumer;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.crypto.CryptoConfig;
import org.somda.sdc.dpws.crypto.CryptoSettings;
import org.somda.sdc.proto.common.ProtoConstants;
import org.somda.sdc.proto.crypto.CryptoUtil;
import org.somda.sdc.proto.model.GetServiceGrpc;
import org.somda.sdc.proto.model.SetServiceGrpc;
import org.somda.sdc.proto.model.discovery.DiscoveryMessages;
import org.somda.sdc.proto.model.discovery.DiscoveryTypes;
import org.somda.sdc.proto.model.discovery.MetadataServiceGrpc;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

public class ConsumerImpl implements Consumer {
    private static final Logger LOG = LogManager.getLogger(ConsumerImpl.class);
    private final Logger instanceLogger;
    private ManagedChannel channel;
    private final CryptoSettings cryptoSettings;
    private MetadataServiceGrpc.MetadataServiceBlockingStub metadataStub;
    private GetServiceGrpc.GetServiceBlockingStub getServiceStub;
    private SetServiceGrpc.SetServiceBlockingStub setServiceStub;

    @Inject
    ConsumerImpl(
            @Nullable @Named(CryptoConfig.CRYPTO_SETTINGS) CryptoSettings cryptoSettings,
            @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier
    ) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.cryptoSettings = cryptoSettings;
        this.metadataStub = null;

        this.getServiceStub = null;
        this.setServiceStub = null;
    }

    @Override
    public void connect(final DiscoveryTypes.Endpoint endpoint) throws IOException {
        var address = endpoint.getXAddrList().stream().findFirst()
                .orElseThrow(() -> new IOException("Endpoint does not provide any xAddr"));

        instanceLogger.info("connecting to {} on address {}", endpoint.getEndpointReference(), address);

        // odd workaround to determine host and port
        URI uri;
        try {
            uri = new URI("my://" + address);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
        var host = new InetSocketAddress(uri.getHost(), uri.getPort());
        var channelBuilder = NettyChannelBuilder.forAddress(host);
        if (cryptoSettings != null) {
            channelBuilder.sslContext(GrpcSslContexts.forClient()
                    .keyManager(CryptoUtil.loadKeyStore(cryptoSettings).get())
                    .trustManager(CryptoUtil.loadTrustStore(cryptoSettings).get())
                    .build());
        } else {
            channelBuilder.usePlaintext();
        }
        channel = channelBuilder.build();

        // get metadata to determine available services
        metadataStub = MetadataServiceGrpc.newBlockingStub(channel);
        var metadata = metadataStub.getMetadata(DiscoveryMessages.GetMetadataRequest.getDefaultInstance());

        metadata.getHostedServiceList().forEach(hostedService -> {
            var type = hostedService.getType();
            instanceLogger.info("Device provides type\n{}", type);

            if (ProtoConstants.GET_SERVICE_QNAME.equals(type)) {
                getServiceStub = GetServiceGrpc.newBlockingStub(channel);
            } else if (ProtoConstants.SET_SERVICE_QNAME.equals(type)) {
                setServiceStub = SetServiceGrpc.newBlockingStub(channel);
            }
        });

        instanceLogger.info("Consumer connected to {}", endpoint.getEndpointReference());
    }

    @Override
    public Optional<GetServiceGrpc.GetServiceBlockingStub> getGetService() {
        return Optional.ofNullable(getServiceStub);
    }

    @Override
    public Optional<SetServiceGrpc.SetServiceBlockingStub> getSetService() {
        return Optional.ofNullable(setServiceStub);
    }

    @Override
    public void disconnect() {
        channel.shutdown();
        channel = null;

        metadataStub = null;
        getServiceStub = null;
        setServiceStub = null;
    }
}
