package org.somda.sdc.glue.provider.services.helper;

import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.inject.Injector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.somda.sdc.biceps.common.MdibEntity;
import org.somda.sdc.biceps.common.MdibTypeValidator;
import org.somda.sdc.biceps.common.access.MdibAccess;
import org.somda.sdc.biceps.common.event.*;
import org.somda.sdc.biceps.common.factory.MdibEntityFactory;
import org.somda.sdc.biceps.model.message.*;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.biceps.testutil.Handles;
import org.somda.sdc.biceps.testutil.MockModelFactory;
import org.somda.sdc.dpws.device.EventSourceAccess;
import org.somda.sdc.glue.UnitTestUtil;
import org.somda.sdc.glue.common.ActionConstants;
import org.somda.sdc.glue.provider.services.helper.factory.ReportGeneratorFactory;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReportGeneratorTest {
    private static final UnitTestUtil IT = new UnitTestUtil();

    private Injector injector;
    private ReportGenerator reportGenerator;
    private EventSourceAccess eventSourceAccess;
    private EventBus eventBus;
    private MdibAccess mdibAccess;
    private MdibVersion expectedMdibVersion;

    @Captor
    private ArgumentCaptor<String> actualAction;
    @Captor
    private ArgumentCaptor<Object> actualReport;

    private List<AbstractAlertState> expectedAlertStates;
    private List<AbstractDeviceComponentState> expectedComponentStates;
    private List<AbstractContextState> expectedContextStates;
    private List<AbstractMetricState> expectedMetricStates;
    private List<AbstractOperationState> expectedOperationStates;

    @BeforeEach
    void beforeEach() {
        injector = IT.getInjector();

        expectedMdibVersion = MdibVersion.create();
        mdibAccess = mock(MdibAccess.class);
        when(mdibAccess.getMdibVersion()).thenReturn(expectedMdibVersion);

        eventSourceAccess = mock(EventSourceAccess.class);
        reportGenerator = injector.getInstance(ReportGeneratorFactory.class).createReportGenerator(eventSourceAccess);
        eventBus = injector.getInstance(EventBus.class);
        eventBus.register(reportGenerator);

        actualAction = ArgumentCaptor.forClass(String.class);
        actualReport = ArgumentCaptor.forClass(Object.class);

        expectedAlertStates = List.of(
                MockModelFactory.createState(Handles.ALERTCONDITION_0, AlertConditionState.class),
                MockModelFactory.createState(Handles.ALERTCONDITION_1, AlertConditionState.class),
                MockModelFactory.createState(Handles.ALERTSYSTEM_0, AlertSystemState.class)
        );
        expectedComponentStates = List.of(
                MockModelFactory.createState(Handles.MDS_0, MdsState.class),
                MockModelFactory.createState(Handles.VMD_0, VmdState.class),
                MockModelFactory.createState(Handles.BATTERY_0, BatteryState.class)
        );
        expectedContextStates = List.of(
                MockModelFactory.createState(Handles.CONTEXT_0, PatientContextState.class),
                MockModelFactory.createState(Handles.CONTEXT_1, LocationContextState.class),
                MockModelFactory.createState(Handles.CONTEXT_2, EnsembleContextState.class)
        );
        expectedMetricStates = List.of(
                MockModelFactory.createState(Handles.METRIC_0, NumericMetricState.class),
                MockModelFactory.createState(Handles.METRIC_1, StringMetricState.class),
                MockModelFactory.createState(Handles.METRIC_2, EnumStringMetricState.class)
        );
        expectedOperationStates = List.of(
                MockModelFactory.createState(Handles.OPERATION_0, ActivateOperationState.class),
                MockModelFactory.createState(Handles.OPERATION_1, SetStringOperationState.class),
                MockModelFactory.createState(Handles.OPERATION_2, SetComponentStateOperationState.class)
        );
    }

    @Test
    void sendPeriodicReport() throws Exception {
        {
            reportGenerator.sendPeriodicStateReport(expectedAlertStates, expectedMdibVersion);
            testStateReport(
                    ActionConstants.ACTION_PERIODIC_ALERT_REPORT,
                    expectedAlertStates,
                    PeriodicAlertReport.class,
                    "getAlertState");
        }
        {
            reset(eventSourceAccess);
            reportGenerator.sendPeriodicStateReport(expectedComponentStates, expectedMdibVersion);
            testStateReport(
                    ActionConstants.ACTION_PERIODIC_COMPONENT_REPORT,
                    expectedComponentStates,
                    PeriodicComponentReport.class,
                    "getComponentState");
        }
        {
            reset(eventSourceAccess);
            reportGenerator.sendPeriodicStateReport(expectedContextStates, expectedMdibVersion);
            testStateReport(
                    ActionConstants.ACTION_PERIODIC_CONTEXT_REPORT,
                    expectedContextStates,
                    PeriodicContextReport.class,
                    "getContextState");
        }
        {
            reset(eventSourceAccess);
            reportGenerator.sendPeriodicStateReport(expectedMetricStates, expectedMdibVersion);
            testStateReport(
                    ActionConstants.ACTION_PERIODIC_METRIC_REPORT,
                    expectedMetricStates,
                    PeriodicMetricReport.class,
                    "getMetricState");
        }
        {
            reset(eventSourceAccess);
            reportGenerator.sendPeriodicStateReport(expectedOperationStates, expectedMdibVersion);
            testStateReport(
                    ActionConstants.ACTION_PERIODIC_OPERATIONAL_STATE_REPORT,
                    expectedOperationStates,
                    PeriodicOperationalStateReport.class,
                    "getOperationState");
        }
    }

    @Test
    void onAlertChange() throws Exception {
        eventBus.post(new AlertStateModificationMessage(mdibAccess, expectedAlertStates));
        testStateReport(
                ActionConstants.ACTION_EPISODIC_ALERT_REPORT,
                expectedAlertStates,
                EpisodicAlertReport.class,
                "getAlertState");
    }

    @Test
    void onComponentChange() throws Exception {
        eventBus.post(new ComponentStateModificationMessage(mdibAccess, expectedComponentStates));
        testStateReport(
                ActionConstants.ACTION_EPISODIC_COMPONENT_REPORT,
                expectedComponentStates,
                EpisodicComponentReport.class,
                "getComponentState");
    }

    @Test
    void onContextChange() throws Exception {
        eventBus.post(new ContextStateModificationMessage(mdibAccess, expectedContextStates));
        testStateReport(
                ActionConstants.ACTION_EPISODIC_CONTEXT_REPORT,
                expectedContextStates,
                EpisodicContextReport.class,
                "getContextState");
    }

    @Test
    void onMetricChange() throws Exception {
        eventBus.post(new MetricStateModificationMessage(mdibAccess, expectedMetricStates));
        testStateReport(
                ActionConstants.ACTION_EPISODIC_METRIC_REPORT,
                expectedMetricStates,
                EpisodicMetricReport.class,
                "getMetricState");
    }

    @Test
    void onOperationChange() throws Exception {
        eventBus.post(new OperationStateModificationMessage(mdibAccess, expectedOperationStates));
        testStateReport(
                ActionConstants.ACTION_EPISODIC_OPERATIONAL_STATE_REPORT,
                expectedOperationStates,
                EpisodicOperationalStateReport.class,
                "getOperationState");
    }

    @Test
    void onDescriptionChangeWithAllStateTypes() throws Exception {
        String anyParent = "any-parent";
        List<MdibEntity> insertedEntities = Arrays.asList(
                entity(anyParent, MdsDescriptor.class),
                entity(anyParent, VmdDescriptor.class),
                entity(anyParent, AlertSystemDescriptor.class),
                entity(anyParent, RealTimeSampleArrayMetricDescriptor.class),
                entity(anyParent, ActivateOperationDescriptor.class),
                entity(anyParent, LocationContextDescriptor.class));

        eventBus.post(new DescriptionModificationMessage(mdibAccess, insertedEntities, Collections.EMPTY_LIST, Collections.EMPTY_LIST));

        verify(eventSourceAccess, times(6))
                .sendNotification(actualAction.capture(), actualReport.capture());

        for (Object value : actualReport.getAllValues()) {
            final AbstractReport report = AbstractReport.class.cast(value);
            assertEquals(expectedMdibVersion.getVersion(), report.getMdibVersion());
            assertEquals(expectedMdibVersion.getInstanceId(), report.getInstanceId());
            assertEquals(expectedMdibVersion.getSequenceId().toString(), report.getSequenceId());
            assertEquals(expectedMdibVersion.getSequenceId().toString(), report.getSequenceId());
        }

        assertEquals(DescriptionModificationReport.class, actualReport.getAllValues().get(0).getClass());
        DescriptionModificationReport descrReport = (DescriptionModificationReport) actualReport.getAllValues().get(0);
        assertEquals(insertedEntities.size(), descrReport.getReportPart().size());

        for (int i = 0; i < insertedEntities.size(); ++i) {
            testReportPart(descrReport.getReportPart().get(i), insertedEntities.get(i).getDescriptor().getClass());
        }

        Set<Class<? extends AbstractReport>> classes = Sets.newHashSet(
                DescriptionModificationReport.class,
                EpisodicComponentReport.class,
                EpisodicAlertReport.class,
                EpisodicMetricReport.class,
                EpisodicOperationalStateReport.class,
                EpisodicContextReport.class);

        final List<Object> allReports = actualReport.getAllValues();
        for (Object report : allReports) {
            assertTrue(classes.remove(report.getClass()));
        }
        assertEquals(0, classes.size());
    }

    @Test
    void onDescriptionChangeWithPartialStateTypes() throws Exception {
        List<MdibEntity> insertedEntities = Arrays.asList(
                entity(MdsDescriptor.class),
                entity(VmdDescriptor.class),
                entity(RealTimeSampleArrayMetricDescriptor.class),
                entity(ActivateOperationDescriptor.class));

        eventBus.post(new DescriptionModificationMessage(mdibAccess, insertedEntities, Collections.EMPTY_LIST, Collections.emptyList()));

        verify(eventSourceAccess, times(4))
                .sendNotification(actualAction.capture(), actualReport.capture());

        for (Object value : actualReport.getAllValues()) {
            final AbstractReport report = AbstractReport.class.cast(value);
            assertEquals(expectedMdibVersion.getVersion(), report.getMdibVersion());
            assertEquals(expectedMdibVersion.getInstanceId(), report.getInstanceId());
            assertEquals(expectedMdibVersion.getSequenceId().toString(), report.getSequenceId());
            assertEquals(expectedMdibVersion.getSequenceId().toString(), report.getSequenceId());
        }

        assertTrue(actualReport.getAllValues().get(0) instanceof DescriptionModificationReport);
        DescriptionModificationReport descrReport = (DescriptionModificationReport) actualReport.getAllValues().get(0);
        assertEquals(insertedEntities.size(), descrReport.getReportPart().size());

        for (int i = 0; i < insertedEntities.size(); ++i) {
            testReportPart(descrReport.getReportPart().get(i), insertedEntities.get(i).getDescriptor().getClass());
        }

        Set<Class<? extends AbstractReport>> classes = Sets.newHashSet(
                DescriptionModificationReport.class,
                EpisodicComponentReport.class,
                EpisodicMetricReport.class,
                EpisodicOperationalStateReport.class);

        final List<Object> allReports = actualReport.getAllValues();
        for (Object report : allReports) {
            assertTrue(classes.remove(report.getClass()));
        }
        assertEquals(0, classes.size());
    }

    @Test
    void onDescriptionChangeWithInsertedUpdatedDeleted() throws Exception {
        List<MdibEntity> insertedEntities = Arrays.asList(
                entity(MdsDescriptor.class),
                entity(VmdDescriptor.class),
                entity(RealTimeSampleArrayMetricDescriptor.class),
                entity(ActivateOperationDescriptor.class));

        List<MdibEntity> updatedEntities = Arrays.asList(
                entity(AlertSystemDescriptor.class),
                entity(ActivateOperationDescriptor.class));

        List<MdibEntity> deletedEntities = Arrays.asList(
                entity(ActivateOperationDescriptor.class),
                entity(PatientContextDescriptor.class));

        eventBus.post(new DescriptionModificationMessage(mdibAccess, insertedEntities, updatedEntities, deletedEntities));

        verify(eventSourceAccess, times(5))
                .sendNotification(actualAction.capture(), actualReport.capture());

        assertTrue(actualReport.getAllValues().get(0) instanceof DescriptionModificationReport);
        DescriptionModificationReport descrReport = (DescriptionModificationReport) actualReport.getAllValues().get(0);
        assertEquals(insertedEntities.size() + updatedEntities.size() + deletedEntities.size(),
                descrReport.getReportPart().size());

        for (int i = 0; i < deletedEntities.size(); ++i) {
            testReportPart(descrReport.getReportPart().get(i), deletedEntities.get(i).getDescriptor().getClass());
        }

        int offset = deletedEntities.size();

        for (int i = 0; i < insertedEntities.size(); ++i) {
            testReportPart(descrReport.getReportPart().get(offset + i), insertedEntities.get(i).getDescriptor().getClass());
        }

        offset += insertedEntities.size();

        for (int i = 0; i < updatedEntities.size(); ++i) {
            testReportPart(descrReport.getReportPart().get(offset + i), updatedEntities.get(i).getDescriptor().getClass());
        }

        // Expect no context state updates as contexts have been deleted
        Set<Class<? extends AbstractReport>> classes = Sets.newHashSet(
                DescriptionModificationReport.class,
                EpisodicComponentReport.class,
                EpisodicMetricReport.class,
                EpisodicOperationalStateReport.class,
                EpisodicAlertReport.class);

        final List<Object> allReports = actualReport.getAllValues();
        for (Object report : allReports) {
            assertTrue(classes.remove(report.getClass()));
        }
        assertEquals(0, classes.size());
    }

    @Test
    void onWaveformChange() throws Exception {
        final List<RealTimeSampleArrayMetricState> expectedStates = Arrays.asList(
                MockModelFactory.createState(Handles.METRIC_0, RealTimeSampleArrayMetricState.class),
                MockModelFactory.createState(Handles.METRIC_1, RealTimeSampleArrayMetricState.class)
        );
        eventBus.post(new WaveformStateModificationMessage(mdibAccess, expectedStates));
        AbstractReport abstractReport = testStateReportHeader(ActionConstants.ACTION_WAVEFORM_STREAM, WaveformStream.class);
        assertEquals(expectedStates, ((WaveformStream) abstractReport).getState());
    }

    private void testStateReport(String expectedAction,
                                 List<? extends AbstractState> expectedStates,
                                 Class<? extends AbstractReport> reportClass,
                                 String getStatesMethodName) throws Exception {
        testStateReportHeader(expectedAction, reportClass);
        testStateReportBody(expectedStates, reportClass, getStatesMethodName);
    }

    private AbstractReport testStateReportHeader(String expectedAction, Class<? extends AbstractReport> reportClass) throws Exception {
        verify(eventSourceAccess).sendNotification(actualAction.capture(), actualReport.capture());

        assertEquals(expectedAction, actualAction.getValue());
        assertTrue(reportClass.isAssignableFrom(actualReport.getValue().getClass()));

        final AbstractReport actualNotification = reportClass.cast(actualReport.getValue());
        assertEquals(expectedMdibVersion.getVersion(), actualNotification.getMdibVersion());
        assertEquals(expectedMdibVersion.getInstanceId(), actualNotification.getInstanceId());
        assertEquals(expectedMdibVersion.getSequenceId(), actualNotification.getSequenceId());
        return actualNotification;
    }

    private void testStateReportBody(List<? extends AbstractState> expectedStates,
                                     Class<? extends AbstractReport> reportClass,
                                     String getStatesMethodName) throws Exception {
        final AbstractReport actualNotification = reportClass.cast(actualReport.getValue());
        final Method getReportPart = actualNotification.getClass().getMethod("getReportPart");
        final List<?> reportParts = ((List) getReportPart.invoke(actualNotification));
        assertEquals(1, reportParts.size());
        assertNull(reportParts.get(0).getClass().getMethod("getSourceMds").invoke(reportParts.get(0)));
        assertEquals(expectedStates, reportParts.get(0).getClass().getMethod(getStatesMethodName).invoke(reportParts.get(0)));
    }

    private void testReportPart(DescriptionModificationReport.ReportPart reportPart,
                                Class<? extends AbstractDescriptor> descriptorType) {
        assertEquals(1, reportPart.getDescriptor().size());
        assertEquals(1, reportPart.getState().size());
        assertTrue(descriptorType.isAssignableFrom(reportPart.getDescriptor().get(0).getClass()));
    }

    private MdibEntity entity(Class<? extends AbstractDescriptor> theClass) throws Exception {
        return entity(null, theClass);
    }

    private MdibEntity entity(@Nullable String parentHandle, Class<? extends AbstractDescriptor> theClass) throws Exception {
        final MdibTypeValidator typeValidator = injector.getInstance(MdibTypeValidator.class);
        final MdibEntityFactory factory = injector.getInstance(MdibEntityFactory.class);
        return factory.createMdibEntity(
                parentHandle,
                Collections.emptyList(),
                theClass.getConstructor().newInstance(),
                Collections.singletonList(typeValidator.resolveStateType(theClass).getConstructor().newInstance()),
                MdibVersion.create());
    }
}