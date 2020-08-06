package org.somda.sdc.proto.mapping.participant;

import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.model.participant.ApprovedJurisdictions;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.biceps.model.participant.ChannelState;
import org.somda.sdc.biceps.model.participant.ComponentActivation;
import org.somda.sdc.biceps.model.participant.OperatingJurisdiction;
import org.somda.sdc.biceps.model.participant.SafetyClassification;
import org.somda.sdc.biceps.model.participant.factory.InstanceIdentifierFactory;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.testutil.Handles;

import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class SystemContextRoundTrip implements BiConsumer<LocalMdibAccess, RemoteMdibAccess> {
    private static final String HANDLE = Handles.SYSTEMCONTEXT_0;
    private static final String HANDLE_MIN = Handles.SYSTEMCONTEXT_1;
    public SystemContextRoundTrip(MdibDescriptionModifications modifications) {
        bigSet(modifications);
        minimalSet(modifications);
    }

    void bigSet(MdibDescriptionModifications modifications) {
        var descriptor = new SystemContextDescriptor();
        {
            descriptor.setHandle(HANDLE);
        }

        var state = new SystemContextState();
        {
        }

        modifications.insert(descriptor, state, Handles.MDS_0);
    }

    void minimalSet(MdibDescriptionModifications modifications) {
        var descriptor = new SystemContextDescriptor();
        {
            descriptor.setHandle(HANDLE_MIN);
        }

        var state = new SystemContextState();
        {
        }

        modifications.insert(descriptor, state, Handles.MDS_1);
    }

    @Override
    public void accept(LocalMdibAccess localMdibAccess, RemoteMdibAccess remoteMdibAccess) {
        {
            var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE, SystemContextDescriptor.class);
            var expectedState = localMdibAccess.getState(HANDLE, SystemContextState.class);
            var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE, SystemContextDescriptor.class);
            var actualState = remoteMdibAccess.getState(HANDLE, SystemContextState.class);


            // if everything is empty, everything is equal...
            assertFalse(actualDescriptor.isEmpty());
            assertFalse(actualState.isEmpty());

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
        {
            var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE_MIN, SystemContextDescriptor.class);
            var expectedState = localMdibAccess.getState(HANDLE_MIN, SystemContextState.class);
            var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE_MIN, SystemContextDescriptor.class);
            var actualState = remoteMdibAccess.getState(HANDLE_MIN, SystemContextState.class);

            // if everything is empty, everything is equal...
            assertFalse(actualDescriptor.isEmpty());
            assertFalse(actualState.isEmpty());

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
    }
}