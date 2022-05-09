package it.org.somda.glue.provider;

import com.google.common.util.concurrent.Service;
import org.somda.sdc.biceps.common.MdibStateModifications;
import org.somda.sdc.biceps.common.access.ReadTransaction;
import org.somda.sdc.biceps.common.storage.PreprocessingException;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.glue.common.FallbackInstanceIdentifier;
import org.somda.sdc.glue.common.MdibXmlIo;
import org.somda.sdc.glue.common.factory.ModificationsBuilderFactory;
import org.somda.sdc.glue.provider.SdcDeviceContext;
import org.somda.sdc.glue.provider.SdcDevicePlugin;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;

public class VentilatorMdibRunner implements SdcDevicePlugin {
    public static final String HANDLE_MDC_DEV_SYS_PT_VENT_MDS = "handle_MDC_DEV_SYS_PT_VENT_MDS";
    public static final String HANDLE_CLOCK = "handle_Clock";
    public static final String HANDLE_SYSTEMCONTEXT = "handle_SystemContext";
    public static final String HANDLE_LOCATIONCONTEXT = "handle_LocationContext";
    public static final String HANDLE_MDC_DEV_SYS_PT_VENT_VMD = "handle_MDC_DEV_SYS_PT_VENT_VMD";
    public static final String HANDLE_MDC_DEV_SYS_PT_VENT_CHAN = "handle_MDC_DEV_SYS_PT_VENT_CHAN";
    public static final String HANDLE_MDC_VENT_MODE = "handle_MDC_VENT_MODE";
    public static final String HANDLE_MDC_PRESS_AWAY_END_EXP_POS = "handle_MDC_PRESS_AWAY_END_EXP_POS";
    public static final String HANDLE_BAD_MDC_DEV_SYS_PT_VENT_VMD = "handle_bad_MDC_DEV_SYS_PT_VENT_VMD";
    public static final String HANDLE_VIS_BAD_MDC_DEV_SYS_PT_VENT_VMD = "handle_vis_bad_MDC_DEV_SYS_PT_VENT_VMD";
    public static final String HANDLE_SET_MDC_DEV_SYS_PT_VENT_VMD = "handle_set_MDC_DEV_SYS_PT_VENT_VMD";
    public static final String HANDLE_ALERT_SYSTEM = "handle_AlertSystem";
    public static final String HANDLE_SCO = "handle_Sco";

    private final MdibXmlIo mdibXmlIo;
    private final ModificationsBuilderFactory modificationsBuilderFactory;
    private LocalMdibAccess mdibAccess;
    private SdcDeviceContext sdcDeviceContext;

    private int locationContextHandleSuffixCounter;

    public VentilatorMdibRunner(MdibXmlIo mdibXmlIo,
                                ModificationsBuilderFactory modificationsBuilderFactory) {
        this.mdibXmlIo = mdibXmlIo;
        this.modificationsBuilderFactory = modificationsBuilderFactory;
        this.locationContextHandleSuffixCounter = 0;
    }

    @Override
    public void beforeStartUp(SdcDeviceContext context) throws Exception {
        sdcDeviceContext = context;
        mdibAccess = context.getLocalMdibAccess();

        var classLoader = getClass().getClassLoader();
        var mdibAsStream = classLoader.getResourceAsStream("it/org/somda/sdc/glue/VentilatorMdib.xml");
        assert mdibAsStream != null;
        var mdib = mdibXmlIo.readMdib(mdibAsStream);
        var modifications = modificationsBuilderFactory.createModificationsBuilder(mdib).get();

        mdibAccess.writeDescription(modifications);
    }

    public void changeLocation(LocationDetail newLocation) throws PreprocessingException {
        final InstanceIdentifier identifier = FallbackInstanceIdentifier.create(newLocation).orElseThrow(() ->
                new IllegalStateException(String.format("Could not create fallback instance identifier from location %s",
                        newLocation.toString())));

        final MdibStateModifications modifications = MdibStateModifications.create(MdibStateModifications.Type.CONTEXT);
        for (AbstractContextState contextState : mdibAccess.getContextStates(HANDLE_LOCATIONCONTEXT)) {
            if (contextState.getContextAssociation().equals(ContextAssociation.ASSOC)) {
                modifications.add(contextState.newCopyBuilder().withContextAssociation(ContextAssociation.DIS).build());
            }
        }
        mdibAccess.writeStates(modifications);

        modifications.clear();
        for (AbstractContextState contextState : mdibAccess.getContextStates(HANDLE_LOCATIONCONTEXT)) {
            var contextStateBuilder = contextState.newCopyBuilder();
            if (contextState.getContextAssociation().equals(ContextAssociation.DIS)) {
                contextStateBuilder.withContextAssociation(ContextAssociation.NO);
            }
            modifications.add(contextStateBuilder.build());
        }

        var locationContextState = LocationContextState.builder()
            .withContextAssociation(ContextAssociation.ASSOC)
            .withLocationDetail(newLocation)
            .addIdentification(identifier)
            .withDescriptorHandle(HANDLE_LOCATIONCONTEXT)
            .withHandle(HANDLE_LOCATIONCONTEXT + "_state" + locationContextHandleSuffixCounter++);
        var validator = InstanceIdentifier.builder()
            .withRootName("urn:validator:demo");
        locationContextState.addValidator(validator.build());
        modifications.add(locationContextState.build());
        mdibAccess.writeStates(modifications);
    }

