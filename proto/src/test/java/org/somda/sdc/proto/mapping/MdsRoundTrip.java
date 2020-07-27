package org.somda.sdc.proto.mapping;

import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.model.participant.ApprovedJurisdictions;
import org.somda.sdc.biceps.model.participant.ComponentActivation;
import org.somda.sdc.biceps.model.participant.MdsDescriptor;
import org.somda.sdc.biceps.model.participant.MdsState;
import org.somda.sdc.biceps.model.participant.OperatingJurisdiction;
import org.somda.sdc.biceps.model.participant.SafetyClassification;
import org.somda.sdc.biceps.model.participant.factory.InstanceIdentifierFactory;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.testutil.Handles;

import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MdsRoundTrip implements BiConsumer<LocalMdibAccess, RemoteMdibAccess> {
    private final ApprovedJurisdictions approvedJurisdictions;
    private final OperatingJurisdiction operatingJurisdiction;

    public MdsRoundTrip(MdibDescriptionModifications modifications) {
        var descriptor = new MdsDescriptor();
        {
            descriptor.setHandle(Handles.MDS_0);
            approvedJurisdictions = new ApprovedJurisdictions();
            approvedJurisdictions.getApprovedJurisdiction()
                    .add(InstanceIdentifierFactory.createInstanceIdentifier("http://test/", "extension"));
            descriptor.setApprovedJurisdictions(approvedJurisdictions);
            descriptor.setSafetyClassification(SafetyClassification.MED_B);
        }

        var state = new MdsState();
        {
            state.setActivationState(ComponentActivation.NOT_RDY);
            state.setLang("de");
            state.setOperatingCycles(1000);
            state.setOperatingHours(5L);
            operatingJurisdiction = new OperatingJurisdiction();
            operatingJurisdiction.setRootName("http://full-qualifying-root");
            state.setOperatingJurisdiction(operatingJurisdiction);
        }

        modifications.insert(descriptor, state);
    }

    @Override
    public void accept(LocalMdibAccess localMdibAccess, RemoteMdibAccess remoteMdibAccess) {
        var expectedDescriptor = localMdibAccess.getDescriptor(Handles.MDS_0, MdsDescriptor.class).get();
        var expectedState = localMdibAccess.getState(Handles.MDS_0, MdsState.class).get();
        var actualDescriptor = remoteMdibAccess.getDescriptor(Handles.MDS_0, MdsDescriptor.class).get();
        var actualState = remoteMdibAccess.getState(Handles.MDS_0, MdsState.class).get();

        assertEquals(expectedDescriptor.getHandle(), actualDescriptor.getHandle());
        assertEquals(SafetyClassification.MED_B, expectedDescriptor.getSafetyClassification());

        assertEquals(expectedState.getDescriptorHandle(), actualState.getDescriptorHandle());
        assertEquals(ComponentActivation.NOT_RDY, actualState.getActivationState());
        assertEquals(operatingJurisdiction.getRootName(), actualState.getOperatingJurisdiction().getRootName());
    }
}
