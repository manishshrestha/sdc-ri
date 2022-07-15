package it.org.somda.glue;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import it.org.somda.glue.consumer.TestSdcClient;
import it.org.somda.glue.provider.TestSdcDevice;
import it.org.somda.sdc.dpws.IntegrationTestUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.somda.sdc.biceps.common.MdibStateModifications;
import org.somda.sdc.biceps.common.MdibTypeValidator;
import org.somda.sdc.biceps.model.history.ChangeSequenceReportType;
import org.somda.sdc.biceps.model.history.HistoryQueryType;
import org.somda.sdc.biceps.model.history.ObjectFactory;
import org.somda.sdc.biceps.model.history.VersionRangeType;
import org.somda.sdc.biceps.model.participant.NumericMetricState;
import org.somda.sdc.biceps.testutil.BaseTreeModificationsSet;
import org.somda.sdc.biceps.testutil.Handles;
import org.somda.sdc.biceps.testutil.MockEntryFactory;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.interception.Interceptor;
import org.somda.sdc.dpws.soap.interception.MessageInterceptor;
import org.somda.sdc.dpws.soap.interception.NotificationObject;
import org.somda.sdc.dpws.soap.wseventing.SubscribeResult;
import org.somda.sdc.dpws.soap.wseventing.model.FilterType;
import test.org.somda.common.CIDetector;
import test.org.somda.common.LoggingTestWatcher;

import javax.xml.bind.JAXBElement;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.somda.sdc.glue.GlueConstants.WS_EVENTING_HISTORY_DIALECT;
import static org.somda.sdc.glue.common.ActionConstants.ACTION_HISTORY_MDIB_REPORT;
import static org.somda.sdc.glue.common.WsdlConstants.SERVICE_HISTORY;

@ExtendWith(LoggingTestWatcher.class)
public class HistoryIT {
    private static final Logger LOG = LogManager.getLogger(HistoryIT.class);
    private static final TimeUnit WAIT_TIME_UNIT = TimeUnit.SECONDS;
    private static int WAIT_IN_SECONDS = 30;

    static {
        if (CIDetector.isRunningInCi()) {
            WAIT_IN_SECONDS = 180;
            LOG.info("CI detected, setting WAIT_IN_SECONDS to {}", WAIT_IN_SECONDS);
        }
    }

    private final it.org.somda.sdc.dpws.IntegrationTestUtil IT = new IntegrationTestUtil();
    private final SoapUtil soapUtil = IT.getInjector().getInstance(SoapUtil.class);
    private TestSdcDevice testDevice;
    private TestSdcClient testClient;
    private HostingServiceProxy hostingServiceProxy;

    private ObjectFactory objectFactory;
    private org.somda.sdc.dpws.soap.wseventing.model.ObjectFactory wseFactory;

    @BeforeEach
    void beforeEach(TestInfo testInfo) throws Exception {
        LOG.info("Running test case {}", testInfo.getDisplayName());

        testDevice = new TestSdcDevice();
        testClient = new TestSdcClient();

        BaseTreeModificationsSet baseTreeModificationsSet = new BaseTreeModificationsSet(
                new MockEntryFactory(IT.getInjector().getInstance(MdibTypeValidator.class)));

        testDevice.getSdcDevice().getLocalMdibAccess().writeDescription(
                baseTreeModificationsSet.createFullyPopulatedTree());

        testDevice.startAsync().awaitRunning(WAIT_IN_SECONDS, WAIT_TIME_UNIT);
        testClient.startAsync().awaitRunning(WAIT_IN_SECONDS, WAIT_TIME_UNIT);

        hostingServiceProxy = testClient.getClient().connect(testDevice.getSdcDevice().getEprAddress())
                .get(WAIT_IN_SECONDS, WAIT_TIME_UNIT);

        objectFactory = new ObjectFactory();
        wseFactory = new org.somda.sdc.dpws.soap.wseventing.model.ObjectFactory();
    }

    @AfterEach
    void afterEach(TestInfo testInfo) {
        LOG.info("Done with test case {}", testInfo.getDisplayName());
        testDevice.stopAsync().awaitTerminated();
        testClient.stopAsync().awaitTerminated();
    }

