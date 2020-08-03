package it.org.somda.sdc.proto.provider;

import com.google.inject.Injector;
import com.google.protobuf.UInt64Value;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import it.org.somda.sdc.dpws.soap.Ssl;
import it.org.somda.sdc.proto.IntegrationTestUtil;
import it.org.somda.sdc.proto.VentilatorMdibRunner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.provider.access.factory.LocalMdibAccessFactory;
import org.somda.sdc.common.guice.AbstractConfigurationModule;
import org.somda.sdc.dpws.crypto.CryptoConfig;
import org.somda.sdc.dpws.crypto.CryptoSettings;
import org.somda.sdc.proto.crypto.CryptoUtil;
import org.somda.sdc.proto.model.GetMdStateRequest;
import org.somda.sdc.proto.model.GetMdStateResponse;
import org.somda.sdc.proto.model.GetMdibRequest;
import org.somda.sdc.proto.model.GetMdibResponse;
import org.somda.sdc.proto.model.GetServiceGrpc;
import org.somda.sdc.proto.model.discovery.GetMetadataRequest;
import org.somda.sdc.proto.model.discovery.GetMetadataResponse;
import org.somda.sdc.proto.model.discovery.MetadataServiceGrpc;
import org.somda.sdc.proto.provider.factory.SdcDeviceFactory;
import test.org.somda.common.LoggingTestWatcher;

import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(LoggingTestWatcher.class)
public class SdcDeviceImplIT {
    private static final Logger LOG = LogManager.getLogger(ProviderImplIT.class);
    private static final String PROVIDER_NAME = "Ṱ̺̺̕o͞ ̷i̲̬͇̪͙n̝̗͕v̟̜̘̦͟o̶̙̰̠kè͚̮̺̪̹̱̤ ̖t̝͕̳̣̻̪͞h̼͓̲̦̳̘̲e͇̣̰̦̬͎ ̢̼̻̱̘h͚͎͙̜̣̲ͅi̦̲̣̰̤v̻͍e̺̭̳̪̰-m̢iͅn̖̺̞̲̯̰d̵̼̟͙̩̼̘̳ ̞̥̱̳̭r̛̗̘e͙p͠r̼̞̻̭̗e̺̠̣͟s̘͇̳͍̝͉e͉̥̯̞̲͚̬͜ǹ̬͎͎̟̖͇̤t͍̬̤͓̼̭͘ͅi̪̱n͠g̴͉ ͏͉ͅc̬̟h͡a̫̻̯͘o̫̟̖͍̙̝͉s̗̦̲.̨̹͈̣";
    private Injector providerInjector;
    private CryptoSettings clientCrypto;
    private SdcDeviceFactory sdcDeviceFactory;
    private LocalMdibAccess mdibAccess;
    private VentilatorMdibRunner ventilatorMdibRunner;

    @BeforeEach
    void setUp() {
        this.providerInjector = new IntegrationTestUtil(
                new AbstractConfigurationModule() {
                    @Override
                    protected void defaultConfigure() {
                        bind(CryptoConfig.CRYPTO_SETTINGS,
                                CryptoSettings.class,
                                Ssl.setupServer());
                    }
                }
        ).getInjector();
        this.sdcDeviceFactory = providerInjector.getInstance(SdcDeviceFactory.class);
        this.mdibAccess = providerInjector.getInstance(LocalMdibAccessFactory.class).createLocalMdibAccess();
        this.ventilatorMdibRunner = providerInjector.getInstance(VentilatorMdibRunner.class);

        this.clientCrypto = Ssl.setupClient();
    }

    @Test
    void testDevice() throws SocketException, SSLException {
        var serverAddr = new InetSocketAddress("127.0.0.1", 13373);
        var epr = "urn:uuid:" + UUID.randomUUID().toString();

        var provider = sdcDeviceFactory.createSdcDevice(
                epr, serverAddr, mdibAccess, Collections.emptyList(),
                List.of(ventilatorMdibRunner));
        provider.startAsync().awaitRunning();

        System.out.println("UP");

        var client = new MetadataClient(serverAddr, clientCrypto);

        var metadataResponse = client.getMetadata();
        var providerName = "SdcDevice " + epr;
        assertEquals(providerName, metadataResponse.getMetadata().getFriendlyName().getValue());

        var getClient = new GetServiceClient(serverAddr, clientCrypto);
        var mdib = getClient.getMdib();

        LOG.info("Mdib response was {}", mdib);
        assertNotNull(mdib);
        var mdibVersionGroup = mdib.getPayload().getMdib().getAMdibVersionGroup();
        LOG.info("MdibVersionGroup is {}", mdibVersionGroup);
        assertTrue(mdibVersionGroup.hasAMdibVersion());

        var mdState = getClient.getMdState();
        LOG.info("mdState response was {}", mdib);
        assertNotNull(mdState);
        provider.stopAsync().awaitTerminated();
    }

    static class MetadataClient {
        private final ManagedChannel channel;
        private final MetadataServiceGrpc.MetadataServiceBlockingStub blockingStub;

        MetadataClient(InetSocketAddress host, CryptoSettings cryptoSettings) throws SSLException {
            this(NettyChannelBuilder.forAddress(host)
                    .sslContext(GrpcSslContexts.forClient()
                            .keyManager(CryptoUtil.loadKeyStore(cryptoSettings).get())
                            .trustManager(CryptoUtil.loadTrustStore(cryptoSettings).get())
                            .build())
                    .build());
        }

        MetadataClient(ManagedChannel channel) {
            this.channel = channel;
            blockingStub = MetadataServiceGrpc.newBlockingStub(channel);
        }

        GetMetadataResponse getMetadata() {
            return blockingStub.getMetadata(GetMetadataRequest.newBuilder().build());
        }
    }

    static class GetServiceClient {
        private final ManagedChannel channel;
        private final GetServiceGrpc.GetServiceBlockingStub blockingStub;

        GetServiceClient(InetSocketAddress host, CryptoSettings cryptoSettings) throws SSLException {
            this(NettyChannelBuilder.forAddress(host)
                    .sslContext(GrpcSslContexts.forClient()
                            .keyManager(CryptoUtil.loadKeyStore(cryptoSettings).get())
                            .trustManager(CryptoUtil.loadTrustStore(cryptoSettings).get())
                            .build())
                    .build());
        }

        GetServiceClient(ManagedChannel channel) {
            this.channel = channel;
            blockingStub = GetServiceGrpc.newBlockingStub(channel);
        }

        GetMdibResponse getMdib() {
            return blockingStub.getMdib(GetMdibRequest.newBuilder().build());
        }

        GetMdStateResponse getMdState() {
            return blockingStub.getMdState(GetMdStateRequest.newBuilder().build());
        }
    }
}
