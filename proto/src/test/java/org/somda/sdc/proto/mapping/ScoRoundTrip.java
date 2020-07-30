package org.somda.sdc.proto.mapping;

import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.model.participant.ScoDescriptor;
import org.somda.sdc.biceps.model.participant.ScoState;
import org.somda.sdc.biceps.model.participant.SystemContextDescriptor;
import org.somda.sdc.biceps.model.participant.SystemContextState;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.testutil.Handles;

import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ScoRoundTrip implements BiConsumer<LocalMdibAccess, RemoteMdibAccess> {
    public ScoRoundTrip(MdibDescriptionModifications modifications) {
        var minimalDescriptor = new ScoDescriptor();
        {
            minimalDescriptor.setHandle(Handles.SCO_0);
        }

        var state = new ScoState();
        {
        }

        modifications.insert(minimalDescriptor, state, Handles.MDS_0);
    }

    @Override
    public void accept(LocalMdibAccess localMdibAccess, RemoteMdibAccess remoteMdibAccess) {
        var expectedDescriptor = localMdibAccess.getDescriptor(Handles.SCO_0, ScoDescriptor.class);
        var expectedState = localMdibAccess.getState(Handles.SCO_0, ScoState.class);
        var actualDescriptor = remoteMdibAccess.getDescriptor(Handles.SCO_0, ScoDescriptor.class);
        var actualState = remoteMdibAccess.getState(Handles.SCO_0, ScoState.class);

        assertEquals(expectedDescriptor, actualDescriptor);
        assertEquals(expectedState, actualState);
    }
}