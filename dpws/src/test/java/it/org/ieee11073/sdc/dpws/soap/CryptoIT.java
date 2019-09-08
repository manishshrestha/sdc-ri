package it.org.ieee11073.sdc.dpws.soap;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import dpws_test_service.messages._2017._05._10.ObjectFactory;
import dpws_test_service.messages._2017._05._10.TestNotification;
import it.org.ieee11073.sdc.dpws.IntegrationTestUtil;
import it.org.ieee11073.sdc.dpws.TestServiceMetadata;
import org.ieee11073.sdc.dpws.client.DiscoveredDevice;
import org.ieee11073.sdc.dpws.crypto.CryptoConfig;
import org.ieee11073.sdc.dpws.crypto.CryptoSettings;
import org.ieee11073.sdc.dpws.device.DeviceSettings;
import org.ieee11073.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.ieee11073.sdc.dpws.service.HostedServiceProxy;
import org.ieee11073.sdc.dpws.service.HostingServiceProxy;
import org.ieee11073.sdc.dpws.soap.SoapConfig;
import org.ieee11073.sdc.dpws.soap.SoapUtil;
import org.ieee11073.sdc.dpws.soap.interception.Interceptor;
import org.ieee11073.sdc.dpws.soap.interception.MessageInterceptor;
import org.ieee11073.sdc.dpws.soap.interception.NotificationObject;
import org.ieee11073.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.ieee11073.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.WsDiscoveryConfig;
import org.ieee11073.sdc.dpws.soap.wseventing.SubscribeResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import test.org.ieee11073.common.TestLogging;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class CryptoIT {
    private static final Duration MAX_WAIT_TIME = Duration.ofMinutes(3);

    private final IntegrationTestUtil IT = new IntegrationTestUtil();
    private final SoapUtil soapUtil = IT.getInjector().getInstance(SoapUtil.class);
    private final WsAddressingUtil wsaUtil = IT.getInjector().getInstance(WsAddressingUtil.class);

    private BasicPopulatedDevice devicePeer;
    private ClientPeer clientPeer;

    @Before
    public void setUp() {
        TestLogging.configure();
        final CryptoSettings serverCryptoSettings = Ssl.setupServer();

        this.devicePeer = new BasicPopulatedDevice(new DeviceSettings() {
            final EndpointReferenceType epr = wsaUtil.createEprWithAddress(soapUtil.createUriFromUuid(UUID.randomUUID()));

            @Override
            public EndpointReferenceType getEndpointReference() {
                return epr;
            }

            @Override
            public List<URI> getHostingServiceBindings() {
                return Collections.singletonList(URI.create("https://localhost:6464"));
            }
        }, new DefaultDpwsConfigModule() {
            @Override
            public void customConfigure() {
                bind(CryptoConfig.CRYPTO_SETTINGS, CryptoSettings.class, serverCryptoSettings);
            }
        });

        final CryptoSettings clientCryptoSettings = Ssl.setupClient();
        try {
            this.clientPeer = new ClientPeer(new DefaultDpwsConfigModule() {
                @Override
                public void customConfigure() {
                    bind(WsDiscoveryConfig.MAX_WAIT_FOR_PROBE_MATCHES, Duration.class,
                            Duration.ofSeconds(MAX_WAIT_TIME.getSeconds() / 2));
                    bind(CryptoConfig.CRYPTO_SETTINGS, CryptoSettings.class, clientCryptoSettings);
                    bind(SoapConfig.JAXB_CONTEXT_PATH, String.class,
                            TestServiceMetadata.JAXB_CONTEXT_PATH);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @After
    public void tearDown() {
        this.devicePeer.stopAsync().awaitTerminated();
        this.clientPeer.stopAsync().awaitTerminated();
    }

    @Test
    public void directedProbeSecured() throws Exception {
        // Given a device under test (DUT) and a client up and running
        devicePeer.startAsync().awaitRunning();
        clientPeer.startAsync().awaitRunning();

        // When the DUT's physical addresses are resolved
        final DiscoveredDevice discoveredDevice = clientPeer.getClient().resolve(devicePeer.getEprAddress())
                .get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        final List<String> xAddrs = discoveredDevice.getXAddrs();
        assertFalse(xAddrs.isEmpty());
        final URI uri = URI.create(xAddrs.get(0));

        // Then expect the EPR address returned by a directed probe to be the DUT's EPR address
        final String expectedEprAddress = devicePeer.getEprAddress().toString();

        assertEquals(expectedEprAddress, clientPeer.getClient().directedProbe(uri)
                .get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS)
                .getProbeMatch().get(0).getEndpointReference().getAddress().getValue());
    }

    @Test
    public void notificationSecured() throws Exception {
        // Given a device under test (DUT) and a client up and running
        devicePeer.startAsync().awaitRunning();
        clientPeer.startAsync().awaitRunning();
        final HostingServiceProxy hostingServiceProxy = clientPeer.getClient().connect(devicePeer.getEprAddress())
                .get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        final int COUNT = 100;
        final SettableFuture<List<TestNotification>> notificationFuture = SettableFuture.create();
        final HostedServiceProxy srv1 = hostingServiceProxy.getHostedServices().get(BasicPopulatedDevice.SERVICE_ID_1);
        final ListenableFuture<SubscribeResult> subscribe = srv1.getEventSinkAccess()
                .subscribe(Collections.singletonList(TestServiceMetadata.ACTION_NOTIFICATION_1), Duration.ofMinutes(1),
                        new Interceptor() {
                            private final List<TestNotification> receivedNotifications = new ArrayList<>();

                            @MessageInterceptor(value = TestServiceMetadata.ACTION_NOTIFICATION_1)
                            void onNotification(NotificationObject message) {
                                receivedNotifications.add(
                                        soapUtil.getBody(message.getNotification(), TestNotification.class)
                                                .orElseThrow(() -> new RuntimeException("TestNotification could not be converted")));
                                if (receivedNotifications.size() == COUNT) {
                                    notificationFuture.set(receivedNotifications);
                                }
                            }
                        });

        subscribe.get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);

        final ObjectFactory factory = IT.getInjector().getInstance(ObjectFactory.class);
        for (int i = 0; i < COUNT; ++i) {
            final TestNotification testNotification = factory.createTestNotification();
            testNotification.setParam1(Integer.toString(i));
            testNotification.setParam2(i);
            devicePeer.getService1().sendNotification(testNotification);
        }

        final List<TestNotification> notifications = notificationFuture.get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        assertEquals(COUNT, notifications.size());
        for (int i = 0; i < COUNT; ++i) {
            final TestNotification notification = notifications.get(i);
            assertEquals(Integer.toString(i), notification.getParam1());
            assertEquals(i, notification.getParam2());
        }
    }
}
