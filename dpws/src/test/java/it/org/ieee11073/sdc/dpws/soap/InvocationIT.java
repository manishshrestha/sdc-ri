package it.org.ieee11073.sdc.dpws.soap;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import dpws_test_service.messages._2017._05._10.ObjectFactory;
import dpws_test_service.messages._2017._05._10.TestNotification;
import dpws_test_service.messages._2017._05._10.TestOperationRequest;
import dpws_test_service.messages._2017._05._10.TestOperationResponse;
import it.org.ieee11073.sdc.dpws.IntegrationTestUtil;
import it.org.ieee11073.sdc.dpws.TestServiceMetadata;
import org.apache.log4j.BasicConfigurator;
import org.ieee11073.sdc.dpws.service.HostedServiceProxy;
import org.ieee11073.sdc.dpws.service.HostingServiceProxy;
import org.ieee11073.sdc.dpws.soap.SoapMessage;
import org.ieee11073.sdc.dpws.soap.SoapUtil;
import org.ieee11073.sdc.dpws.soap.interception.Interceptor;
import org.ieee11073.sdc.dpws.soap.interception.MessageInterceptor;
import org.ieee11073.sdc.dpws.soap.interception.NotificationObject;
import org.ieee11073.sdc.dpws.soap.wseventing.SubscribeResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.JDBCType;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class InvocationIT {
    private static final Duration MAX_WAIT_TIME = IntegrationTestUtil.MAX_WAIT_TIME;

    private final IntegrationTestUtil IT = new IntegrationTestUtil();

    private BasicPopulatedDevice devicePeer;
    private ClientPeer clientPeer;

    private HostingServiceProxy hostingServiceProxy;
    private ObjectFactory factory;
    private SoapUtil soapUtil = IT.getInjector().getInstance(SoapUtil.class);

    @Before
    public void setUp() throws Exception {
        BasicConfigurator.configure();

        factory = new ObjectFactory();

        devicePeer = new BasicPopulatedDevice();
        clientPeer = new ClientPeer();
        devicePeer.startAsync().awaitRunning();
        clientPeer.startAsync().awaitRunning();

        hostingServiceProxy = clientPeer.getClient().connect(devicePeer.getEprAddress())
                .get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
    }

    @After
    public void tearDown() throws Exception {
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
    }

    @Test
    public void notification() throws Exception {
        final int COUNT = 100;
        final SettableFuture<List<TestNotification>> notificationFuture = SettableFuture.create();
        final HostedServiceProxy srv1 = hostingServiceProxy.getHostedServices().get(BasicPopulatedDevice.SERVICE_ID_1);
        final ListenableFuture<SubscribeResult> subscribe = srv1.getEventSinkAccess()
                .subscribe(Arrays.asList(TestServiceMetadata.ACTION_NOTIFICATION_1), Duration.ofMinutes(1),
                        new Interceptor() {
                            private List<TestNotification> receivedNotifications = new ArrayList<>();
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
            devicePeer.getService1().sendNotifications(Arrays.asList(testNotification));
        }

        final List<TestNotification> notifications = notificationFuture.get(MAX_WAIT_TIME.getSeconds(), TimeUnit.SECONDS);
        assertEquals(COUNT, notifications.size());
        for (int i = 0; i < COUNT; ++i) {
            TestNotification notification = notifications.get(i);
            assertEquals(Integer.toString(i), notification.getParam1());
            assertEquals(i, notification.getParam2());
        }
    }

    @Test
    public void notificationWithMultipleSubscriptions() throws Exception {

    }

    @Test
    public void notificationSubscribeMultipleActions() throws Exception {

    }
}
