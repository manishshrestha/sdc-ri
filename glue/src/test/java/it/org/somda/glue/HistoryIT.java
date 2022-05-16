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
import org.somda.sdc.biceps.model.participant.NumericMetricState;
import org.somda.sdc.biceps.testutil.BaseTreeModificationsSet;
import org.somda.sdc.biceps.testutil.Handles;
import org.somda.sdc.biceps.testutil.MockEntryFactory;
import org.somda.sdc.dpws.device.DeviceConfig;
import org.somda.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.interception.Interceptor;
import org.somda.sdc.dpws.soap.interception.MessageInterceptor;
import org.somda.sdc.dpws.soap.interception.NotificationObject;
import org.somda.sdc.dpws.soap.wseventing.SubscribeResult;
import test.org.somda.common.CIDetector;
import test.org.somda.common.LoggingTestWatcher;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    @BeforeEach
    void beforeEach(TestInfo testInfo) throws Exception {
        LOG.info("Running test case {}", testInfo.getDisplayName());

        IT.getInjector().injectMembers(new DefaultDpwsConfigModule() {
            @Override
            public void customConfigure() {
                bind(DeviceConfig.HISTORY_SERVICE_SUPPORT, Boolean.class, true);
            }
        });

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
    }

    @AfterEach
    void afterEach(TestInfo testInfo) {
        LOG.info("Done with test case {}", testInfo.getDisplayName());
        testDevice.stopAsync().awaitTerminated();
        testClient.stopAsync().awaitTerminated();
    }

    @Test
    void testHistoryServiceSubscribe() throws Exception {
        int COUNT = 2;
        SettableFuture<List<ChangeSequenceReportType>> notificationFuture = SettableFuture.create();
        var historyService = hostingServiceProxy.getHostedServices().get(SERVICE_HISTORY);
        assertNotNull(historyService);
        ListenableFuture<SubscribeResult> subscribe = historyService.getEventSinkAccess().subscribe(
                List.of(ACTION_HISTORY_MDIB_REPORT), null,
                new Interceptor() {
                    private final List<ChangeSequenceReportType> receivedNotifications = new ArrayList<>();

                    @MessageInterceptor(value = ACTION_HISTORY_MDIB_REPORT)
                    void onNotification(NotificationObject message) {
                        receivedNotifications.add(convertBodyToReport(message));

                        if (receivedNotifications.size() == COUNT) {
                            notificationFuture.set(receivedNotifications);
                        }
                    }
                });

        subscribe.get(WAIT_IN_SECONDS, WAIT_TIME_UNIT);

        // TODO: NOW we should do some modifications to the MDIB so reports are triggered and send to subscriber.
        applyModifications(COUNT);

        final List<ChangeSequenceReportType> notifications = notificationFuture.get(WAIT_IN_SECONDS, WAIT_TIME_UNIT);
        assertEquals(COUNT, notifications.size());
    }

    private ChangeSequenceReportType convertBodyToReport(NotificationObject message) {
        return soapUtil.getBody(message.getNotification(), ChangeSequenceReportType.class)
                .orElseThrow(
                        () -> new RuntimeException("ChangeSequenceReportType could not be converted"));
    }

    private void applyModifications(int count) {
        var mdibAccess = testDevice.getSdcDevice().getLocalMdibAccess();
        new Thread(() -> {
            for (int i = 0; i < count; ++i) {
                assertDoesNotThrow(() -> {
                    var state = mdibAccess.getState(Handles.METRIC_0, NumericMetricState.class);

                    var clone = (NumericMetricState) state.get().clone();
                    clone.getMetricValue().setValue(clone.getMetricValue().getValue().add(BigDecimal.ONE));

                    var modifications = MdibStateModifications.create(
                            MdibStateModifications.Type.METRIC).add(clone);
                    mdibAccess.writeStates(modifications);

                    Thread.sleep(100);
                });
            }
        }).start();
    }
}
