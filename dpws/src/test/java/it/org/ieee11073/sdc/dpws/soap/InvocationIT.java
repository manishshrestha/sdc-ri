package it.org.ieee11073.sdc.dpws.soap;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import dpws_test_service.messages._2017._05._10.ObjectFactory;
import dpws_test_service.messages._2017._05._10.TestNotification;
import dpws_test_service.messages._2017._05._10.TestOperationRequest;
import dpws_test_service.messages._2017._05._10.TestOperationResponse;
import it.org.ieee11073.sdc.dpws.IntegrationTestUtil;
import it.org.ieee11073.sdc.dpws.TestServiceMetadata;
import org.apache.log4j.BasicConfigurator;
import org.ieee11073.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.ieee11073.sdc.dpws.service.HostedServiceProxy;
import org.ieee11073.sdc.dpws.service.HostingServiceProxy;
import org.ieee11073.sdc.dpws.soap.SoapConfig;
import org.ieee11073.sdc.dpws.soap.SoapMessage;
import org.ieee11073.sdc.dpws.soap.SoapUtil;
import org.ieee11073.sdc.dpws.soap.interception.Interceptor;
import org.ieee11073.sdc.dpws.soap.interception.MessageInterceptor;
import org.ieee11073.sdc.dpws.soap.interception.NotificationObject;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.WsDiscoveryConfig;
import org.ieee11073.sdc.dpws.soap.wseventing.SubscribeResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class InvocationIT {
    private static final Duration MAX_WAIT_TIME = IntegrationTestUtil.MAX_WAIT_TIME;

    private final IntegrationTestUtil IT = new IntegrationTestUtil();

    private BasicPopulatedDevice devicePeer;
    private ClientPeer clientPeer;

    private HostingServiceProxy hostingServiceProxy;
    private ObjectFactory factory;
    private final SoapUtil soapUtil = IT.getInjector().getInstance(SoapUtil.class);

    @Before
    public void setUp() throws Exception {
        BasicConfigurator.configure();

        factory = new ObjectFactory();

        devicePeer = new BasicPopulatedDevice();
        clientPeer = new ClientPeer(new DefaultDpwsConfigModule() {
            @Override
            public void customConfigure() {
                bind(SoapConfig.JAXB_CONTEXT_PATH, String.class,
                        TestServiceMetadata.JAXB_CONTEXT_PATH);
            }
        });
        devicePeer.startAsync().awaitRunning();
        clientPeer.startAsync().awaitRunning();

        hostingServiceProxy = clientPeer.getClient().connect(devicePeer.getEprAddress())
                .get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
    }

    @After
    public void tearDown() {
        this.devicePeer.stopAsync().awaitTerminated();
        this.clientPeer.stopAsync().awaitTerminated();
    }

    @Test
    public void requestResponse() throws Exception {
        final int COUNT = 100;

        final HostedServiceProxy srv1 = hostingServiceProxy.getHostedServices().get(BasicPopulatedDevice.SERVICE_ID_1);
        assertNotNull(srv1);

        final String testString = "test";
        final Integer testInt = 10;

        final String expectedString = new StringBuilder(testString).reverse().toString();
        final Integer expectedInt = testInt * 2;

        final TestOperationRequest request = factory.createTestOperationRequest();
        request.setParam1(testString);
        request.setParam2(testInt);

        // test multiple iterations
        for (int i = 0; i < COUNT; ++i) {
            final SoapMessage reqMsg = soapUtil.createMessage(TestServiceMetadata.ACTION_OPERATION_REQUEST_1, request);
            final SoapMessage resMsg = srv1.sendRequestResponse(reqMsg);
            final Optional<TestOperationResponse> resBody = soapUtil.getBody(resMsg, TestOperationResponse.class);
            assertTrue(resBody.isPresent());
            final TestOperationResponse testOperationResponse = resBody.get();
            assertEquals(expectedString, testOperationResponse.getResult1());
            assertEquals(expectedInt.intValue(), testOperationResponse.getResult2());
        }
        
        // test multiple iterations concurrently (in order to increase probability of concurrency issues)
        ArrayList<ListenableFuture<Boolean>> futures = new ArrayList<>();
        final ListeningExecutorService les = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(COUNT));
        for (int i = 0; i < COUNT; ++i) {
            final ListenableFuture<Boolean> future = les.submit(() -> {
                final SoapMessage reqMsg = soapUtil.createMessage(TestServiceMetadata.ACTION_OPERATION_REQUEST_1, request);
                final SoapMessage resMsg = srv1.sendRequestResponse(reqMsg);
                final Optional<TestOperationResponse> resBody = soapUtil.getBody(resMsg, TestOperationResponse.class);
                assertTrue(resBody.isPresent());
                final TestOperationResponse testOperationResponse = resBody.get();
                assertEquals(expectedString, testOperationResponse.getResult1());
                assertEquals(expectedInt.intValue(), testOperationResponse.getResult2());
                return true;
            });
            futures.add(future);
        }

        for (int i = 0; i < COUNT; ++i) {
            futures.get(i).get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        }
    }

    @Test
    public void notification() throws Exception {
        final int COUNT = 100;
        final SettableFuture<List<TestNotification>> notificationFuture = SettableFuture.create();
        final HostedServiceProxy srv1 = hostingServiceProxy.getHostedServices().get(BasicPopulatedDevice.SERVICE_ID_1);
        final ListenableFuture<SubscribeResult> subscribe = srv1.getEventSinkAccess()
                .subscribe(Collections.singletonList(TestServiceMetadata.ACTION_NOTIFICATION_1), Duration.ofMinutes(1),
                        new Interceptor() {
                            private final List<TestNotification> receivedNotifications = new ArrayList<>();

                            @MessageInterceptor
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

    @Test
    public void notificationWithMultipleSubscriptions() throws Exception {
        final int COUNT = 100;
        final SettableFuture<List<TestNotification>> notificationFuture1 = SettableFuture.create();
        final SettableFuture<List<TestNotification>> notificationFuture2 = SettableFuture.create();
        final SettableFuture<List<TestNotification>> notificationFuture3 = SettableFuture.create();
        final HostedServiceProxy srv1 = hostingServiceProxy.getHostedServices().get(BasicPopulatedDevice.SERVICE_ID_1);
        final HostedServiceProxy srv2 = hostingServiceProxy.getHostedServices().get(BasicPopulatedDevice.SERVICE_ID_2);
        final ListenableFuture<SubscribeResult> subscribe1 = srv1.getEventSinkAccess()
                .subscribe(Collections.singletonList(TestServiceMetadata.ACTION_NOTIFICATION_1), Duration.ofMinutes(1),
                        new Interceptor() {
                            private final List<TestNotification> receivedNotifications = new ArrayList<>();

                            @MessageInterceptor
                            void onNotification(NotificationObject message) {
                                receivedNotifications.add(
                                        soapUtil.getBody(message.getNotification(), TestNotification.class)
                                                .orElseThrow(() -> new RuntimeException("TestNotification could not be converted")));
                                if (receivedNotifications.size() == COUNT) {
                                    notificationFuture1.set(receivedNotifications);
                                }
                            }
                        });

        final ListenableFuture<SubscribeResult> subscribe2 = srv1.getEventSinkAccess()
                .subscribe(Collections.singletonList(TestServiceMetadata.ACTION_NOTIFICATION_2), Duration.ofMinutes(1),
                        new Interceptor() {
                            private final List<TestNotification> receivedNotifications = new ArrayList<>();

                            @MessageInterceptor
                            void onNotification(NotificationObject message) {
                                receivedNotifications.add(
                                        soapUtil.getBody(message.getNotification(), TestNotification.class)
                                                .orElseThrow(() -> new RuntimeException("TestNotification could not be converted")));
                                if (receivedNotifications.size() == COUNT) {
                                    notificationFuture2.set(receivedNotifications);
                                }
                            }
                        });
        final ListenableFuture<SubscribeResult> subscribe3 = srv2.getEventSinkAccess()
                .subscribe(Collections.singletonList(TestServiceMetadata.ACTION_NOTIFICATION_3), Duration.ofMinutes(1),
                        new Interceptor() {
                            private final List<TestNotification> receivedNotifications = new ArrayList<>();

                            @MessageInterceptor
                            void onNotification(NotificationObject message) {
                                receivedNotifications.add(
                                        soapUtil.getBody(message.getNotification(), TestNotification.class)
                                                .orElseThrow(() -> new RuntimeException("TestNotification could not be converted")));
                                if (receivedNotifications.size() == COUNT) {
                                    notificationFuture3.set(receivedNotifications);
                                }
                            }
                        });

        subscribe1.get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        subscribe2.get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        subscribe3.get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);

        for (int i = 0; i < COUNT; ++i) {
            final TestNotification testNotification = factory.createTestNotification();
            testNotification.setParam1(Integer.toString(i));
            testNotification.setParam2(i);
            devicePeer.getService1().sendNotification(testNotification);
            devicePeer.getService2().sendNotification(testNotification);
        }

        final List<TestNotification> notifications1 = notificationFuture1.get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        final List<TestNotification> notifications2 = notificationFuture2.get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        final List<TestNotification> notifications3 = notificationFuture3.get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        assertEquals(COUNT, notifications1.size());
        assertEquals(COUNT, notifications2.size());
        assertEquals(COUNT, notifications3.size());
        for (int i = 0; i < COUNT; ++i) {
            final TestNotification notification1 = notifications1.get(i);
            assertEquals(Integer.toString(i), notification1.getParam1());
            assertEquals(i, notification1.getParam2());
            final TestNotification notification2 = notifications2.get(i);
            assertEquals(Integer.toString(i), notification2.getParam1());
            assertEquals(i, notification2.getParam2());
            final TestNotification notification3 = notifications3.get(i);
            assertEquals(Integer.toString(i), notification3.getParam1());
            assertEquals(i, notification3.getParam2());
        }
    }

    @Test
    public void notificationSubscribeMultipleActions() throws Exception {
        final int COUNT = 100;
        final SettableFuture<List<TestNotification>> notificationFuture = SettableFuture.create();
        final HostedServiceProxy srv1 = hostingServiceProxy.getHostedServices().get(BasicPopulatedDevice.SERVICE_ID_1);
        final ListenableFuture<SubscribeResult> subscribe = srv1.getEventSinkAccess()
                .subscribe(Arrays.asList(TestServiceMetadata.ACTION_NOTIFICATION_1, TestServiceMetadata.ACTION_NOTIFICATION_2), Duration.ofMinutes(1),
                        new Interceptor() {
                            private final List<TestNotification> receivedNotifications = new ArrayList<>();

                            @MessageInterceptor
                            void onNotification(NotificationObject message) {
                                receivedNotifications.add(
                                        soapUtil.getBody(message.getNotification(), TestNotification.class)
                                                .orElseThrow(() -> new RuntimeException("TestNotification could not be converted")));
                                if (receivedNotifications.size() == COUNT*2) {
                                    notificationFuture.set(receivedNotifications);
                                }
                            }
                        });

        subscribe.get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);

        for (int i = 0; i < COUNT; ++i) {
            final TestNotification testNotification = factory.createTestNotification();
            testNotification.setParam1(Integer.toString(i));
            testNotification.setParam2(i);
            devicePeer.getService1().sendNotification(testNotification);
        }

        final List<TestNotification> notifications = notificationFuture.get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        assertEquals(COUNT*2, notifications.size());
        for (int i = 0; i < COUNT; ++i) {
            final TestNotification notificationEven = notifications.get(i*2);
            assertEquals(Integer.toString(i), notificationEven.getParam1());
            assertEquals(i, notificationEven.getParam2());
            final TestNotification notificationOdd = notifications.get(i*2+1);
            assertEquals(Integer.toString(i), notificationOdd.getParam1());
            assertEquals(i, notificationOdd.getParam2());
        }
    }
}
