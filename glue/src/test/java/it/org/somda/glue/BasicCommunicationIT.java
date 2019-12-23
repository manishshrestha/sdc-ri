package it.org.somda.glue;

import com.google.common.util.concurrent.ListenableFuture;
import it.org.somda.glue.consumer.TestSdcClient;
import it.org.somda.glue.provider.TestSdcDevice;
import it.org.somda.glue.provider.VentilatorMdibRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.somda.sdc.biceps.common.event.AbstractMdibAccessMessage;
import org.somda.sdc.biceps.common.event.AlertStateModificationMessage;
import org.somda.sdc.biceps.common.event.MetricStateModificationMessage;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.biceps.testutil.MdibAccessObserverSpy;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.glue.common.MdibXmlIo;
import org.somda.sdc.glue.common.factory.ModificationsBuilderFactory;
import org.somda.sdc.glue.consumer.ConnectConfiguration;
import org.somda.sdc.glue.consumer.SdcRemoteDevice;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BasicCommunicationIT {
    private static final IntegrationTestUtil IT = new IntegrationTestUtil();

    private TestSdcDevice testDevice;
    private TestSdcClient testClient;

    private static final int WAIT_IN_SECONDS = 5000;
    private static final TimeUnit WAIT_TIME_UNIT = TimeUnit.SECONDS;
    private static final Duration WAIT_DURATION = Duration.of(WAIT_IN_SECONDS, TimeUnit.SECONDS.toChronoUnit());
    private VentilatorMdibRunner ventilatorMdibRunner;

    @BeforeEach
    void beforeEach() {
        testDevice = new TestSdcDevice();
        testClient = new TestSdcClient();
        ventilatorMdibRunner = new VentilatorMdibRunner(
                IT.getInjector().getInstance(MdibXmlIo.class),
                IT.getInjector().getInstance(ModificationsBuilderFactory.class),
                testDevice.getSdcDevice().getMdibAccess());
    }

    @Test
    void startDeviceAndConnectOneClient() throws Exception {
        ventilatorMdibRunner.startAsync().awaitRunning();
        testDevice.startAsync().awaitRunning();
        testClient.startAsync().awaitRunning();

        final ListenableFuture<HostingServiceProxy> hostingServiceFuture = testClient.getClient()
                .connect(testDevice.getSdcDevice().getEprAddress());
        final HostingServiceProxy hostingServiceProxy = hostingServiceFuture.get(WAIT_IN_SECONDS, WAIT_TIME_UNIT);

        final ListenableFuture<SdcRemoteDevice> remoteDeviceFuture = testClient.getConnector().connect(hostingServiceProxy,
                ConnectConfiguration.create(ConnectConfiguration.ALL_EPISODIC_AND_WAVEFORM_REPORTS));

        final SdcRemoteDevice sdcRemoteDevice = remoteDeviceFuture.get(WAIT_IN_SECONDS, WAIT_TIME_UNIT);
        MdibAccessObserverSpy mdibSpy = new MdibAccessObserverSpy();
        sdcRemoteDevice.getMdibAccessObservable().registerObserver(mdibSpy);

        {
            ventilatorMdibRunner.changeMetrics(VentilatorMdibRunner.VentilatorMode.VENT_MODE_INSPASSIST, null);

            assertTrue(mdibSpy.waitForNumberOfRecordedMessages(1, WAIT_DURATION));
            final AbstractMdibAccessMessage recordedMessage = mdibSpy.getRecordedMessages().get(0);
            assertTrue(recordedMessage instanceof MetricStateModificationMessage);
            MetricStateModificationMessage metricStateMessage = (MetricStateModificationMessage) recordedMessage;
            assertEquals(1, metricStateMessage.getStates().size());
            final AbstractMetricState abstractMetricState = metricStateMessage.getStates().get(0);
            assertTrue(abstractMetricState instanceof EnumStringMetricState);
            EnumStringMetricState stringMetricState = (EnumStringMetricState) abstractMetricState;
            assertEquals(VentilatorMdibRunner.VentilatorMode.VENT_MODE_INSPASSIST.getModeValue(),
                    stringMetricState.getMetricValue().getValue());
        }

        {
            ventilatorMdibRunner.changeAlertsPresence(true);

            assertTrue(mdibSpy.waitForNumberOfRecordedMessages(2, WAIT_DURATION));
            final AbstractMdibAccessMessage recordedMessage = mdibSpy.getRecordedMessages().get(1);
            assertTrue(recordedMessage instanceof AlertStateModificationMessage);
            AlertStateModificationMessage alertStateMessage = (AlertStateModificationMessage) recordedMessage;
            assertEquals(2, alertStateMessage.getStates().size());
            final AbstractAlertState abstractConditionState = alertStateMessage.getStates().get(0);
            assertTrue(abstractConditionState instanceof AlertConditionState);
            AlertConditionState conditionState = (AlertConditionState) abstractConditionState;
            assertEquals(true, conditionState.isPresence());

            final AbstractAlertState abstractSignalState = alertStateMessage.getStates().get(1);
            assertTrue(abstractSignalState instanceof AlertSignalState);
            AlertSignalState signalState = (AlertSignalState) abstractSignalState;
            assertEquals(AlertSignalPresence.ON, signalState.getPresence());
        }
    }
}
