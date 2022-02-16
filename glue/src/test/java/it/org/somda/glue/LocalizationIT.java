package it.org.somda.glue;

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
import org.somda.sdc.biceps.model.message.GetSupportedLanguagesResponse;
import org.somda.sdc.biceps.model.message.ObjectFactory;
import org.somda.sdc.glue.consumer.ConnectConfiguration;
import org.somda.sdc.glue.consumer.SdcRemoteDevice;
import org.somda.sdc.glue.consumer.localization.LocalizationServiceAccess;
import test.org.somda.common.CIDetector;
import test.org.somda.common.LoggingTestWatcher;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(LoggingTestWatcher.class)
public class LocalizationIT {
    private static final Logger LOG = LogManager.getLogger(LocalizationIT.class);
    private static final TimeUnit WAIT_TIME_UNIT = TimeUnit.SECONDS;
    private static int WAIT_IN_SECONDS = 30;

    static {
        if (CIDetector.isRunningInCi()) {
            WAIT_IN_SECONDS = 180;
            LOG.info("CI detected, setting WAIT_IN_SECONDS to {}", WAIT_IN_SECONDS);
        }
    }

    private TestSdcDevice testDevice;
    private TestSdcClient testClient;
    private ObjectFactory factory;
    private LocalizationServiceAccess localizationServiceAccess;

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
        SdcRemoteDevice sdcRemoteDevice = remoteDeviceFuture.get(WAIT_IN_SECONDS, WAIT_TIME_UNIT);

        localizationServiceAccess = sdcRemoteDevice.getLocalizationServiceAccess();
    }

    @AfterEach
    void afterEach(TestInfo testInfo) {
        LOG.info("Done with test case {}", testInfo.getDisplayName());
        testDevice.stopAsync().awaitTerminated();
        testClient.stopAsync().awaitTerminated();
    }

    @Test
    void testSupportedLanguagesAction() throws Exception {
        final GetSupportedLanguagesResponse response = localizationServiceAccess
                .getSupportedLanguages(factory.createGetSupportedLanguages())
                .get(WAIT_IN_SECONDS, WAIT_TIME_UNIT);

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

    @Test
    void testLocalizationCachePrefetch() throws Exception {
        localizationServiceAccess.cachePrefetch(BigInteger.ONE, List.of("EN"));

        testDevice.stopAsync().awaitTerminated();
        { // localized texts are cached on consumer side, so request works even if provider is down
            final GetLocalizedTextResponse response = createRequestAndSend(
                    BigInteger.ONE, List.of("REF1"), List.of("EN"));
            assertNotNull(response);
            assertEquals(response.getText().size(), 1);
            assertEquals(response.getText().get(0).getRef(), "REF1");
            assertEquals(response.getText().get(0).getLang(), "EN");
        }

        { // fails if languages doesn't exist in cache
            assertThrows(
                    ExecutionException.class,
                    () -> createRequestAndSend(BigInteger.ONE, List.of("REF1"), List.of("DE")));
        }

        { // fails if version doesn't exist in cache
            assertThrows(
                    ExecutionException.class,
                    () -> createRequestAndSend(BigInteger.TWO, List.of("REF1"), List.of("EN")));
        }
    }

    private GetLocalizedTextResponse createRequestAndSend(BigInteger version,
                                                          List<String> ref,
                                                          List<String> lang) throws Exception {
        final GetLocalizedText request = factory.createGetLocalizedText();
        request.setVersion(version);
        request.setRef(ref);
        request.setLang(lang);

        return localizationServiceAccess
                .getLocalizedText(request)
                .get(WAIT_IN_SECONDS, WAIT_TIME_UNIT);
    }
}
