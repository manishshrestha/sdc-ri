package org.somda.sdc.proto.mapping.participant;

import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.model.participant.ApprovedJurisdictions;
import org.somda.sdc.biceps.model.participant.ChannelDescriptor;
import org.somda.sdc.biceps.model.participant.ChannelState;
import org.somda.sdc.biceps.model.participant.ComponentActivation;
import org.somda.sdc.biceps.model.participant.OperatingJurisdiction;
import org.somda.sdc.biceps.model.participant.SafetyClassification;
import org.somda.sdc.biceps.model.participant.factory.InstanceIdentifierFactory;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.testutil.Handles;

import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChannelRoundTrip implements BiConsumer<LocalMdibAccess, RemoteMdibAccess> {
    private static final String HANDLE = Handles.CHANNEL_0;
    private static final String HANDLE_MIN = HANDLE + "Min";

    public ChannelRoundTrip(MdibDescriptionModifications modifications) {
        bigSet(modifications);
        minimalSet(modifications);
    }

    private void minimalSet(MdibDescriptionModifications modifications) {
        var descriptor = new ChannelDescriptor();
        {
            descriptor.setHandle(HANDLE_MIN);
        }

        var state = new ChannelState();
        {
        }

        modifications.insert(descriptor, state, Handles.VMD_0);
    }

    private void bigSet(MdibDescriptionModifications modifications) {
        var descriptor = new ChannelDescriptor();
        {
            descriptor.setHandle(HANDLE);
            descriptor.setSafetyClassification(SafetyClassification.MED_C);
        }

        var state = new ChannelState();
        {
            state.setActivationState(ComponentActivation.SHTDN);
            state.setOperatingCycles(1000);
            state.setOperatingHours(5L);
        }

        modifications.insert(descriptor, state, Handles.VMD_0);
    }

    @Override
    public void accept(LocalMdibAccess localMdibAccess, RemoteMdibAccess remoteMdibAccess) {
        {
            var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE, ChannelDescriptor.class);
            var expectedState = localMdibAccess.getState(HANDLE, ChannelState.class);
            var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE, ChannelDescriptor.class);
            var actualState = remoteMdibAccess.getState(HANDLE, ChannelState.class);

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
        {
            var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE_MIN, ChannelDescriptor.class);
            var expectedState = localMdibAccess.getState(HANDLE_MIN, ChannelState.class);
            var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE_MIN, ChannelDescriptor.class);
            var actualState = remoteMdibAccess.getState(HANDLE_MIN, ChannelState.class);

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
    }
}