package org.somda.sdc.proto.consumer;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.grpc.Channel;
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
import org.somda.protosdc.proto.model.GetServiceGrpc;
import org.somda.protosdc.proto.model.MdibReportingServiceGrpc;
import org.somda.protosdc.proto.model.SetServiceGrpc;
import org.somda.protosdc.proto.model.addressing.EndpointReference;
import org.somda.protosdc.proto.model.discovery.DeviceMetadata;
import org.somda.protosdc.proto.model.discovery.Endpoint;
import org.somda.protosdc.proto.model.discovery.GetMetadataRequest;
import org.somda.protosdc.proto.model.discovery.MetadataServiceGrpc;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Optional;

public class ConsumerImpl implements Consumer {
    private static final Logger LOG = LogManager.getLogger(ConsumerImpl.class);
    private final Logger instanceLogger;
    private ManagedChannel channel;
    private final CryptoSettings cryptoSettings;
    private MetadataServiceGrpc.MetadataServiceBlockingStub metadataStub;
    private GetServiceGrpc.GetServiceBlockingStub getServiceStub;
    private SetServiceGrpc.SetServiceStub nonBlockingSetServiceStub;
    private SetServiceGrpc.SetServiceBlockingStub blockingSetServiceStub;
    private DeviceMetadata metadata;
    private EndpointReference epr;
    private MdibReportingServiceGrpc.MdibReportingServiceStub reportingServiceStub;

    @Inject
    ConsumerImpl(@Nullable @Named(CryptoConfig.CRYPTO_SETTINGS) CryptoSettings cryptoSettings,
                 @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.cryptoSettings = cryptoSettings;
        this.metadataStub = null;

        this.getServiceStub = null;
        this.nonBlockingSetServiceStub = null;
        this.blockingSetServiceStub = null;
        this.reportingServiceStub = null;
    }

    @Override
    public void connect(final Endpoint endpoint) throws IOException {
        if (channel != null) {
            throw new IllegalStateException("Consumer is already connected to a server");
        }
        // TODO: Handle multiple addresses
        var address = URI.create(endpoint.getXAddrList().stream().findFirst()
                .orElseThrow(() -> new IOException("Endpoint does not provide any xAddr")));

        instanceLogger.info("connecting to {} on address {}", endpoint.getEndpointReference(), address);

        var host = new InetSocketAddress(address.getHost(), address.getPort());
        var channelBuilder = NettyChannelBuilder.forAddress(host);
        if (cryptoSettings != null) {
            channelBuilder.sslContext(GrpcSslContexts.forClient()
                    .keyManager(CryptoUtil.loadKeyStore(cryptoSettings)
                            .orElseThrow(() -> new IOException("Could not load keystore")))
                    .trustManager(CryptoUtil.loadTrustStore(cryptoSettings)
                            .orElseThrow(() -> new IOException("Could not load truststore")))
                    .build());
        } else {
            channelBuilder.usePlaintext();
        }
        channel = channelBuilder.build();

        // get metadata to determine available services
        metadataStub = MetadataServiceGrpc.newBlockingStub(channel);
        var getMetadataResponse = metadataStub.getMetadata(GetMetadataRequest.getDefaultInstance());
        this.metadata = getMetadataResponse.getMetadata();
        this.epr = getMetadataResponse.getEndpointReference();
        getMetadataResponse.getHostedServiceList().forEach(hostedService -> {
            var type = hostedService.getType();
            instanceLogger.info("Device provides type {{{}}}{}", type.getNamespace(), type.getLocalName());

            if (ProtoConstants.GET_SERVICE_QNAME.equals(type)) {
                getServiceStub = GetServiceGrpc.newBlockingStub(channel);
            } else if (ProtoConstants.SET_SERVICE_QNAME.equals(type)) {
                nonBlockingSetServiceStub = SetServiceGrpc.newStub(channel);
                blockingSetServiceStub = SetServiceGrpc.newBlockingStub(channel);
            } else if (ProtoConstants.MDIB_REPORTING_SERVICE_QNAME.equals(type)) {
                reportingServiceStub = MdibReportingServiceGrpc.newStub(channel);
            }
        });

        instanceLogger.info("Consumer connected to {}", endpoint.getEndpointReference());
    }

    @Override
    public DeviceMetadata getMetadata() {
        return metadata;
    }

    @Override
    public String getEprAddress() {
        return epr.getAddress();
    }

    @Override
    public Optional<GetServiceGrpc.GetServiceBlockingStub> getGetService() {
        return Optional.ofNullable(getServiceStub);
    }

    @Override
    public Optional<SetServiceGrpc.SetServiceStub> getNonblockingSetService() {
        return Optional.ofNullable(nonBlockingSetServiceStub);
    }

    @Override
    public Optional<SetServiceGrpc.SetServiceBlockingStub> getBlockingSetService() {
        return Optional.ofNullable(blockingSetServiceStub);
    }

    public Optional<MdibReportingServiceGrpc.MdibReportingServiceStub> getMdibReportingService() {
        return Optional.of(reportingServiceStub);
    }

    @Override
    public Optional<Channel> getChannel() {
        return Optional.ofNullable(channel);
    }

    @Override
    public void disconnect() {
        if (channel != null) {
            channel.shutdown();
            channel = null;

            metadataStub = null;
            getServiceStub = null;
            nonBlockingSetServiceStub = null;
        }
    }
}
