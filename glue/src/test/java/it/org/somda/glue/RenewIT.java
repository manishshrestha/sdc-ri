package it.org.somda.glue;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.AbstractModule;
import it.org.somda.glue.consumer.TestSdcClient;
import it.org.somda.glue.provider.TestSdcDevice;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.somda.sdc.biceps.testutil.MdibAccessObserverSpy;
import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.CommunicationLogImpl;
import org.somda.sdc.dpws.CommunicationLogSink;
import org.somda.sdc.dpws.factory.TransportBindingFactory;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.HttpApplicationInfo;
import org.somda.sdc.dpws.soap.MarshallingService;
import org.somda.sdc.dpws.soap.RequestResponseClient;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.factory.RequestResponseClientFactory;
import org.somda.sdc.dpws.soap.wseventing.SubscriptionManager;
import org.somda.sdc.dpws.soap.wseventing.WsEventingConstants;
import org.somda.sdc.dpws.soap.wseventing.model.GetStatusResponse;
import org.somda.sdc.dpws.soap.wseventing.model.RenewResponse;
import org.somda.sdc.glue.consumer.ConnectConfiguration;
import org.somda.sdc.glue.consumer.SdcRemoteDevice;
import test.org.somda.common.CIDetector;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RenewIT {
    private static final Logger LOG = LogManager.getLogger(CommunicationIT.class);
    private static final IntegrationTestUtil IT = new IntegrationTestUtil();

    private TestSdcDevice testDevice;
    private TestSdcClient testClient;

    private TransportBindingFactory transportBindingFactory;
    private RequestResponseClientFactory requestResponseClientFactory;

    private org.somda.sdc.dpws.soap.wseventing.model.ObjectFactory wseFactory;

    private static int WAIT_IN_SECONDS = 30;

    static {
        if (CIDetector.isRunningInCi()) {
            WAIT_IN_SECONDS = 180;
            LOG.info("CI detected, setting WAIT_IN_SECONDS to {}", WAIT_IN_SECONDS);
        }
    }

    private static final TimeUnit WAIT_TIME_UNIT = TimeUnit.SECONDS;

    private SoapUtil soapUtil;

    private TestCommLogSink logSink;
    private MarshallingService marshallingService;

    @BeforeEach
    void beforeEach(TestInfo testInfo) throws Exception {
        LOG.info("Running test case {}", testInfo.getDisplayName());

        var override = new AbstractModule() {
            @Override
            protected void configure() {
                bind(CommunicationLogSink.class).to(TestCommLogSink.class).asEagerSingleton();
                bind(CommunicationLog.class).to(CommunicationLogImpl.class).asEagerSingleton();
            }
        };

        testDevice = new TestSdcDevice();
        testClient = new TestSdcClient(override);

        transportBindingFactory = testClient.getInjector().getInstance(TransportBindingFactory.class);
        requestResponseClientFactory = testClient.getInjector().getInstance(RequestResponseClientFactory.class);
        wseFactory = IT.getInjector().getInstance(org.somda.sdc.dpws.soap.wseventing.model.ObjectFactory.class);
        soapUtil = testClient.getInjector().getInstance(SoapUtil.class);
        marshallingService = testClient.getInjector().getInstance(MarshallingService.class);
        logSink = (TestCommLogSink) testClient.getInjector().getInstance(CommunicationLogSink.class);
    }

    @AfterEach
    void afterEach(TestInfo testInfo) {
        LOG.info("Done with test case {}", testInfo.getDisplayName());
        logSink.clear();
        testClient.stopAsync().awaitTerminated();
        testDevice.stopAsync().awaitTerminated();
    }

    @Test
    void testRenewProvider() throws Exception {

        testDevice.startAsync().awaitRunning(WAIT_IN_SECONDS, WAIT_TIME_UNIT);
        testClient.startAsync().awaitRunning(WAIT_IN_SECONDS, WAIT_TIME_UNIT);

        final ListenableFuture<HostingServiceProxy> hostingServiceFuture = testClient.getClient()
                .connect(testDevice.getSdcDevice().getEprAddress());
        final HostingServiceProxy hostingServiceProxy = hostingServiceFuture.get(WAIT_IN_SECONDS, WAIT_TIME_UNIT);
        {
            final ListenableFuture<SdcRemoteDevice> remoteDeviceFuture = testClient.getConnector().connect(hostingServiceProxy,
                    ConnectConfiguration.create(ConnectConfiguration.ALL_EPISODIC_AND_WAVEFORM_REPORTS));

            final SdcRemoteDevice sdcRemoteDevice = remoteDeviceFuture.get(WAIT_IN_SECONDS, WAIT_TIME_UNIT);
            final MdibAccessObserverSpy mdibSpy = new MdibAccessObserverSpy();

            sdcRemoteDevice.getMdibAccessObservable().registerObserver(mdibSpy);

            var subscriptions = testDevice.getSdcDevice().getActiveSubscriptions();
            // there should only be one subscription, get it
            assertEquals(1, subscriptions.size());

            var subscription = subscriptions.entrySet().iterator().next().getValue();

            assertEquals(TestSdcClient.REQUESTED_EXPIRES, subscription.getExpires());

            var expiresTime = subscription.getExpiresTimeout();

            // wait enough time to ensure a renew must've happened
            Thread.sleep(TestSdcClient.REQUESTED_EXPIRES.toMillis());

            // we must be AFTER the expires moment now
            var now = LocalDateTime.now();
            assertTrue(now.isAfter(expiresTime));

            var transportBinding = transportBindingFactory.createHttpBinding(subscription.getSubscriptionManagerEpr().getAddress().getValue());
            var requestResponseClient = requestResponseClientFactory.createRequestResponseClient(transportBinding);

            testWseActions(subscription, requestResponseClient);
        }
        // also test renew with the hosted service eprs addresses
        for (var service : hostingServiceProxy.getHostedServices().values()) {
            testClient.getConnector().disconnect(testDevice.getSdcDevice().getEprAddress());
            final ListenableFuture<SdcRemoteDevice> remoteDeviceFuture = testClient.getConnector().connect(hostingServiceProxy,
                    ConnectConfiguration.create(ConnectConfiguration.ALL_EPISODIC_AND_WAVEFORM_REPORTS));

            final SdcRemoteDevice sdcRemoteDevice = remoteDeviceFuture.get(WAIT_IN_SECONDS, WAIT_TIME_UNIT);
            final MdibAccessObserverSpy mdibSpy = new MdibAccessObserverSpy();

            sdcRemoteDevice.getMdibAccessObservable().registerObserver(mdibSpy);

            var subscriptions = testDevice.getSdcDevice().getActiveSubscriptions();
            // there should only be one subscription, get it
            assertEquals(1, subscriptions.size());

            var subscription = subscriptions.entrySet().iterator().next().getValue();

            assertEquals(TestSdcClient.REQUESTED_EXPIRES, subscription.getExpires());

            var expiresTime = subscription.getExpiresTimeout();

            // wait enough time to ensure a renew must've happened
            Thread.sleep(TestSdcClient.REQUESTED_EXPIRES.toMillis());

            // we must be AFTER the expires moment now
            var now = LocalDateTime.now();
            assertTrue(now.isAfter(expiresTime));

            var transportBinding = transportBindingFactory.createHttpBinding(service.getActiveEprAddress());
            var requestResponseClient = requestResponseClientFactory.createRequestResponseClient(transportBinding);

            testWseActions(subscription, requestResponseClient);
        }

        testClient.getConnector().disconnect(testDevice.getSdcDevice().getEprAddress());

    }

    private void testWseActions(SubscriptionManager subscription, RequestResponseClient requestResponseClient) throws Exception {
        var getStatus = soapUtil.createMessage(WsEventingConstants.WSA_ACTION_GET_STATUS, wseFactory.createGetStatus());
        getStatus.getWsAddressingHeader().setTo(subscription.getSubscriptionManagerEpr().getAddress());
        var response = requestResponseClient.sendRequestResponse(getStatus);
        soapUtil.getBody(response, GetStatusResponse.class);

        var renewMessageBody = wseFactory.createRenew();
        renewMessageBody.setExpires(Duration.ofSeconds(60));
        var renewMessage = soapUtil.createMessage(WsEventingConstants.WSA_ACTION_RENEW, renewMessageBody);
        renewMessage.getWsAddressingHeader().setTo(subscription.getSubscriptionManagerEpr().getAddress());
        var renewResponse = requestResponseClient.sendRequestResponse(renewMessage);
        soapUtil.getBody(renewResponse, RenewResponse.class);

        var unsubscribeMessage = soapUtil.createMessage(WsEventingConstants.WSA_ACTION_UNSUBSCRIBE, wseFactory.createUnsubscribe());
        unsubscribeMessage.getWsAddressingHeader().setTo(subscription.getSubscriptionManagerEpr().getAddress());
        requestResponseClient.sendRequestResponse(unsubscribeMessage);
    }

    @Test
    void testRenewConsumer() throws Exception {
        testDevice.startAsync().awaitRunning(WAIT_IN_SECONDS, WAIT_TIME_UNIT);
        testClient.startAsync().awaitRunning(WAIT_IN_SECONDS, WAIT_TIME_UNIT);

        final ListenableFuture<HostingServiceProxy> hostingServiceFuture = testClient.getClient()
                .connect(testDevice.getSdcDevice().getEprAddress());
        final HostingServiceProxy hostingServiceProxy = hostingServiceFuture.get(WAIT_IN_SECONDS, WAIT_TIME_UNIT);

        final ListenableFuture<SdcRemoteDevice> remoteDeviceFuture = testClient.getConnector().connect(hostingServiceProxy,
                ConnectConfiguration.create(ConnectConfiguration.ALL_EPISODIC_AND_WAVEFORM_REPORTS));

        final SdcRemoteDevice sdcRemoteDevice = remoteDeviceFuture.get(WAIT_IN_SECONDS, WAIT_TIME_UNIT);

        Thread.sleep(TestSdcClient.REQUESTED_EXPIRES.toMillis());

        var allRequests = logSink.getOutbound();
        var allRequestUris = logSink.getRequestUris();
        assertEquals(allRequests.size(), allRequestUris.size());

        for (int i = 0; i < allRequests.size(); i++) {
            var request = marshallingService.unmarshal(new ByteArrayInputStream(allRequests.get(i).toByteArray()));
            var requestAction = request.getWsAddressingHeader().getAction();
            assertTrue(requestAction.isPresent());
            var requestUri = allRequestUris.get(i);
            // check just the renew messages
            if (requestAction.get().getValue().equals(WsEventingConstants.WSA_ACTION_RENEW)) {
                assertTrue(requestUri.isPresent());
                var wsaToHeader = request.getWsAddressingHeader().getTo();
                assertTrue(wsaToHeader.isPresent());
                assertTrue(wsaToHeader.get().getValue().contains(requestUri.get()));
            }
        }
    }

    static class TestCommLogSink implements CommunicationLogSink {

        private final ArrayList<ByteArrayOutputStream> outbound;
        private CommunicationLog.MessageType outboundMessageType;
        private final ArrayList<String> outboundTransactionIds;
        private final ArrayList<Optional<String>> requestUri;

        TestCommLogSink() {
            this.outbound = new ArrayList<>();
            this.outboundTransactionIds = new ArrayList<>();
            this.requestUri = new ArrayList<>();
        }

        @Override
        public OutputStream createTargetStream(CommunicationLog.TransportType path,
                                               CommunicationLog.Direction direction,
                                               CommunicationLog.MessageType messageType,
                                               CommunicationContext communicationContext) {
            var os = new ByteArrayOutputStream();
            var appInfo = (HttpApplicationInfo) communicationContext.getApplicationInfo();
            if (CommunicationLog.Direction.OUTBOUND.equals(direction)) {
                outbound.add(os);
                outboundMessageType = messageType;
                outboundTransactionIds.add(appInfo.getTransactionId());
                requestUri.add(appInfo.getRequestUri());
            }
            return os;
        }

        public ArrayList<ByteArrayOutputStream> getOutbound() {
            return outbound;
        }

        public void clear() {
            outbound.clear();
            requestUri.clear();
        }

        public CommunicationLog.MessageType getOutboundMessageType() {
            return outboundMessageType;
        }

        public ArrayList<String> getOutboundTransactionIds() {
            return outboundTransactionIds;
        }

        public ArrayList<Optional<String>> getRequestUris() {
            return requestUri;
        }
    }
}
