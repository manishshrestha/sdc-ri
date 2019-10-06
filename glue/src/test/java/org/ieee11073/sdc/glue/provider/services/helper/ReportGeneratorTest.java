package org.ieee11073.sdc.glue.provider.services.helper;

import com.google.common.eventbus.EventBus;
import com.google.inject.Injector;
import org.ieee11073.sdc.biceps.common.MdibEntity;
import org.ieee11073.sdc.biceps.common.MdibTypeValidator;
import org.ieee11073.sdc.biceps.common.access.MdibAccess;
import org.ieee11073.sdc.biceps.common.event.*;
import org.ieee11073.sdc.biceps.common.factory.MdibEntityFactory;
import org.ieee11073.sdc.biceps.model.message.*;
import org.ieee11073.sdc.biceps.model.participant.*;
import org.ieee11073.sdc.biceps.testutil.Handles;
import org.ieee11073.sdc.biceps.testutil.MockModelFactory;
import org.ieee11073.sdc.dpws.device.EventSourceAccess;
import org.ieee11073.sdc.glue.UnitTestUtil;
import org.ieee11073.sdc.glue.common.ActionConstants;
import org.ieee11073.sdc.glue.provider.services.helper.factory.ReportGeneratorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
    }

    @Test
    void onAlertChange() throws Exception {
        final List<AbstractAlertState> expectedStates = Arrays.asList(
                MockModelFactory.createState(Handles.ALERTCONDITION_0, AlertConditionState.class),
                MockModelFactory.createState(Handles.ALERTCONDITION_1, AlertConditionState.class),
                MockModelFactory.createState(Handles.ALERTSYSTEM_0, AlertSystemState.class)
        );
        eventBus.post(new AlertStateModificationMessage(mdibAccess, expectedStates));
        testStateReport(
                ActionConstants.ACTION_EPISODIC_ALERT_REPORT,
                expectedStates,
                EpisodicAlertReport.class,
                "getAlertState");
    }

    @Test
    void onComponentChange() throws Exception {
        final List<AbstractDeviceComponentState> expectedStates = Arrays.asList(
                MockModelFactory.createState(Handles.MDS_0, MdsState.class),
                MockModelFactory.createState(Handles.VMD_0, VmdState.class),
                MockModelFactory.createState(Handles.BATTERY_0, BatteryState.class)
        );
        eventBus.post(new ComponentStateModificationMessage(mdibAccess, expectedStates));
        testStateReport(
                ActionConstants.ACTION_EPISODIC_COMPONENT_REPORT,
                expectedStates,
                EpisodicComponentReport.class,
                "getComponentState");
    }

    @Test
    void onContextChange() throws Exception {
        final List<AbstractContextState> expectedStates = Arrays.asList(
                MockModelFactory.createState(Handles.CONTEXT_0, PatientContextState.class),
                MockModelFactory.createState(Handles.CONTEXT_1, LocationContextState.class),
                MockModelFactory.createState(Handles.CONTEXT_2, EnsembleContextState.class)
        );
        eventBus.post(new ContextStateModificationMessage(mdibAccess, expectedStates));
        testStateReport(
                ActionConstants.ACTION_EPISODIC_CONTEXT_REPORT,
                expectedStates,
                EpisodicContextReport.class,
                "getContextState");
    }

    @Test
    void onMetricChange() throws Exception {
        final List<AbstractMetricState> expectedStates = Arrays.asList(
                MockModelFactory.createState(Handles.METRIC_0, NumericMetricState.class),
                MockModelFactory.createState(Handles.METRIC_1, StringMetricState.class),
                MockModelFactory.createState(Handles.METRIC_2, EnumStringMetricState.class)
        );
        eventBus.post(new MetricStateModificationMessage(mdibAccess, expectedStates));
        testStateReport(
                ActionConstants.ACTION_EPISODIC_METRIC_REPORT,
                expectedStates,
                EpisodicMetricReport.class,
                "getMetricState");
    }

    @Test
    void onOperationChange() throws Exception {
        final List<AbstractOperationState> expectedStates = Arrays.asList(
                MockModelFactory.createState(Handles.OPERATION_0, ActivateOperationState.class),
                MockModelFactory.createState(Handles.OPERATION_1, SetStringOperationState.class),
                MockModelFactory.createState(Handles.OPERATION_2, SetComponentStateOperationState.class)
        );
        eventBus.post(new OperationStateModificationMessage(mdibAccess, expectedStates));
        testStateReport(
                ActionConstants.ACTION_EPISODIC_OPERATIONAL_STATE_REPORT,
                expectedStates,
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

        eventBus.post(new DescriptionModificationMessage(mdibAccess, insertedEntities, Collections.EMPTY_LIST, Collections.emptyList()));

        verify(eventSourceAccess, times(6))
                .sendNotification(actualAction.capture(), actualReport.capture());

        for (Object value : actualReport.getAllValues()) {
            final AbstractReport report = AbstractReport.class.cast(value);
            assertEquals(expectedMdibVersion.getVersion(), report.getMdibVersion());
            assertEquals(expectedMdibVersion.getInstanceId(), report.getInstanceId());
            assertEquals(expectedMdibVersion.getSequenceId().toString(), report.getSequenceId());
            assertEquals(expectedMdibVersion.getSequenceId().toString(), report.getSequenceId());
        }
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
    }


    private void testStateReport(String expectedAction,
                                 List<? extends AbstractState> expectedStates,
                                 Class<? extends AbstractReport> reportClass,
                                 String getStatesMethodName) throws Exception {

        verify(eventSourceAccess).sendNotification(actualAction.capture(), actualReport.capture());

        assertEquals(expectedAction, actualAction.getValue());
        assertTrue(reportClass.isAssignableFrom(actualReport.getValue().getClass()));

        final AbstractReport actualNotification = reportClass.cast(actualReport.getValue());
        assertEquals(expectedMdibVersion.getVersion(), actualNotification.getMdibVersion());
        assertEquals(expectedMdibVersion.getInstanceId(), actualNotification.getInstanceId());
        assertEquals(expectedMdibVersion.getSequenceId().toString(), actualNotification.getSequenceId());

        final Method getReportPart = actualNotification.getClass().getMethod("getReportPart");
        final List<?> reportParts = ((List) getReportPart.invoke(actualNotification));
        assertEquals(1, reportParts.size());
        assertNull(reportParts.get(0).getClass().getMethod("getSourceMds").invoke(reportParts.get(0)));
        assertEquals(expectedStates, reportParts.get(0).getClass().getMethod(getStatesMethodName).invoke(reportParts.get(0)));
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
                Arrays.asList(typeValidator.resolveStateType(theClass).getConstructor().newInstance()),
                MdibVersion.create());
    }
}