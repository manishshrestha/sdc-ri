package org.somda.sdc.proto.mapping;

import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.model.participant.ApprovedJurisdictions;
import org.somda.sdc.biceps.model.participant.ChannelDescriptor;
import org.somda.sdc.biceps.model.participant.ChannelState;
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
    private ApprovedJurisdictions approvedJurisdictions;
    private OperatingJurisdiction operatingJurisdiction;

    private static final String HANDLE = Handles.VMD_0;
    private static final String HANDLE_MIN = HANDLE + "Min";

    public VmdRoundTrip(MdibDescriptionModifications modifications) {
        bigSet(modifications);
        minimalSet(modifications);
    }

    private void minimalSet(MdibDescriptionModifications modifications) {
        var descriptor = new VmdDescriptor();
        {
            descriptor.setHandle(HANDLE_MIN);
        }

        var state = new VmdState();
        {
        }

        modifications.insert(descriptor, state, Handles.MDS_0);
    }

    private void bigSet(MdibDescriptionModifications modifications) {
        var descriptor = new VmdDescriptor();
        {
            descriptor.setHandle(HANDLE);
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
        {
            var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE, VmdDescriptor.class);
            var expectedState = localMdibAccess.getState(HANDLE, VmdState.class);
            var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE, VmdDescriptor.class);
            var actualState = remoteMdibAccess.getState(HANDLE, VmdState.class);

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
        {
            var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE_MIN, VmdDescriptor.class);
            var expectedState = localMdibAccess.getState(HANDLE_MIN, VmdState.class);
            var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE_MIN, VmdDescriptor.class);
            var actualState = remoteMdibAccess.getState(HANDLE_MIN, VmdState.class);

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
    }
}