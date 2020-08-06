package org.somda.sdc.proto.mapping.participant;

import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.model.participant.ApprovedJurisdictions;
import org.somda.sdc.biceps.model.participant.ChannelDescriptor;
import org.somda.sdc.biceps.model.participant.ChannelState;
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
    private ApprovedJurisdictions approvedJurisdictions;
    private OperatingJurisdiction operatingJurisdiction;

    private static final String HANDLE = Handles.MDS_0;
    private static final String HANDLE_MIN = Handles.MDS_1;

    public MdsRoundTrip(MdibDescriptionModifications modifications) {
        bigSet(modifications);
        minimalSet(modifications);
    }

    private void minimalSet(MdibDescriptionModifications modifications) {
        var descriptor = new MdsDescriptor();
        {
            descriptor.setHandle(HANDLE);
        }

        var state = new MdsState();
        {
        }

        modifications.insert(descriptor, state);
    }

    private void bigSet(MdibDescriptionModifications modifications) {
        var descriptor = new MdsDescriptor();
        {
            descriptor.setHandle(HANDLE_MIN);
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
        {
            var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE, MdsDescriptor.class);
            var expectedState = localMdibAccess.getState(HANDLE, MdsState.class);
            var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE, MdsDescriptor.class);
            var actualState = remoteMdibAccess.getState(HANDLE, MdsState.class);

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
        {
            var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE_MIN, MdsDescriptor.class);
            var expectedState = localMdibAccess.getState(HANDLE_MIN, MdsState.class);
            var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE_MIN, MdsDescriptor.class);
            var actualState = remoteMdibAccess.getState(HANDLE_MIN, MdsState.class);

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
    }
}
