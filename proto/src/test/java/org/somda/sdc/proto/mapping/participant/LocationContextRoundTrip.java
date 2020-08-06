package org.somda.sdc.proto.mapping.participant;

import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.model.participant.ContextAssociation;
import org.somda.sdc.biceps.model.participant.EnsembleContextDescriptor;
import org.somda.sdc.biceps.model.participant.EnsembleContextState;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.biceps.model.participant.factory.CodedValueFactory;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.testutil.Handles;
import org.somda.sdc.proto.UnitTestUtil;

import java.math.BigInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class LocationContextRoundTrip implements BiConsumer<LocalMdibAccess, RemoteMdibAccess> {
    public LocationContextRoundTrip(MdibDescriptionModifications modifications) {
        var descriptor = new LocationContextDescriptor();
        {
            descriptor.setHandle(Handles.CONTEXTDESCRIPTOR_1);
        }

        var state = new LocationContextState();
        {
            state.setHandle(Handles.CONTEXT_1);
            state.setUnbindingMdibVersion(BigInteger.ONE);
            state.setBindingStartTime(UnitTestUtil.makeTestTimestamp());
            // this needs to be set, otherwise MdibEntityImpl drops the state
            state.setContextAssociation(ContextAssociation.ASSOC);

            var detail = new LocationDetail();
            detail.setRoom("Room");
            detail.setFacility("Facility");
            state.setLocationDetail(detail);
        }

        modifications.insert(descriptor, state, Handles.SYSTEMCONTEXT_0);
    }

    @Override
    public void accept(LocalMdibAccess localMdibAccess, RemoteMdibAccess remoteMdibAccess) {
        var expectedDescriptor = localMdibAccess.getDescriptor(Handles.CONTEXTDESCRIPTOR_1, LocationContextDescriptor.class);
        var expectedState = localMdibAccess.getState(Handles.CONTEXT_1, LocationContextState.class);
        var actualDescriptor = remoteMdibAccess.getDescriptor(Handles.CONTEXTDESCRIPTOR_1, LocationContextDescriptor.class);
        var actualState = remoteMdibAccess.getState(Handles.CONTEXT_1, LocationContextState.class);

        // if everything is empty, everything is equal...
        assertFalse(actualDescriptor.isEmpty());
        assertFalse(actualState.isEmpty());

        assertEquals(expectedDescriptor, actualDescriptor);
        assertEquals(expectedState, actualState);
    }
}