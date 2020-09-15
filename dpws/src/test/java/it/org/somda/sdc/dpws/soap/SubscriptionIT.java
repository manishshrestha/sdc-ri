package it.org.somda.sdc.dpws.soap;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.AbstractModule;
import dpws_test_service.messages._2017._05._10.TestNotification;
import it.org.somda.sdc.dpws.IntegrationTestUtil;
import it.org.somda.sdc.dpws.MockedUdpBindingModule;
import it.org.somda.sdc.dpws.TestServiceMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.CommunicationLogImpl;
import org.somda.sdc.dpws.CommunicationLogSink;
import org.somda.sdc.dpws.DpwsConfig;
import org.somda.sdc.dpws.crypto.CryptoConfig;
import org.somda.sdc.dpws.crypto.CryptoSettings;
import org.somda.sdc.dpws.device.DeviceSettings;
import org.somda.sdc.dpws.factory.TransportBindingFactory;
import org.somda.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.somda.sdc.dpws.service.HostedServiceProxy;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.HttpApplicationInfo;
import org.somda.sdc.dpws.soap.MarshallingService;
import org.somda.sdc.dpws.soap.RequestResponseClient;
import org.somda.sdc.dpws.soap.SoapConfig;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.factory.RequestResponseClientFactory;
import org.somda.sdc.dpws.soap.interception.Interceptor;
import org.somda.sdc.dpws.soap.interception.MessageInterceptor;
import org.somda.sdc.dpws.soap.interception.NotificationObject;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingUtil;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryConfig;
import org.somda.sdc.dpws.soap.wseventing.SubscribeResult;
import org.somda.sdc.dpws.soap.wseventing.SubscriptionManager;
import org.somda.sdc.dpws.soap.wseventing.WsEventingConstants;
import org.somda.sdc.dpws.soap.wseventing.model.GetStatusResponse;
import org.somda.sdc.dpws.soap.wseventing.model.RenewResponse;
import test.org.somda.common.LoggingTestWatcher;

import javax.net.ssl.HostnameVerifier;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(LoggingTestWatcher.class)
public class SubscriptionIT {
    private static final Duration MAX_WAIT_TIME = Duration.ofMinutes(3);

    private final IntegrationTestUtil IT = new IntegrationTestUtil();
    private final SoapUtil soapUtil = IT.getInjector().getInstance(SoapUtil.class);
    private final WsAddressingUtil wsaUtil = IT.getInjector().getInstance(WsAddressingUtil.class);

    private BasicPopulatedDevice devicePeer;
    private ClientPeer clientPeer;
    private HostnameVerifier verifier;
    private TestCommLogSink logSink;
    private MarshallingService marshallingService;

    private TransportBindingFactory transportBindingFactory;
    private RequestResponseClientFactory requestResponseClientFactory;
    private org.somda.sdc.dpws.soap.wseventing.model.ObjectFactory wseFactory;

    SubscriptionIT() {
        IntegrationTestUtil.preferIpV4Usage();
    }

