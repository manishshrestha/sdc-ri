package org.somda.sdc.proto.mapping;

import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.model.participant.ApprovedJurisdictions;
import org.somda.sdc.biceps.model.participant.ComponentActivation;
import org.somda.sdc.biceps.model.participant.OperatingJurisdiction;
import org.somda.sdc.biceps.model.participant.SafetyClassification;
import org.somda.sdc.biceps.model.participant.VmdDescriptor;
import org.somda.sdc.biceps.model.participant.VmdState;
import org.somda.sdc.biceps.model.participant.factory.InstanceIdentifierFactory;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.testutil.Handles;

import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VmdRoundTrip implements BiConsumer<LocalMdibAccess, RemoteMdibAccess> {
    private final ApprovedJurisdictions approvedJurisdictions;
    private final OperatingJurisdiction operatingJurisdiction;

    public VmdRoundTrip(MdibDescriptionModifications modifications) {
        var descriptor = new VmdDescriptor();
        {
            descriptor.setHandle(Handles.VMD_0);
            approvedJurisdictions = new ApprovedJurisdictions();
            approvedJurisdictions.getApprovedJurisdiction()
                    .add(InstanceIdentifierFactory.createInstanceIdentifier("http://test/", "extension"));
            descriptor.setApprovedJurisdictions(approvedJurisdictions);
            descriptor.setSafetyClassification(SafetyClassification.MED_B);
        }

        var state = new VmdState();
        {
            state.setActivationState(ComponentActivation.OFF);
            state.setOperatingCycles(1000);
            state.setOperatingHours(5L);
            operatingJurisdiction = new OperatingJurisdiction();
            operatingJurisdiction.setRootName("http://full-qualifying-root");
            state.setOperatingJurisdiction(operatingJurisdiction);
        }

        modifications.insert(descriptor, state, Handles.MDS_0);
    }

    @Override
    public void accept(LocalMdibAccess localMdibAccess, RemoteMdibAccess remoteMdibAccess) {
        var expectedDescriptor = localMdibAccess.getDescriptor(Handles.VMD_0, VmdDescriptor.class).get();
        var expectedState = localMdibAccess.getState(Handles.VMD_0, VmdState.class).get();
        var actualDescriptor = remoteMdibAccess.getDescriptor(Handles.VMD_0, VmdDescriptor.class).get();
        var actualState = remoteMdibAccess.getState(Handles.VMD_0, VmdState.class).get();

        assertEquals(expectedDescriptor.getHandle(), actualDescriptor.getHandle());
        assertEquals(SafetyClassification.MED_B, expectedDescriptor.getSafetyClassification());

        assertEquals(Handles.MDS_0, remoteMdibAccess.getEntity(Handles.VMD_0).get().getParent().get());
        assertEquals(expectedState.getDescriptorHandle(), actualState.getDescriptorHandle());
        assertEquals(ComponentActivation.OFF, actualState.getActivationState());
        assertEquals(operatingJurisdiction.getRootName(), actualState.getOperatingJurisdiction().getRootName());
    }
}