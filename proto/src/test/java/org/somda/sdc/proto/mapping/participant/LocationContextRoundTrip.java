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
import org.somda.sdc.proto.mapping.TypeCollection;

import java.math.BigInteger;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class LocationContextRoundTrip implements BiConsumer<LocalMdibAccess, RemoteMdibAccess> {
    private static final String HANDLE = Handles.CONTEXTDESCRIPTOR_1;
    private static final String HANDLE_MIN = HANDLE + "Min";
    private static final String HANDLE_STATE = Handles.CONTEXT_1;;
    private static final String HANDLE_STATE_MIN = HANDLE_STATE + "Min";


    public LocationContextRoundTrip(MdibDescriptionModifications modifications) {
        bigSet(modifications);
        minimalSet(modifications);
    }

    private void bigSet(MdibDescriptionModifications modifications) {
        var descriptor = new LocationContextDescriptor();
        {
            descriptor.setHandle(HANDLE);
            descriptor.setDescriptorVersion(BigInteger.valueOf(555));
            descriptor.setSafetyClassification(SafetyClassification.MED_B);

            descriptor.setType(TypeCollection.CODED_VALUE);

            // TODO: Extension
//            descriptor.setExtension();
        }

        var state = new LocationContextState();
        {
            state.setStateVersion(BigInteger.valueOf(5323));
            state.setDescriptorHandle(descriptor.getHandle());
            state.setDescriptorVersion(descriptor.getDescriptorVersion());
            state.setHandle(HANDLE_STATE);
            state.setContextAssociation(ContextAssociation.NO);
            state.setBindingMdibVersion(BigInteger.valueOf(5));
            state.setUnbindingMdibVersion(BigInteger.valueOf(6));
            state.setBindingStartTime(UnitTestUtil.makeTestTimestamp());
            state.setBindingEndTime(UnitTestUtil.makeTestTimestamp());

            state.setCategory(TypeCollection.CODED_VALUE);
            state.setValidator(List.of(TypeCollection.INSTANCE_IDENTIFIER));
            state.setIdentification(List.of(TypeCollection.INSTANCE_IDENTIFIER, TypeCollection.INSTANCE_IDENTIFIER));

            var detail = new LocationDetail();
            detail.setPoC("PoC");
            detail.setRoom("Room");
            detail.setBed("Bed");
            detail.setFacility("Facility");
            detail.setBuilding("Building");
            detail.setFloor("Floor");

            state.setLocationDetail(detail);

            // TODO: Extension
//            state.setExtension();
        }

        modifications.insert(descriptor, state, Handles.SYSTEMCONTEXT_0);
    }

    private void minimalSet(MdibDescriptionModifications modifications) {
        var descriptor = new LocationContextDescriptor();
        {
            descriptor.setHandle(HANDLE_MIN);
        }

        var state = new LocationContextState();
        {
            state.setDescriptorHandle(descriptor.getHandle());
            state.setHandle(HANDLE_STATE_MIN);
        }

        modifications.insert(descriptor, state, Handles.SYSTEMCONTEXT_1);
    }

    @Override
    public void accept(LocalMdibAccess localMdibAccess, RemoteMdibAccess remoteMdibAccess) {
        {
            var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE, LocationContextDescriptor.class);
            var expectedState = localMdibAccess.getState(HANDLE_STATE, LocationContextState.class);
            var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE, LocationContextDescriptor.class);
            var actualState = remoteMdibAccess.getState(HANDLE_STATE, LocationContextState.class);

            // if everything is empty, everything is equal...
            assertFalse(actualDescriptor.isEmpty());
            assertFalse(actualState.isEmpty());

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
        {
            var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE_MIN, LocationContextDescriptor.class);
            var expectedState = localMdibAccess.getState(HANDLE_STATE_MIN, LocationContextState.class);
            var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE_MIN, LocationContextDescriptor.class);
            var actualState = remoteMdibAccess.getState(HANDLE_STATE_MIN, LocationContextState.class);

            // if everything is empty, everything is equal...
            assertFalse(actualDescriptor.isEmpty());
            assertFalse(actualState.isEmpty());

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
    }
}