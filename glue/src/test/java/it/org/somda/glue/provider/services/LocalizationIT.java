package it.org.somda.glue.provider.services;

import org.somda.sdc.biceps.model.message.GetSupportedLanguages;
import org.somda.sdc.biceps.model.message.GetSupportedLanguagesResponse;
import org.somda.sdc.biceps.model.message.ObjectFactory;
import it.org.somda.glue.IntegrationTestUtil;
import it.org.somda.glue.consumer.TestSdcClient;
import it.org.somda.glue.provider.TestSdcDevice;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.biceps.model.message.GetLocalizedText;
import org.somda.sdc.biceps.model.message.GetLocalizedTextResponse;
import org.somda.sdc.biceps.testutil.MdibAccessObserverSpy;
import org.somda.sdc.dpws.service.HostedServiceProxy;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.glue.common.ActionConstants;
import org.somda.sdc.glue.consumer.ConnectConfiguration;
import org.somda.sdc.glue.consumer.SdcRemoteDevice;
import test.org.somda.common.CIDetector;
import test.org.somda.common.LoggingTestWatcher;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(LoggingTestWatcher.class)
public class LocalizationIT {
    private static final Logger LOG = LogManager.getLogger(LocalizationIT.class);
    private static final IntegrationTestUtil IT = new IntegrationTestUtil();

    private TestSdcDevice testDevice;
    private TestSdcClient testClient;

    private ObjectFactory factory;

    private static int WAIT_IN_SECONDS = 30;
    private static String LOW_PRIORITY_SERVICES = "LowPriorityServices";

    static {
        if (CIDetector.isRunningInCi()) {
            WAIT_IN_SECONDS = 180;
            LOG.info("CI detected, setting WAIT_IN_SECONDS to {}", WAIT_IN_SECONDS);
        }
    }

    private static final TimeUnit WAIT_TIME_UNIT = TimeUnit.SECONDS;

    private MdibAccessObserverSpy mdibSpy;
    private SdcRemoteDevice sdcRemoteDevice;
    private final SoapUtil soapUtil = IT.getInjector().getInstance(SoapUtil.class);

    @BeforeEach
    void beforeEach(TestInfo testInfo) throws Exception {
        LOG.info("Running test case {}", testInfo.getDisplayName());
        testDevice = new TestSdcDevice();
        testClient = new TestSdcClient();
        factory = new ObjectFactory();

        testDevice.startAsync().awaitRunning(WAIT_IN_SECONDS, WAIT_TIME_UNIT);
        testClient.startAsync().awaitRunning(WAIT_IN_SECONDS, WAIT_TIME_UNIT);

        var hostingServiceFuture = testClient.getClient()
                .connect(testDevice.getSdcDevice().getEprAddress());
        var hostingServiceProxy = hostingServiceFuture.get(WAIT_IN_SECONDS, WAIT_TIME_UNIT);
        var remoteDeviceFuture = testClient.getConnector().connect(hostingServiceProxy,
                ConnectConfiguration.create(ConnectConfiguration.ALL_EPISODIC_AND_WAVEFORM_REPORTS));
        sdcRemoteDevice = remoteDeviceFuture.get(WAIT_IN_SECONDS, WAIT_TIME_UNIT);

        mdibSpy = new MdibAccessObserverSpy();
        sdcRemoteDevice.getMdibAccessObservable().registerObserver(mdibSpy);
    }

    @AfterEach
    void afterEach(TestInfo testInfo) {
        LOG.info("Done with test case {}", testInfo.getDisplayName());
        testDevice.stopAsync().awaitTerminated();
        testClient.stopAsync().awaitTerminated();
    }

    @Test
    void testGetLocalizedTextAction() throws Exception {
        final HostedServiceProxy lowPriorityService = sdcRemoteDevice.getHostingServiceProxy()
                .getHostedServices().get(LOW_PRIORITY_SERVICES);
        assertNotNull(lowPriorityService);
        final GetLocalizedText request = createRequest(BigInteger.ONE, Collections.emptyList(),
                Collections.emptyList());
        final SoapMessage reqMsg = soapUtil.createMessage(ActionConstants.ACTION_GET_LOCALIZED_TEXT, request);
        final SoapMessage resMsg = lowPriorityService.sendRequestResponse(reqMsg);

        final Optional<GetLocalizedTextResponse> resBody = soapUtil.getBody(resMsg, GetLocalizedTextResponse.class);

        assertTrue(resBody.isPresent());
        final GetLocalizedTextResponse response = resBody.get();

        assertNotNull(response);
        assertEquals(response.getText().size(), 9);
    }

    @Test
    void testSupportedLanguagesAction() throws Exception {
        final HostedServiceProxy lowPriorityService = sdcRemoteDevice.getHostingServiceProxy()
                .getHostedServices().get(LOW_PRIORITY_SERVICES);
        assertNotNull(lowPriorityService);
        final GetSupportedLanguages request = factory.createGetSupportedLanguages();
        final SoapMessage reqMsg = soapUtil.createMessage(ActionConstants.ACTION_GET_SUPPORTED_LANGUAGES, request);
        final SoapMessage resMsg = lowPriorityService.sendRequestResponse(reqMsg);

        final Optional<GetSupportedLanguagesResponse> resBody = soapUtil.getBody(resMsg,
                GetSupportedLanguagesResponse.class);

        assertTrue(resBody.isPresent());
        final GetSupportedLanguagesResponse response = resBody.get();

        assertNotNull(response);
        assertEquals(response.getLang().size(), 3);
    }

    private GetLocalizedText createRequest(BigInteger version, List<String> ref, List<String> lang) {
        final GetLocalizedText request = factory.createGetLocalizedText();
        request.setVersion(version);
        request.setRef(ref);
        request.setLang(lang);
        return request;
    }
}
