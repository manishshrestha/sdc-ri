package it.org.somda.glue;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.ListenableFuture;
import it.org.somda.glue.consumer.ReportListenerSpy;
import it.org.somda.glue.consumer.TestSdcClient;
import it.org.somda.glue.provider.TestSdcDevice;
import it.org.somda.glue.provider.VentilatorMdibRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.somda.sdc.biceps.common.event.AbstractMdibAccessMessage;
import org.somda.sdc.biceps.common.event.AlertStateModificationMessage;
import org.somda.sdc.biceps.common.event.MetricStateModificationMessage;
import org.somda.sdc.biceps.common.storage.PreprocessingException;
import org.somda.sdc.biceps.model.message.*;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.biceps.testutil.MdibAccessObserverSpy;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.glue.common.MdibXmlIo;
import org.somda.sdc.glue.common.factory.ModificationsBuilderFactory;
import org.somda.sdc.glue.consumer.ConnectConfiguration;
import org.somda.sdc.glue.consumer.SdcRemoteDevice;
import org.somda.sdc.glue.consumer.sco.ScoTransaction;
import org.somda.sdc.glue.provider.sco.Context;
import org.somda.sdc.glue.provider.sco.IncomingSetServiceRequest;
import org.somda.sdc.glue.provider.sco.InvocationResponse;
import org.somda.sdc.glue.provider.sco.OperationInvocationReceiver;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CommunicationIT {
    private static final IntegrationTestUtil IT = new IntegrationTestUtil();

    private TestSdcDevice testDevice;
    private TestSdcClient testClient;

    private static final int WAIT_IN_SECONDS = 5000;
    private static final TimeUnit WAIT_TIME_UNIT = TimeUnit.SECONDS;
    private static final Duration WAIT_DURATION = Duration.of(WAIT_IN_SECONDS, TimeUnit.SECONDS.toChronoUnit());
    private VentilatorMdibRunner ventilatorMdibRunner;

    @BeforeEach
    void beforeEach() {
        testDevice = new TestSdcDevice(Collections.singletonList(new OperationInvocationReceiver() {
            @IncomingSetServiceRequest(operationHandle = VentilatorMdibRunner.HANDLE_SET_MDC_DEV_SYS_PT_VENT_VMD)
            public InvocationResponse onSet(Context context, String requestedValue) throws PreprocessingException {
                Optional<VentilatorMdibRunner.VentilatorMode> match = Optional.empty();
                for (VentilatorMdibRunner.VentilatorMode value : VentilatorMdibRunner.VentilatorMode.values()) {
                    if (requestedValue.equals(value.getModeValue())) {
                        match = Optional.of(value);
                        break;
                    }
                }

                if (match.isEmpty()) {
                    context.sendUnsuccessfulReport(InvocationState.FAIL, InvocationError.OTH, Collections.emptyList());
                    return context.createUnsuccessfulResponse(InvocationState.FAIL, InvocationError.OTH, Collections.emptyList());
                }

                context.sendSuccessfulReport(InvocationState.WAIT);
                context.sendSuccessfulReport(InvocationState.START);
                ventilatorMdibRunner.changeMetrics(match.get(), null);
                context.sendSuccessfulReport(InvocationState.FIN);
                return context.createSuccessfulResponse(InvocationState.FIN);
            }
        }));
        testClient = new TestSdcClient();
        ventilatorMdibRunner = new VentilatorMdibRunner(
                IT.getInjector().getInstance(MdibXmlIo.class),
                IT.getInjector().getInstance(ModificationsBuilderFactory.class),
                testDevice.getSdcDevice().getMdibAccess());
    }

    @Test
    void connectOneClientAndTransferData() throws Exception {
        ventilatorMdibRunner.startAsync().awaitRunning();
        testDevice.startAsync().awaitRunning();
        testClient.startAsync().awaitRunning();

        final ListenableFuture<HostingServiceProxy> hostingServiceFuture = testClient.getClient()
                .connect(testDevice.getSdcDevice().getEprAddress());
        final HostingServiceProxy hostingServiceProxy = hostingServiceFuture.get(WAIT_IN_SECONDS, WAIT_TIME_UNIT);

        final ListenableFuture<SdcRemoteDevice> remoteDeviceFuture = testClient.getConnector().connect(hostingServiceProxy,
                ConnectConfiguration.create(ConnectConfiguration.ALL_EPISODIC_AND_WAVEFORM_REPORTS));

        final SdcRemoteDevice sdcRemoteDevice = remoteDeviceFuture.get(WAIT_IN_SECONDS, WAIT_TIME_UNIT);
        final MdibAccessObserverSpy mdibSpy = new MdibAccessObserverSpy();

        sdcRemoteDevice.getMdibAccessObservable().registerObserver(mdibSpy);

        {
            mdibSpy.reset();
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
            mdibSpy.reset();
            ventilatorMdibRunner.changeAlertsPresence(true);

            assertTrue(mdibSpy.waitForNumberOfRecordedMessages(1, WAIT_DURATION));
            final AbstractMdibAccessMessage recordedMessage = mdibSpy.getRecordedMessages().get(0);
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

        {
            mdibSpy.reset();

            ReportListenerSpy reportListenerSpy = new ReportListenerSpy();

            SetString setString = new SetString();
            setString.setOperationHandleRef(VentilatorMdibRunner.HANDLE_SET_MDC_DEV_SYS_PT_VENT_VMD);
            setString.setRequestedStringValue(VentilatorMdibRunner.VentilatorMode.VENT_MODE_SIMV.getModeValue());
            final ListenableFuture<ScoTransaction<SetStringResponse>> scoFuture = sdcRemoteDevice.getSetServiceAccess()
                    .invoke(setString, reportListenerSpy, SetStringResponse.class);
            final ScoTransaction<SetStringResponse> scoTransaction = scoFuture.get(WAIT_IN_SECONDS, WAIT_TIME_UNIT);
            final List<OperationInvokedReport.ReportPart> reportParts = scoTransaction.waitForFinalReport(WAIT_DURATION);
            assertTrue(!reportParts.isEmpty());

            assertTrue(reportListenerSpy.waitForReports(3, WAIT_DURATION));
            assertEquals(InvocationState.WAIT, reportListenerSpy.getReports().get(0).getInvocationInfo().getInvocationState());
            assertEquals(InvocationState.START, reportListenerSpy.getReports().get(1).getInvocationInfo().getInvocationState());
            assertEquals(InvocationState.FIN, reportListenerSpy.getReports().get(2).getInvocationInfo().getInvocationState());

            assertTrue(mdibSpy.waitForNumberOfRecordedMessages(1, WAIT_DURATION));
            final AbstractMdibAccessMessage recordedMessage = mdibSpy.getRecordedMessages().get(0);
            assertTrue(recordedMessage instanceof MetricStateModificationMessage);
            MetricStateModificationMessage metricStateMessage = (MetricStateModificationMessage) recordedMessage;
            assertEquals(1, metricStateMessage.getStates().size());
            final AbstractMetricState abstractMetricState = metricStateMessage.getStates().get(0);
            assertTrue(abstractMetricState instanceof EnumStringMetricState);
            EnumStringMetricState stringMetricState = (EnumStringMetricState) abstractMetricState;
            assertEquals(VentilatorMdibRunner.VentilatorMode.VENT_MODE_SIMV.getModeValue(),
                    stringMetricState.getMetricValue().getValue());
        }

        {
            for (int i = 0; i < 100; ++i) {
                mdibSpy.reset();
                ventilatorMdibRunner.changeMetrics(null, BigDecimal.valueOf(i));

                assertTrue(mdibSpy.waitForNumberOfRecordedMessages(1, WAIT_DURATION));
                final AbstractMdibAccessMessage recordedMessage = mdibSpy.getRecordedMessages().get(0);
                assertTrue(recordedMessage instanceof MetricStateModificationMessage);
                MetricStateModificationMessage metricStateMessage = (MetricStateModificationMessage) recordedMessage;
                assertEquals(1, metricStateMessage.getStates().size());
                final AbstractMetricState abstractMetricState = metricStateMessage.getStates().get(0);
                assertTrue(abstractMetricState instanceof NumericMetricState);
                NumericMetricState numericMetricState = (NumericMetricState) abstractMetricState;
                assertEquals(BigDecimal.valueOf(i), numericMetricState.getMetricValue().getValue());
            }
        }
    }

    @Test
    void connectMultipleClientsAndTransferData() throws Exception {
        int numberMessages = 100;
        int numberClients = 10;


        ventilatorMdibRunner.startAsync().awaitRunning();
        testDevice.startAsync().awaitRunning();

        List<TestSdcClient> testSdcClients = new ArrayList<>(numberClients);
        List<MdibAccessObserverSpy> mdibSpies = new ArrayList<>(numberClients);
        for (int i = 0; i < numberClients; ++i) {
            final TestSdcClient testSdcClient = new TestSdcClient();
            testSdcClients.add(testSdcClient);
            testSdcClient.startAsync();
        }

        testSdcClients.parallelStream().forEach(AbstractIdleService::awaitRunning);

        List<ListenableFuture<SdcRemoteDevice>> futures = new ArrayList<>(numberClients);
        testSdcClients.parallelStream().forEach(client -> {
            try {
                final ListenableFuture<HostingServiceProxy> hostingServiceFuture = client.getClient()
                        .connect(testDevice.getSdcDevice().getEprAddress());
                final HostingServiceProxy hostingServiceProxy = hostingServiceFuture.get(WAIT_IN_SECONDS, WAIT_TIME_UNIT);

                futures.add(client.getConnector().connect(hostingServiceProxy,
                        ConnectConfiguration.create(ConnectConfiguration.ALL_EPISODIC_AND_WAVEFORM_REPORTS)));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        for (int i = 0; i < numberClients; ++i) {
            final SdcRemoteDevice sdcRemoteDevice = futures.get(i).get(WAIT_IN_SECONDS, WAIT_TIME_UNIT);
            sdcRemoteDevice.getMdibAccessObservable().registerObserver(mdibSpies.get(i));
        }

        for (int j = 0; j < numberMessages; ++j) {
            ventilatorMdibRunner.changeMetrics(null, BigDecimal.valueOf(j));
        }

        mdibSpies.parallelStream().forEach(spy -> {
            assertTrue(spy.waitForNumberOfRecordedMessages(numberMessages, WAIT_DURATION));

            final List<AbstractMdibAccessMessage> recordedMessages = spy.getRecordedMessages();
            for (int j = 0; j < numberMessages; ++j) {
                final AbstractMdibAccessMessage recordedMessage = recordedMessages.get(j);
                assertTrue(recordedMessage instanceof MetricStateModificationMessage);
                MetricStateModificationMessage metricStateMessage = (MetricStateModificationMessage) recordedMessage;
                assertEquals(1, metricStateMessage.getStates().size());
                final AbstractMetricState abstractMetricState = metricStateMessage.getStates().get(0);
                assertTrue(abstractMetricState instanceof NumericMetricState);
                NumericMetricState numericMetricState = (NumericMetricState) abstractMetricState;
                assertEquals(BigDecimal.valueOf(j), numericMetricState.getMetricValue().getValue());
            }
        });
    }

    @Test
    void connectMultipleDevicesAndTransferData() throws Exception {
        int numberMessagesPerDevice = 100;
        int numberDevices = 10;

        testClient.startAsync().awaitRunning();

        List<TestSdcDevice> testSdcDevices = new ArrayList<>(numberDevices);
        List<MdibAccessObserverSpy> mdibSpies = new ArrayList<>(numberDevices);
        for (int i = 0; i < numberDevices; ++i) {
            TestSdcDevice testSdcDevice = new TestSdcDevice();
            testSdcDevices.add(testSdcDevice);
            testSdcDevice.startAsync();
            mdibSpies.add(new MdibAccessObserverSpy());
        }

        List<VentilatorMdibRunner> ventilatorMdibRunners = new ArrayList<>(numberDevices);
        testSdcDevices.parallelStream().forEach(testDevice -> {
            testDevice.awaitRunning();
            final VentilatorMdibRunner ventilatorMdibRunner = new VentilatorMdibRunner(
                    IT.getInjector().getInstance(MdibXmlIo.class),
                    IT.getInjector().getInstance(ModificationsBuilderFactory.class),
                    testDevice.getSdcDevice().getMdibAccess());
            ventilatorMdibRunners.add(ventilatorMdibRunner);
            ventilatorMdibRunner.startAsync().awaitRunning();
        });

        List<HostingServiceProxy> hostingServiceProxies = new ArrayList<>(numberDevices);
        testSdcDevices.parallelStream().forEach(testSdcDevice -> {
            final URI eprAddress = testSdcDevice.getSdcDevice().getEprAddress();
            try {
                final ListenableFuture<HostingServiceProxy> hostingServiceFuture = testClient.getClient()
                        .connect(eprAddress);
                hostingServiceProxies.add(hostingServiceFuture.get(WAIT_IN_SECONDS, WAIT_TIME_UNIT));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        List<ListenableFuture<SdcRemoteDevice>> futures = new ArrayList<>(numberDevices);
        hostingServiceProxies.parallelStream().forEach(hostingServiceProxy -> {
            try {
                futures.add(testClient.getConnector().connect(hostingServiceProxy,
                        ConnectConfiguration.create(ConnectConfiguration.ALL_EPISODIC_AND_WAVEFORM_REPORTS)));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        for (int i = 0; i < numberDevices; ++i) {
            final SdcRemoteDevice sdcRemoteDevice = futures.get(i).get(WAIT_IN_SECONDS, WAIT_TIME_UNIT);
            sdcRemoteDevice.getMdibAccessObservable().registerObserver(mdibSpies.get(i));
        }

        ventilatorMdibRunners.parallelStream().forEach(ventilatorMdibRunner -> {
            try {
                for (int j = 0; j < numberMessagesPerDevice; ++j) {
                    ventilatorMdibRunner.changeMetrics(null, BigDecimal.valueOf(j));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        mdibSpies.parallelStream().forEach(spy -> {
            assertTrue(spy.waitForNumberOfRecordedMessages(numberMessagesPerDevice, WAIT_DURATION));

            final List<AbstractMdibAccessMessage> recordedMessages = spy.getRecordedMessages();
            for (int j = 0; j < numberMessagesPerDevice; ++j) {
                final AbstractMdibAccessMessage recordedMessage = recordedMessages.get(j);
                assertTrue(recordedMessage instanceof MetricStateModificationMessage);
                MetricStateModificationMessage metricStateMessage = (MetricStateModificationMessage) recordedMessage;
                assertEquals(1, metricStateMessage.getStates().size());
                final AbstractMetricState abstractMetricState = metricStateMessage.getStates().get(0);
                assertTrue(abstractMetricState instanceof NumericMetricState);
                NumericMetricState numericMetricState = (NumericMetricState) abstractMetricState;
                assertEquals(BigDecimal.valueOf(j), numericMetricState.getMetricValue().getValue());
            }
        });
    }
}
