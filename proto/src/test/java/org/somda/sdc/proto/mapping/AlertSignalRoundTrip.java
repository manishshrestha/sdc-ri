package org.somda.sdc.proto.mapping;

import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.model.participant.AlertActivation;
import org.somda.sdc.biceps.model.participant.AlertConditionDescriptor;
import org.somda.sdc.biceps.model.participant.AlertConditionState;
import org.somda.sdc.biceps.model.participant.AlertSignalDescriptor;
import org.somda.sdc.biceps.model.participant.AlertSignalManifestation;
import org.somda.sdc.biceps.model.participant.AlertSignalPresence;
import org.somda.sdc.biceps.model.participant.AlertSignalPrimaryLocation;
import org.somda.sdc.biceps.model.participant.AlertSignalState;
import org.somda.sdc.biceps.model.participant.SafetyClassification;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.testutil.Handles;

import java.math.BigInteger;
import java.time.Duration;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AlertSignalRoundTrip implements BiConsumer<LocalMdibAccess, RemoteMdibAccess> {

    private static final String HANDLE = Handles.ALERTSIGNAL_0;
    private static final String HANDLE_MIN = HANDLE + "Min";

    AlertSignalRoundTrip(MdibDescriptionModifications modifications) {
        bigSet(modifications);
        minimalSet(modifications);
    }

    void bigSet(MdibDescriptionModifications modifications) {
        var descriptor = new AlertSignalDescriptor();
        {
            descriptor.setHandle(HANDLE);
            descriptor.setDescriptorVersion(BigInteger.valueOf(64646778));
            descriptor.setSafetyClassification(SafetyClassification.MED_A);
            descriptor.setConditionSignaled("BEST CONDITION FITE ME");
            descriptor.setManifestation(AlertSignalManifestation.OTH);
            descriptor.setLatching(false);
            descriptor.setDefaultSignalGenerationDelay(Duration.ofDays(737));
            descriptor.setMinSignalGenerationDelay(Duration.ofMillis(3));
            descriptor.setMaxSignalGenerationDelay(Duration.ofMillis(8));
            descriptor.setSignalDelegationSupported(true);
            descriptor.setAcknowledgementSupported(false);
            descriptor.setAcknowledgeTimeout(Duration.ofMillis(5));
        }
        var state = new AlertSignalState();
        {
            state.setStateVersion(BigInteger.valueOf(8));
            state.setDescriptorHandle(descriptor.getHandle());
            state.setDescriptorVersion(descriptor.getDescriptorVersion());
            state.setActivationState(AlertActivation.ON);
            state.setActualSignalGenerationDelay(Duration.ofSeconds(8));
            state.setPresence(AlertSignalPresence.ON);
            state.setLocation(AlertSignalPrimaryLocation.REM);
            state.setSlot(8L);
        }
        modifications.insert(descriptor, state, Handles.ALERTSYSTEM_0);
    }

    void minimalSet(MdibDescriptionModifications modifications) {
        var descriptor = new AlertSignalDescriptor();
        {
            descriptor.setHandle(HANDLE_MIN);
            descriptor.setManifestation(AlertSignalManifestation.OTH);
            descriptor.setLatching(false);
        }
        var state = new AlertSignalState();
        {
            state.setDescriptorHandle(descriptor.getHandle());
            state.setActivationState(AlertActivation.ON);
        }
        modifications.insert(descriptor, state, Handles.ALERTSYSTEM_0);
    }

        @Override
    public void accept(final LocalMdibAccess localMdibAccess, final RemoteMdibAccess remoteMdibAccess) {
            {
                var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE, AlertSignalDescriptor.class).get();
                var expectedState = localMdibAccess.getState(HANDLE, AlertSignalState.class).get();
                var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE, AlertSignalDescriptor.class).get();
                var actualState = remoteMdibAccess.getState(HANDLE, AlertSignalState.class).get();

                assertEquals(expectedDescriptor, actualDescriptor);
                assertEquals(expectedState, actualState);
            }
            {
                var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE_MIN, AlertSignalDescriptor.class).get();
                var expectedState = localMdibAccess.getState(HANDLE_MIN, AlertSignalState.class).get();
                var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE_MIN, AlertSignalDescriptor.class).get();
                var actualState = remoteMdibAccess.getState(HANDLE_MIN, AlertSignalState.class).get();

                assertEquals(expectedDescriptor, actualDescriptor);
                assertEquals(expectedState, actualState);
            }
    }
}