    @Test
    void testHistoricalDataSubscribeVersionBased() throws Exception {
        int COUNT = 202;
        int END_VERSION = 203;
        // apply modifications so changeSequence with reports is generated
        applyModifications(COUNT);

        // subscribe to the history service and try to get reports
        SettableFuture<List<ChangeSequenceReportType>> notificationFuture = SettableFuture.create();
        var historyService = hostingServiceProxy.getHostedServices().get(SERVICE_HISTORY);
        assertNotNull(historyService);
        ListenableFuture<SubscribeResult> subscribe = historyService.getEventSinkAccess().subscribe(
                createFilterType(BigInteger.valueOf(END_VERSION)),
                null,
                new Interceptor() {
                    List<ChangeSequenceReportType> receivedNotifications = new ArrayList<>();

                    @MessageInterceptor(value = ACTION_HISTORY_MDIB_REPORT)
                    void onNotification(NotificationObject message) {
                        //notificationFuture.set(List.of(convertBodyToReport(message)));
                        receivedNotifications.add(convertBodyToReport(message));

                        if (receivedNotifications.size() == END_VERSION / 100 + 1) {
                            notificationFuture.set(receivedNotifications);
                        }
                    }
                });

        subscribe.get(WAIT_IN_SECONDS, WAIT_TIME_UNIT);

        var notifications =
                notificationFuture.get(WAIT_IN_SECONDS, WAIT_TIME_UNIT);

        // should receive 3 notification since limit it 100 reports per notification and there are 203 in total
        assertEquals(3, notifications.size());
        assertEquals(1, notifications.get(0).getChangeSequence().size());
        // only first notification contains historic MDIB
        assertNotNull(notifications.get(0).getChangeSequence().get(0).getHistoricMdib());
        assertNull(notifications.get(1).getChangeSequence().get(0).getHistoricMdib());
        assertNull(notifications.get(2).getChangeSequence().get(0).getHistoricMdib());

        var allReports = new ArrayList<>();
        notifications.forEach(msg -> allReports.addAll(msg.getChangeSequence().get(0).getHistoricReport()));
        assertEquals(END_VERSION, allReports.size());

        // check if subscription ended after all reports was sent
        assertTrue(subscribe.isDone());
    }

    private ChangeSequenceReportType convertBodyToReport(NotificationObject message) {
        return soapUtil.getBody(message.getNotification(), ChangeSequenceReportType.class)
                .orElseThrow(
                        () -> new RuntimeException("ChangeSequenceReportType could not be converted"));
    }

    private void applyModifications(int count) {
        var mdibAccess = testDevice.getSdcDevice().getLocalMdibAccess();
        for (int i = 0; i < count; ++i) {
            assertDoesNotThrow(() -> {
                var state = mdibAccess.getState(Handles.METRIC_0, NumericMetricState.class);

                var clone = (NumericMetricState) state.get().clone();
                clone.getMetricValue().setValue(clone.getMetricValue().getValue().add(BigDecimal.ONE));

                var modifications = MdibStateModifications.create(
                        MdibStateModifications.Type.METRIC).add(clone);
                mdibAccess.writeStates(modifications);

                Thread.sleep(10);
            });
        }
    }

    private FilterType createFilterType(BigInteger endVersion) {
        var filterType = wseFactory.createFilterType();
        filterType.setDialect(WS_EVENTING_HISTORY_DIALECT);
        filterType.setContent(Collections.singletonList(createHistoryQuery(endVersion)));
        return filterType;
    }

    private JAXBElement<HistoryQueryType> createHistoryQuery(BigInteger endVersion) {
        var queryType = objectFactory.createHistoryQueryType();
        queryType.setVersionRange(createVersionRangeQuery(endVersion));
        return objectFactory.createHistoryQuery(queryType);
    }

    private VersionRangeType createVersionRangeQuery(BigInteger endVersion) {
        var versionRange = objectFactory.createVersionRangeType();
        versionRange.setSequenceId(testDevice.getSdcDevice().getLocalMdibAccess().getMdibVersion().getSequenceId());
        versionRange.setInstanceId(BigInteger.ZERO);
        versionRange.setStartVersion(BigInteger.ZERO);
        versionRange.setEndVersion(endVersion);
        return versionRange;
    }
}
