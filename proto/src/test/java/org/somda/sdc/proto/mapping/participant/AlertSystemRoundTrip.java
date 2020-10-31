package org.somda.sdc.proto.mapping.participant;

import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.model.participant.AlertActivation;
import org.somda.sdc.biceps.model.participant.AlertConditionDescriptor;
import org.somda.sdc.biceps.model.participant.AlertConditionKind;
import org.somda.sdc.biceps.model.participant.AlertConditionPriority;
import org.somda.sdc.biceps.model.participant.AlertSignalManifestation;
import org.somda.sdc.biceps.model.participant.AlertSystemDescriptor;
import org.somda.sdc.biceps.model.participant.AlertSystemState;
import org.somda.sdc.biceps.model.participant.SafetyClassification;
import org.somda.sdc.biceps.model.participant.SystemSignalActivation;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.testutil.Handles;
import org.somda.sdc.proto.UnitTestUtil;
import org.somda.sdc.proto.mapping.TypeCollection;

import java.math.BigInteger;
import java.time.Duration;
import java.util.List;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AlertSystemRoundTrip implements BiConsumer<LocalMdibAccess, RemoteMdibAccess> {

    private static final String HANDLE = Handles.ALERTSYSTEM_0;
    private static final String HANDLE_MIN = Handles.ALERTSYSTEM_1;

    AlertSystemRoundTrip(MdibDescriptionModifications modifications) {
        bigSet(modifications);
        minimalSet(modifications);
    }

    void bigSet(MdibDescriptionModifications modifications) {
        var descriptor = new AlertSystemDescriptor();
        {
            descriptor.setHandle(HANDLE);
            descriptor.setDescriptorVersion(BigInteger.valueOf(1231231231));
            descriptor.setSafetyClassification(SafetyClassification.INF);
            descriptor.setType(TypeCollection.CODED_VALUE);

            descriptor.setMaxTechnicalParallelAlarms(1337L);
            descriptor.setMaxPhysiologicalParallelAlarms(1338L);
            descriptor.setSelfCheckPeriod(Duration.ofMillis(1)); // check yourself before you latch yourself

            // these are handled in separate tests
            // TODO: Do we still need children here?
//            descriptor.setAlertSignal();
//            descriptor.setAlertCondition();

            // TODO: Extension
//            descriptor.setExtension();
        }
        var state = new AlertSystemState();
        {
            state.setStateVersion(BigInteger.valueOf(123123123123L));
            state.setDescriptorHandle(descriptor.getHandle());
            state.setDescriptorVersion(descriptor.getDescriptorVersion());
            state.setActivationState(AlertActivation.ON);

            state.setLastSelfCheck(UnitTestUtil.makeTestTimestamp());
            state.setSelfCheckCount(55554444333L);
            state.setPresentTechnicalAlarmConditions(List.of("・(￣∀￣)・:*:"));
            state.setPresentPhysiologicalAlarmConditions(List.of("\uD83D\uDC35 \uD83D\uDE48 \uD83D\uDE49 \uD83D\uDE4A"));

            var ssa = new SystemSignalActivation();
            ssa.setManifestation(AlertSignalManifestation.VIS);
            ssa.setState(AlertActivation.OFF);

            var ssa2 = new SystemSignalActivation();
            ssa2.setManifestation(AlertSignalManifestation.AUD);
            ssa2.setState(AlertActivation.PSD);

            state.setSystemSignalActivation(List.of(ssa, ssa2));

            // TODO: Extension
//            state.setExtension();
        }

        modifications.insert(descriptor, state, Handles.MDS_0);
    }

    void minimalSet(MdibDescriptionModifications modifications) {
        var descriptor = new AlertSystemDescriptor();
        {
            descriptor.setHandle(HANDLE_MIN);
        }
        var state = new AlertSystemState();
        {
            state.setDescriptorHandle(HANDLE_MIN);
            state.setActivationState(AlertActivation.ON);
        }

        modifications.insert(descriptor, state, Handles.VMD_0);
    }

    @Override
    public void accept(final LocalMdibAccess localMdibAccess, final RemoteMdibAccess remoteMdibAccess) {
        {
            var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE, AlertSystemDescriptor.class).get();
            var expectedState = localMdibAccess.getState(HANDLE, AlertSystemState.class).get();
            var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE, AlertSystemDescriptor.class).get();
            var actualState = remoteMdibAccess.getState(HANDLE, AlertSystemState.class).get();

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
        {
            var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE_MIN, AlertSystemDescriptor.class).get();
            var expectedState = localMdibAccess.getState(HANDLE_MIN, AlertSystemState.class).get();
            var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE_MIN, AlertSystemDescriptor.class).get();
            var actualState = remoteMdibAccess.getState(HANDLE_MIN, AlertSystemState.class).get();

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
    }
}