    @BeforeEach
    void setUp() {
        final CryptoSettings serverCryptoSettings = Ssl.setupServer();

        // add custom hostname verifier
        this.verifier = mock(HostnameVerifier.class);
        when(verifier.verify(anyString(), any())).thenReturn(true);

        this.devicePeer = new BasicPopulatedDevice(new DeviceSettings() {
            final EndpointReferenceType epr = wsaUtil.createEprWithAddress(soapUtil.createUriFromUuid(UUID.randomUUID()));

            @Override
            public EndpointReferenceType getEndpointReference() {
                return epr;
            }

            @Override
            public NetworkInterface getNetworkInterface() {
                try {
                    return NetworkInterface.getByInetAddress(InetAddress.getLoopbackAddress());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }, new DefaultDpwsConfigModule() {
            @Override
            public void customConfigure() {
                bind(CryptoConfig.CRYPTO_SETTINGS, CryptoSettings.class, serverCryptoSettings);
                bind(DpwsConfig.HTTP_SUPPORT, Boolean.class, false);
                bind(DpwsConfig.HTTPS_SUPPORT, Boolean.class, true);
                bind(CryptoConfig.CRYPTO_DEVICE_HOSTNAME_VERIFIER, HostnameVerifier.class, verifier);
                bind(CryptoConfig.CRYPTO_TLS_ENABLED_VERSIONS, String[].class, new String[]{"TLSv1.3"});
                bind(CryptoConfig.CRYPTO_TLS_ENABLED_CIPHERS, String[].class, new String[]{"TLS_AES_128_GCM_SHA256"});
            }
        }, new MockedUdpBindingModule());

        final CryptoSettings clientCryptoSettings = Ssl.setupClient();
        var override = new AbstractModule() {
            @Override
            protected void configure() {
                bind(CommunicationLogSink.class).to(TestCommLogSink.class).asEagerSingleton();
                bind(CommunicationLog.class).to(CommunicationLogImpl.class).asEagerSingleton();
            }
        };
        try {
            this.clientPeer = new ClientPeer(new DefaultDpwsConfigModule() {
                @Override
                public void customConfigure() {
                    bind(WsDiscoveryConfig.MAX_WAIT_FOR_PROBE_MATCHES, Duration.class,
                            Duration.ofSeconds(MAX_WAIT_TIME.getSeconds() / 2));
                    bind(CryptoConfig.CRYPTO_SETTINGS, CryptoSettings.class, clientCryptoSettings);
                    bind(SoapConfig.JAXB_CONTEXT_PATH, String.class,
                            TestServiceMetadata.JAXB_CONTEXT_PATH);
                    bind(CryptoConfig.CRYPTO_TLS_ENABLED_VERSIONS, String[].class, new String[]{"TLSv1.3"});
                    bind(CryptoConfig.CRYPTO_TLS_ENABLED_CIPHERS, String[].class, new String[]{"TLS_AES_128_GCM_SHA256"});
                    bind(DpwsConfig.HTTP_SUPPORT, Boolean.class, false);
                    bind(DpwsConfig.HTTPS_SUPPORT, Boolean.class, true);
                }
            }, new MockedUdpBindingModule(), override);
        } catch (Exception e) {
            e.printStackTrace();
        }

        transportBindingFactory = clientPeer.getInjector().getInstance(TransportBindingFactory.class);
        requestResponseClientFactory = clientPeer.getInjector().getInstance(RequestResponseClientFactory.class);
        wseFactory = clientPeer.getInjector().getInstance(org.somda.sdc.dpws.soap.wseventing.model.ObjectFactory.class);
        marshallingService = clientPeer.getInjector().getInstance(MarshallingService.class);
        logSink = (TestCommLogSink) clientPeer.getInjector().getInstance(CommunicationLogSink.class);
    }

    @AfterEach
    void tearDown() {
        logSink.clear();
        this.devicePeer.stopAsync().awaitTerminated();
        this.clientPeer.stopAsync().awaitTerminated();
    }

    @Test
    void testSubscriptionProviderSubscriptionManagerEndpoint() throws Exception {
        devicePeer.startAsync().awaitRunning();
        clientPeer.startAsync().awaitRunning();

        HostingServiceProxy hostingServiceProxy = clientPeer.getClient().connect(devicePeer.getEprAddress())
                .get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        int COUNT = 100;
        SettableFuture<List<TestNotification>> notificationFuture = SettableFuture.create();
        HostedServiceProxy srv1 = hostingServiceProxy.getHostedServices().get(TestServiceMetadata.SERVICE_ID_1);
        ListenableFuture<SubscribeResult> subscribe = srv1.getEventSinkAccess()
                .subscribe(Collections.singletonList(TestServiceMetadata.ACTION_NOTIFICATION_1), Duration.ofMinutes(1),
                        new Interceptor() {
                            private final List<TestNotification> receivedNotifications = new ArrayList<>();

                            @MessageInterceptor(value = TestServiceMetadata.ACTION_NOTIFICATION_1)
                            void onNotification(NotificationObject message) {
                                assertTrue(message.getCommunicationContext().isPresent());
                                assertFalse(message.getCommunicationContext().get().getTransportInfo().getX509Certificates().isEmpty());
                                receivedNotifications.add(
                                        soapUtil.getBody(message.getNotification(), TestNotification.class)
                                                .orElseThrow(() -> new RuntimeException("TestNotification could not be converted")));
                                if (receivedNotifications.size() == COUNT) {
                                    notificationFuture.set(receivedNotifications);
                                }
                            }
                        });

        subscribe.get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);

        var transportBinding = transportBindingFactory.createHttpBinding(devicePeer.getDevice().getActiveSubscriptions().values().stream().findFirst().get().getSubscriptionManagerEpr().getAddress().getValue());
        var requestResponseClient = requestResponseClientFactory.createRequestResponseClient(transportBinding);

        var subscriptionManagerOpt = devicePeer.getDevice().getActiveSubscriptions().values().stream().findFirst();
        assertTrue(subscriptionManagerOpt.isPresent());

        testWseActions(subscriptionManagerOpt.get(), requestResponseClient);
    }

    @Test
    void testSubscriptionProviderHostedServiceEndpoint() throws Exception {
        devicePeer.startAsync().awaitRunning();
        clientPeer.startAsync().awaitRunning();

        HostingServiceProxy hostingServiceProxy = clientPeer.getClient().connect(devicePeer.getEprAddress())
                .get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        int COUNT = 100;
        SettableFuture<List<TestNotification>> notificationFuture = SettableFuture.create();
        HostedServiceProxy srv1 = hostingServiceProxy.getHostedServices().get(TestServiceMetadata.SERVICE_ID_1);
        ListenableFuture<SubscribeResult> subscribe = srv1.getEventSinkAccess()
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

        var transportBinding = transportBindingFactory.createHttpBinding(srv1.getActiveEprAddress());
        var requestResponseClient = requestResponseClientFactory.createRequestResponseClient(transportBinding);

        var subscriptionManagerOpt = devicePeer.getDevice().getActiveSubscriptions().values().stream().findFirst();
        assertTrue(subscriptionManagerOpt.isPresent());

        testWseActions(subscriptionManagerOpt.get(), requestResponseClient);
    }

    private void testWseActions(SubscriptionManager subscription, RequestResponseClient requestResponseClient) throws Exception {
        var getStatus = soapUtil.createMessage(WsEventingConstants.WSA_ACTION_GET_STATUS, wseFactory.createGetStatus());
        getStatus.getWsAddressingHeader().setTo(subscription.getSubscriptionManagerEpr().getAddress());
        var response = requestResponseClient.sendRequestResponse(getStatus);
        soapUtil.getBody(response, GetStatusResponse.class);

        var renewMessageBody = wseFactory.createRenew();
        renewMessageBody.setExpires(Duration.ofSeconds(120));
        var renewMessage = soapUtil.createMessage(WsEventingConstants.WSA_ACTION_RENEW, renewMessageBody);
        renewMessage.getWsAddressingHeader().setTo(subscription.getSubscriptionManagerEpr().getAddress());
        var renewResponse = requestResponseClient.sendRequestResponse(renewMessage);
        soapUtil.getBody(renewResponse, RenewResponse.class);

        var unsubscribeMessage = soapUtil.createMessage(WsEventingConstants.WSA_ACTION_UNSUBSCRIBE, wseFactory.createUnsubscribe());
        unsubscribeMessage.getWsAddressingHeader().setTo(subscription.getSubscriptionManagerEpr().getAddress());
        requestResponseClient.sendRequestResponse(unsubscribeMessage);
    }

    @Test
    void testSubscriptionConsumer() throws Exception {
        devicePeer.startAsync().awaitRunning();
        clientPeer.startAsync().awaitRunning();

        HostingServiceProxy hostingServiceProxy = clientPeer.getClient().connect(devicePeer.getEprAddress())
                .get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        int COUNT = 100;
        SettableFuture<List<TestNotification>> notificationFuture = SettableFuture.create();
        HostedServiceProxy srv1 = hostingServiceProxy.getHostedServices().get(TestServiceMetadata.SERVICE_ID_1);
        ListenableFuture<SubscribeResult> subscribe = srv1.getEventSinkAccess()
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

        var subscribeResult = subscribe.get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        assertEquals(1, devicePeer.getDevice().getActiveSubscriptions().size());

        var currentDuration = srv1.getEventSinkAccess().getStatus(subscribeResult.getSubscriptionId());
        var currentDurationResult = currentDuration.get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);

        var renewSubscription = srv1.getEventSinkAccess().renew(subscribeResult.getSubscriptionId(), Duration.ofMinutes(2));
        renewSubscription.get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        var newDuration = srv1.getEventSinkAccess().getStatus(subscribeResult.getSubscriptionId());
        var newDurationResult = newDuration.get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        assertTrue(0 < newDurationResult.compareTo(currentDurationResult));

        var unsubscribe = srv1.getEventSinkAccess().unsubscribe(subscribeResult.getSubscriptionId());
        unsubscribe.get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        assertEquals(0, devicePeer.getDevice().getActiveSubscriptions().size());

        seenWseMessageWithCorrectRequestUri(WsEventingConstants.WSA_ACTION_GET_STATUS);
        seenWseMessageWithCorrectRequestUri(WsEventingConstants.WSA_ACTION_RENEW);
        seenWseMessageWithCorrectRequestUri(WsEventingConstants.WSA_ACTION_UNSUBSCRIBE);
    }

