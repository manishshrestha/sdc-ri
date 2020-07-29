package org.somda.sdc.proto.mapping;

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

public class SystemContextRoundTrip implements BiConsumer<LocalMdibAccess, RemoteMdibAccess> {
    public SystemContextRoundTrip(MdibDescriptionModifications modifications) {
        var descriptor = new SystemContextDescriptor();
        {
            descriptor.setHandle(Handles.SYSTEMCONTEXT_0);
        }

        var state = new SystemContextState();
        {
        }

        modifications.insert(descriptor, state, Handles.MDS_0);
    }

    @Override
    public void accept(LocalMdibAccess localMdibAccess, RemoteMdibAccess remoteMdibAccess) {
        var expectedDescriptor = localMdibAccess.getDescriptor(Handles.SYSTEMCONTEXT_0, SystemContextDescriptor.class);
        var expectedState = localMdibAccess.getState(Handles.SYSTEMCONTEXT_0, SystemContextState.class);
        var actualDescriptor = remoteMdibAccess.getDescriptor(Handles.SYSTEMCONTEXT_0, SystemContextDescriptor.class);
        var actualState = remoteMdibAccess.getState(Handles.SYSTEMCONTEXT_0, SystemContextState.class);

        assertEquals(expectedDescriptor, actualDescriptor);
        assertEquals(expectedState, actualState);
    }
}