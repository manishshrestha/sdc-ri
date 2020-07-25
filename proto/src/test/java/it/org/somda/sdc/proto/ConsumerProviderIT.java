package it.org.somda.sdc.proto;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Injector;
import io.grpc.stub.StreamObserver;
import it.org.somda.sdc.dpws.soap.Ssl;
import it.org.somda.sdc.proto.provider.ProviderImplIT;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.common.guice.AbstractConfigurationModule;
import org.somda.sdc.dpws.DpwsFramework;
import org.somda.sdc.dpws.crypto.CryptoConfig;
import org.somda.sdc.dpws.crypto.CryptoSettings;
import org.somda.sdc.proto.consumer.Consumer;
import org.somda.sdc.proto.common.ProtoConstants;
import org.somda.sdc.proto.discovery.consumer.Client;
import org.somda.sdc.proto.discovery.consumer.event.DeviceEnteredMessage;
import org.somda.sdc.proto.discovery.consumer.event.ProbedDeviceFoundMessage;
import org.somda.sdc.proto.model.GetServiceGrpc;
import org.somda.sdc.proto.model.SdcMessages;
import org.somda.sdc.proto.model.discovery.DiscoveryTypes;
import org.somda.sdc.proto.provider.ProviderSettings;
import org.somda.sdc.proto.provider.guice.ProviderImplFactory;
import test.org.somda.common.LoggingTestWatcher;
import test.org.somda.common.TimedWait;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("UnstableApiUsage")
@ExtendWith(LoggingTestWatcher.class)
public class ConsumerProviderIT {
    private static final Logger LOG = LogManager.getLogger(ProviderImplIT.class);
    private static final String PROVIDER_NAME = "Ṱ̺̺̕o͞ ̷i̲̬͇̪͙n̝̗͕v̟̜̘̦͟o̶̙̰̠kè͚̮̺̪̹̱̤ ̖t̝͕̳̣̻̪͞h̼͓̲̦̳̘̲e͇̣̰̦̬͎ ̢̼̻̱̘h͚͎͙̜̣̲ͅi̦̲̣̰̤v̻͍e̺̭̳̪̰-m̢iͅn̖̺̞̲̯̰d̵̼̟͙̩̼̘̳ ̞̥̱̳̭r̛̗̘e͙p͠r̼̞̻̭̗e̺̠̣͟s̘͇̳͍̝͉e͉̥̯̞̲͚̬͜ǹ̬͎͎̟̖͇̤t͍̬̤͓̼̭͘ͅi̪̱n͠g̴͉ ͏͉ͅc̬̟h͡a̫̻̯͘o̫̟̖͍̙̝͉s̗̦̲.̨̹͈̣";
    private Injector providerInjector;
    private DiscoveryObserver observer;
    private Client discoveryClient;
    private DpwsFramework dpwsFramework;
    private Injector consumerInjector;
    private DpwsFramework consumerDpwsFramework;


    @BeforeEach
    void setUp() throws SocketException {
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
        this.consumerInjector = new IntegrationTestUtil(
                new AbstractConfigurationModule() {
                    @Override
                    protected void defaultConfigure() {
                        bind(CryptoConfig.CRYPTO_SETTINGS,
                                CryptoSettings.class,
                                Ssl.setupClient());
                    }
                }
        ).getInjector();

        this.observer = new DiscoveryObserver();
        this.discoveryClient = providerInjector.getInstance(Client.class);
        this.discoveryClient.startAsync().awaitRunning();

        this.dpwsFramework = providerInjector.getInstance(DpwsFramework.class);
        dpwsFramework.setNetworkInterface(NetworkInterface.getByInetAddress(InetAddress.getLoopbackAddress()));
        dpwsFramework.startAsync().awaitRunning();

        this.consumerDpwsFramework = consumerInjector.getInstance(DpwsFramework.class);
        consumerDpwsFramework.setNetworkInterface(NetworkInterface.getByInetAddress(InetAddress.getLoopbackAddress()));
        consumerDpwsFramework.startAsync().awaitRunning();
    }

    @AfterEach
    void tearDown() {
        discoveryClient.stopAsync().awaitTerminated();
        dpwsFramework.stopAsync().awaitTerminated();
        consumerDpwsFramework.stopAsync().awaitTerminated();
    }

    @Test
    @DisplayName("Probe, connect, GetMdib, disconnect.")
    void testBasicExchange() throws Exception {
        var providerFactory = providerInjector.getInstance(ProviderImplFactory.class);
        var serverAddr = new InetSocketAddress("127.0.0.1", 0);
        var providerSettings = ProviderSettings.builder()
                .setNetworkAddress(serverAddr)
                .setProviderName(PROVIDER_NAME)
                .build();

        var getService = new GetService();
        var provider = providerFactory.create(providerSettings);
        provider.addService(ProtoConstants.GET_SERVICE_QNAME, getService);
        provider.startAsync().awaitRunning();

        discoveryClient.registerObserver(observer);
        var probeResponse = discoveryClient.probe(DiscoveryTypes.ScopeMatcher.newBuilder().build(), 1).get();
        discoveryClient.unregisterObserver(observer);
        assertFalse(probeResponse.isEmpty());

        var consumer = consumerInjector.getInstance(Consumer.class);
        consumer.connect(probeResponse.get(0));
        var consumerGetService = consumer.getGetService().orElseThrow(() -> new Exception("No get service"));
        var getMdibResponse = consumerGetService.getMdib(SdcMessages.GetMdibRequest.getDefaultInstance());
        LOG.debug("getMdibResponse {}", getMdibResponse);
        assertTrue(consumer.getSetService().isEmpty(), "SetService stub should not be present.");

        consumer.disconnect();
        provider.stopAsync().awaitTerminated();

        assertTrue(getService.getMdibCalled, "GetMdib was not called by consumer");
    }

    static class GetService extends GetServiceGrpc.GetServiceImplBase {

        private boolean getMdibCalled;

        GetService() {
            this.getMdibCalled = false;
        }

        @Override
        public void getMdib(final SdcMessages.GetMdibRequest request, final StreamObserver<SdcMessages.GetMdibResponse> responseObserver) {
            getMdibCalled = true;
            responseObserver.onNext(SdcMessages.GetMdibResponse.getDefaultInstance());
            responseObserver.onCompleted();
        }
    }

    private static class DiscoveryObserver implements org.somda.sdc.proto.discovery.consumer.DiscoveryObserver {
        TimedWait<List<DiscoveryTypes.Endpoint>> timedWait = new TimedWait<>(ArrayList::new);

        @Subscribe
        void onEnteredDevice(DeviceEnteredMessage message) {
            timedWait.modifyData(testNotifications -> testNotifications.add(message.getPayload()));
        }

        @Subscribe
        void onEnteredDevice(ProbedDeviceFoundMessage message) {
            timedWait.modifyData(testNotifications -> testNotifications.addAll(message.getPayload()));
        }

        boolean waitForMessages(int messageCount, Duration wait) {
            return timedWait.waitForData(notifications -> notifications.size() >= messageCount, wait);
        }
    }
}
