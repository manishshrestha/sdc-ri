package org.somda.sdc.proto.mapping;

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
    private final ApprovedJurisdictions approvedJurisdictions;
    private final OperatingJurisdiction operatingJurisdiction;

    public ChannelRoundTrip(MdibDescriptionModifications modifications) {
        var descriptor = new ChannelDescriptor();
        {
            descriptor.setHandle(Handles.CHANNEL_0);
            approvedJurisdictions = new ApprovedJurisdictions();
            approvedJurisdictions.getApprovedJurisdiction()
                    .add(InstanceIdentifierFactory.createInstanceIdentifier("http://test/", "extension"));
            descriptor.setSafetyClassification(SafetyClassification.MED_C);
        }

        var state = new ChannelState();
        {
            state.setActivationState(ComponentActivation.SHTDN);
            state.setOperatingCycles(1000);
            state.setOperatingHours(5L);
            operatingJurisdiction = new OperatingJurisdiction();
            operatingJurisdiction.setRootName("http://full-qualifying-root");
        }

        modifications.insert(descriptor, state, Handles.VMD_0);
    }

    @Override
    public void accept(LocalMdibAccess localMdibAccess, RemoteMdibAccess remoteMdibAccess) {
        var expectedDescriptor = localMdibAccess.getDescriptor(Handles.CHANNEL_0, ChannelDescriptor.class).get();
        var expectedState = localMdibAccess.getState(Handles.CHANNEL_0, ChannelState.class).get();
        var actualDescriptor = remoteMdibAccess.getDescriptor(Handles.CHANNEL_0, ChannelDescriptor.class).get();
        var actualState = remoteMdibAccess.getState(Handles.CHANNEL_0, ChannelState.class).get();

        assertEquals(expectedDescriptor.getHandle(), actualDescriptor.getHandle());
        assertEquals(SafetyClassification.MED_C, expectedDescriptor.getSafetyClassification());

        assertEquals(Handles.VMD_0, remoteMdibAccess.getEntity(Handles.CHANNEL_0).get().getParent().get());
        assertEquals(expectedState.getDescriptorHandle(), actualState.getDescriptorHandle());
        assertEquals(ComponentActivation.SHTDN, actualState.getActivationState());
    }
}