    public void changeAlertsPresence(@Nullable Boolean ventilatorModeAlarm) throws PreprocessingException {
        final MdibStateModifications modifications = MdibStateModifications.create(MdibStateModifications.Type.ALERT);
        try (ReadTransaction readTransaction = mdibAccess.startTransaction()) {
            if (ventilatorModeAlarm != null) {
                changeVentilatorModeAlarm(readTransaction, modifications, ventilatorModeAlarm);
            }
        }
        mdibAccess.writeStates(modifications);
    }

    private void changeVentilatorModeAlarm(ReadTransaction readTransaction, MdibStateModifications modifications, Boolean ventilatorModeAlarm) {
        final var conditionState = readTransaction.getState(HANDLE_BAD_MDC_DEV_SYS_PT_VENT_VMD, AlertConditionState.class)
                .orElseThrow(() ->
                        new RuntimeException(String.format("Could not find state for handle %s", HANDLE_BAD_MDC_DEV_SYS_PT_VENT_VMD)))
            .newCopyBuilder();
        final var signalState = readTransaction.getState(HANDLE_VIS_BAD_MDC_DEV_SYS_PT_VENT_VMD, AlertSignalState.class)
                .orElseThrow(() ->
                        new RuntimeException(String.format("Could not find state for handle %s", HANDLE_VIS_BAD_MDC_DEV_SYS_PT_VENT_VMD)))
            .newCopyBuilder();

        conditionState.withPresence(ventilatorModeAlarm)
            .withDeterminationTime(Instant.now());
        if (ventilatorModeAlarm) {
            signalState.withPresence(AlertSignalPresence.ON);
        } else {
            signalState.withPresence(AlertSignalPresence.OFF);
        }

        modifications.addAll(Arrays.asList(conditionState.build(), signalState.build()));
    }

    public void changeMetrics(@Nullable VentilatorMode ventilatorMode,
                              @Nullable BigDecimal peep) throws PreprocessingException {
        if (!(isRunning())) {
            return;
        }

        final MdibStateModifications modifications = MdibStateModifications.create(MdibStateModifications.Type.METRIC);

        try (ReadTransaction readTransaction = mdibAccess.startTransaction()) {
            if (ventilatorMode != null) {
                changeVentilatorModeValue(readTransaction, modifications, ventilatorMode);
            }
            if (peep != null) {
                changePeepValue(readTransaction, modifications, peep);
            }
        }

        mdibAccess.writeStates(modifications);
    }

    private boolean isRunning() {
        return sdcDeviceContext != null && sdcDeviceContext.getServiceState().equals(Service.State.RUNNING);
    }

    private void changeVentilatorModeValue(ReadTransaction readTransaction, MdibStateModifications modifications, VentilatorMode ventilatorMode) {
        final EnumStringMetricState state = readTransaction.getState(HANDLE_MDC_VENT_MODE, EnumStringMetricState.class)
                .orElseThrow(() ->
                        new RuntimeException(String.format("Could not find state for handle %s", HANDLE_MDC_VENT_MODE)));

        final var stateBuilder = state.newCopyBuilder();

        final var metricQuality = AbstractMetricValue.MetricQuality.builder()
            .withMode(GenerationMode.DEMO)
            .withValidity(MeasurementValidity.INV);
        StringMetricValue oldMetricValue = state.getMetricValue();
        StringMetricValue.Builder<?> metricValueBuilder;
        if (oldMetricValue == null) {
            metricValueBuilder = StringMetricValue.builder();
        } else {
            metricValueBuilder = oldMetricValue.newCopyBuilder();
        }
        metricValueBuilder.withValue(ventilatorMode.getModeValue())
            .withDeterminationTime(Instant.now())
            .withMetricQuality(metricQuality.build());
        stateBuilder.withMetricValue(metricValueBuilder.build());
        modifications.add(stateBuilder.build());
    }

    public static void changePeepValue(ReadTransaction readTransaction, MdibStateModifications modifications, BigDecimal peep) {
        final NumericMetricState state = readTransaction.getState(HANDLE_MDC_PRESS_AWAY_END_EXP_POS, NumericMetricState.class)
                .orElseThrow(() ->
                        new RuntimeException(String.format("Could not find state for handle %s", HANDLE_MDC_VENT_MODE)));

        final var stateBuilder = state.newCopyBuilder();

        final var metricQuality = AbstractMetricValue.MetricQuality.builder()
            .withMode(GenerationMode.DEMO)
            .withValidity(MeasurementValidity.INV);
        var oldMetricValue = state.getMetricValue();
        NumericMetricValue.Builder<?> metricValueBuilder;
        if (oldMetricValue == null) {
            metricValueBuilder = NumericMetricValue.builder();
        } else {
            metricValueBuilder = oldMetricValue.newCopyBuilder();
        }
        metricValueBuilder.withValue(peep)
            .withDeterminationTime(Instant.now())
            .withMetricQuality(metricQuality.build());
        stateBuilder.withMetricValue(metricValueBuilder.build());
        modifications.add(stateBuilder.build());
    }

    public enum VentilatorMode {
        VENT_MODE_CPAP("vent-mode-cpap"),
        VENT_MODE_SIMV("vent-mode-simv"),
        VENT_MODE_INSPASSIST("vent-mode-inspassist");

        private String modeValue;

        VentilatorMode(String modeValue) {
            this.modeValue = modeValue;
        }

        public String getModeValue() {
            return modeValue;
        }
    }
}
