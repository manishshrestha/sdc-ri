package it.org.somda.sdc.proto.provider;

import com.google.inject.Injector;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import it.org.somda.sdc.dpws.soap.Ssl;
import it.org.somda.sdc.proto.IntegrationTestUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.somda.sdc.common.guice.AbstractConfigurationModule;
import org.somda.sdc.dpws.crypto.CryptoConfig;
import org.somda.sdc.dpws.crypto.CryptoSettings;
import org.somda.sdc.proto.crypto.CryptoUtil;
import org.somda.sdc.proto.model.discovery.DiscoveryMessages;
import org.somda.sdc.proto.model.service.MetadataServiceGrpc;
import org.somda.sdc.proto.provider.ProviderSettings;
import org.somda.sdc.proto.provider.guice.ProviderImplFactory;

import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("UnstableApiUsage")
public class ProviderImplIT {
    private static final Logger LOG = LogManager.getLogger(ProviderImplIT.class);
    private static final String PROVIDER_NAME = "Ṱ̺̺̕o͞ ̷i̲̬͇̪͙n̝̗͕v̟̜̘̦͟o̶̙̰̠kè͚̮̺̪̹̱̤ ̖t̝͕̳̣̻̪͞h̼͓̲̦̳̘̲e͇̣̰̦̬͎ ̢̼̻̱̘h͚͎͙̜̣̲ͅi̦̲̣̰̤v̻͍e̺̭̳̪̰-m̢iͅn̖̺̞̲̯̰d̵̼̟͙̩̼̘̳ ̞̥̱̳̭r̛̗̘e͙p͠r̼̞̻̭̗e̺̠̣͟s̘͇̳͍̝͉e͉̥̯̞̲͚̬͜ǹ̬͎͎̟̖͇̤t͍̬̤͓̼̭͘ͅi̪̱n͠g̴͉ ͏͉ͅc̬̟h͡a̫̻̯͘o̫̟̖͍̙̝͉s̗̦̲.̨̹͈̣";
    private Injector providerInjector;
    private Injector consumerInjector;
    private CryptoSettings clientCrypto;


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

        this.clientCrypto = Ssl.setupClient();
    }

    @Test
    void testProvider() throws SSLException {
        var providerFactory = providerInjector.getInstance(ProviderImplFactory.class);
        var serverAddr = new InetSocketAddress("127.0.0.1", 0);
        var providerSettings = ProviderSettings.builder()
            .setNetworkAddress(serverAddr)
            .setProviderName(PROVIDER_NAME)
            .build();

        var provider = providerFactory.create(providerSettings);
        provider.startAsync().awaitRunning();


        var client = new MetadataClient(provider.getAddress(), clientCrypto);

        var metadataResponse = client.getMetadata();

        assertEquals(PROVIDER_NAME, metadataResponse.getMetadata().getFriendlyName().getValue());
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

        DiscoveryMessages.GetHostResponse getMetadata() {
            return blockingStub.getMetadata(DiscoveryMessages.GetHost.newBuilder().build());
        }
    }
}