    private void seenWseMessageWithCorrectRequestUri(String wseAction) throws Exception {
        var allRequests = logSink.getOutbound();
        assertFalse(allRequests.isEmpty());
        var allRequestUris = logSink.getRequestUris();
        assertEquals(allRequests.keySet(), allRequestUris.keySet());
        var seenWseAction = new AtomicBoolean(false);

        for (var transactionId : allRequests.keySet()) {
            var request = marshallingService.unmarshal(new ByteArrayInputStream(allRequests.get(transactionId).toByteArray()));
            var requestAction = request.getWsAddressingHeader().getAction();
            assertTrue(requestAction.isPresent());
            var requestUri = allRequestUris.get(transactionId);

            if (requestAction.get().getValue().equals(wseAction)) {
                seenWseAction.set(true);
                assertTrue(requestUri.isPresent());
                var wsaToHeader = request.getWsAddressingHeader().getTo();
                assertTrue(wsaToHeader.isPresent());
                assertNotEquals("", requestUri.get());
                assertTrue(wsaToHeader.get().getValue().endsWith(requestUri.get()));
            }
        }
        assertTrue(seenWseAction.get());
    }

    static class TestCommLogSink implements CommunicationLogSink {

        private final Map<String, ByteArrayOutputStream> outbound;
        private CommunicationLog.MessageType outboundMessageType;
        private final ArrayList<String> outboundTransactionIds;
        private final Map<String, Optional<String>> requestUris;

