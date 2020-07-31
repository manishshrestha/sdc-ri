package org.somda.sdc.proto.mapping.participant;

import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.model.participant.AlertActivation;
import org.somda.sdc.biceps.model.participant.AlertConditionDescriptor;
import org.somda.sdc.biceps.model.participant.AlertConditionKind;
import org.somda.sdc.biceps.model.participant.AlertConditionPriority;
import org.somda.sdc.biceps.model.participant.AlertConditionState;
import org.somda.sdc.biceps.model.participant.SafetyClassification;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.testutil.Handles;
import org.somda.sdc.proto.UnitTestUtil;

import java.math.BigInteger;
import java.time.Duration;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AlertConditionRoundTrip implements BiConsumer<LocalMdibAccess, RemoteMdibAccess> {

    private static final String HANDLE = Handles.ALERTCONDITION_0;
    private static final String HANDLE_MIN = HANDLE + "Min";

    AlertConditionRoundTrip(MdibDescriptionModifications modifications) {
        bigSet(modifications);
        minimalSet(modifications);
    }


    void bigSet(MdibDescriptionModifications modifications) {
        var descriptor = new AlertConditionDescriptor();
        {
            descriptor.setHandle(HANDLE);
            descriptor.setDescriptorVersion(BigInteger.valueOf(123123));
            descriptor.setSafetyClassification(SafetyClassification.MED_C);
            descriptor.setKind(AlertConditionKind.PHY);
            descriptor.setPriority(AlertConditionPriority.HI);
            descriptor.setDefaultConditionGenerationDelay(Duration.ofMillis(33));
            descriptor.setCanDeescalate(AlertConditionPriority.LO);
            descriptor.setCanEscalate(AlertConditionPriority.LO);

            // TODO
//            descriptor.setSource();
//            descriptor.setCauseInfo();
        }
        var state = new AlertConditionState();
        {
            state.setStateVersion(BigInteger.TEN);
            state.setDescriptorHandle(HANDLE);
            state.setDescriptorVersion(descriptor.getDescriptorVersion());
            state.setActivationState(AlertActivation.ON);
            state.setActualConditionGenerationDelay(Duration.ofDays(4));
            state.setActualPriority(AlertConditionPriority.LO);
            state.setRank(1337);
            state.setPresence(false);
            state.setDeterminationTime(UnitTestUtil.makeTestTimestamp());
        }
        modifications.insert(descriptor, state, Handles.ALERTSYSTEM_1);
    }

    void minimalSet(MdibDescriptionModifications modifications) {
        var descriptor = new AlertConditionDescriptor();
        {
            descriptor.setHandle(HANDLE_MIN);
            descriptor.setKind(AlertConditionKind.PHY);
            descriptor.setPriority(AlertConditionPriority.HI);
        }
        var state = new AlertConditionState();
        {
            state.setDescriptorHandle(HANDLE_MIN);
            state.setActivationState(AlertActivation.ON);
        }
        modifications.insert(descriptor, state, Handles.ALERTSYSTEM_0);
    }

    @Override
    public void accept(final LocalMdibAccess localMdibAccess, final RemoteMdibAccess remoteMdibAccess) {
        {
            var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE, AlertConditionDescriptor.class).get();
            var expectedState = localMdibAccess.getState(HANDLE, AlertConditionState.class).get();
            var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE, AlertConditionDescriptor.class).get();
            var actualState = remoteMdibAccess.getState(HANDLE, AlertConditionState.class).get();

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
        {
            var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE_MIN, AlertConditionDescriptor.class).get();
            var expectedState = localMdibAccess.getState(HANDLE_MIN, AlertConditionState.class).get();
            var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE_MIN, AlertConditionDescriptor.class).get();
            var actualState = remoteMdibAccess.getState(HANDLE_MIN, AlertConditionState.class).get();

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
    }
}
