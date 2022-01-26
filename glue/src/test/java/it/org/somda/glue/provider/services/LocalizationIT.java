package it.org.somda.glue.provider.services;

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
import org.somda.sdc.biceps.model.message.GetSupportedLanguages;
import org.somda.sdc.biceps.model.message.GetSupportedLanguagesResponse;
import org.somda.sdc.biceps.model.message.ObjectFactory;
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
    private static final String LOW_PRIORITY_SERVICES = "LowPriorityServices";
    private static final TimeUnit WAIT_TIME_UNIT = TimeUnit.SECONDS;
    private static int WAIT_IN_SECONDS = 30;

    static {
        if (CIDetector.isRunningInCi()) {
            WAIT_IN_SECONDS = 180;
            LOG.info("CI detected, setting WAIT_IN_SECONDS to {}", WAIT_IN_SECONDS);
        }
    }

    private final SoapUtil soapUtil = IT.getInjector().getInstance(SoapUtil.class);
    HostedServiceProxy lowPriorityService;
    private TestSdcDevice testDevice;
    private TestSdcClient testClient;
    private ObjectFactory factory;
    private MdibAccessObserverSpy mdibSpy;
    private SdcRemoteDevice sdcRemoteDevice;

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

        lowPriorityService = sdcRemoteDevice.getHostingServiceProxy().getHostedServices().get(LOW_PRIORITY_SERVICES);
    }

    @AfterEach
    void afterEach(TestInfo testInfo) {
        LOG.info("Done with test case {}", testInfo.getDisplayName());
        testDevice.stopAsync().awaitTerminated();
        testClient.stopAsync().awaitTerminated();
    }

    @Test
    void testSupportedLanguagesAction() throws Exception {
        final GetSupportedLanguages request = factory.createGetSupportedLanguages();
        final SoapMessage reqMsg = soapUtil.createMessage(ActionConstants.ACTION_GET_SUPPORTED_LANGUAGES, request);
        final SoapMessage resMsg = lowPriorityService.sendRequestResponse(reqMsg);

        final Optional<GetSupportedLanguagesResponse> resBody = soapUtil.getBody(resMsg,
                GetSupportedLanguagesResponse.class);

        assertTrue(resBody.isPresent());
        final GetSupportedLanguagesResponse response = resBody.get();

        assertNotNull(response);
        assertEquals(response.getLang().size(), 3);
        assertTrue(response.getLang().containsAll(List.of("EN", "DE", "ES")));
    }

    @Test
    void testGetLocalizedTextAction() throws Exception {
        {  // version = 0 should return the latest version
            final GetLocalizedTextResponse response = createRequestAndSend(
                    BigInteger.ZERO, Collections.emptyList(), Collections.emptyList());

            assertNotNull(response);
            assertEquals(response.getText().size(), 9);
            assertEquals(response.getText().get(0).getVersion(), BigInteger.TWO);
        }

        { // filter by version and language
            final GetLocalizedTextResponse response = createRequestAndSend(BigInteger.ONE, Collections.emptyList(),
                    List.of("EN"));
            assertNotNull(response);
            assertEquals(response.getText().size(), 3);
            assertEquals(response.getText().get(0).getLang(), "EN");
        }

        { // filter by version and reference
            final GetLocalizedTextResponse response = createRequestAndSend(BigInteger.ONE, List.of("REF1"),
                    Collections.emptyList());
            assertNotNull(response);
            assertEquals(response.getText().size(), 3);
            assertEquals(response.getText().get(0).getRef(), "REF1");
            assertEquals(response.getText().get(1).getRef(), "REF1");
            assertEquals(response.getText().get(2).getRef(), "REF1");
        }

        { // filter by version, language and reference
            final GetLocalizedTextResponse response = createRequestAndSend(BigInteger.ONE, List.of("REF2"),
                    List.of("DE"));
            assertNotNull(response);
            assertEquals(response.getText().size(), 1);
            assertEquals(response.getText().get(0).getRef(), "REF2");
            assertEquals(response.getText().get(0).getLang(), "DE");
        }
    }

    private GetLocalizedTextResponse createRequestAndSend(BigInteger version,
                                                          List<String> ref,
                                                          List<String> lang) throws Exception {
        final GetLocalizedText request = factory.createGetLocalizedText();
        request.setVersion(version);
        request.setRef(ref);
        request.setLang(lang);

        final SoapMessage reqMsg = soapUtil.createMessage(ActionConstants.ACTION_GET_LOCALIZED_TEXT, request);
        final SoapMessage resMsg = lowPriorityService.sendRequestResponse(reqMsg);

        final Optional<GetLocalizedTextResponse> resBody = soapUtil.getBody(resMsg, GetLocalizedTextResponse.class);

        assertTrue(resBody.isPresent());
        return resBody.get();
    }
}