        TestCommLogSink() {
            this.outbound = new HashMap<>();
            this.outboundTransactionIds = new ArrayList<>();
            this.requestUris = new HashMap<>();
        }

        @Override
        public OutputStream createTargetStream(CommunicationLog.TransportType path,
                                               CommunicationLog.Direction direction,
                                               CommunicationLog.MessageType messageType,
                                               CommunicationContext communicationContext) {
            var os = new ByteArrayOutputStream();
            var appInfo = (HttpApplicationInfo) communicationContext.getApplicationInfo();
            if (CommunicationLog.Direction.OUTBOUND.equals(direction)) {
                outbound.put(appInfo.getTransactionId(), os);
                outboundMessageType = messageType;
                outboundTransactionIds.add(appInfo.getTransactionId());
                requestUris.put(appInfo.getTransactionId(), appInfo.getRequestUri());
            }
            return os;
        }

        public Map<String, ByteArrayOutputStream> getOutbound() {
            return outbound;
        }

        public void clear() {
            outbound.clear();
            requestUris.clear();
        }

        public CommunicationLog.MessageType getOutboundMessageType() {
            return outboundMessageType;
        }

        public ArrayList<String> getOutboundTransactionIds() {
            return outboundTransactionIds;
        }

        public Map<String, Optional<String>> getRequestUris() {
            return requestUris;
        }
    }
}
