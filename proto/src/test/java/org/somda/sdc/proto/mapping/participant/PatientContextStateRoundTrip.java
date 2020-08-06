package org.somda.sdc.proto.mapping.participant;

import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.model.participant.ContextAssociation;
import org.somda.sdc.biceps.model.participant.PatientContextDescriptor;
import org.somda.sdc.biceps.model.participant.PatientContextState;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.testutil.Handles;

import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class PatientContextStateRoundTrip implements BiConsumer<LocalMdibAccess, RemoteMdibAccess> {
    private static final String HANDLE = Handles.CONTEXTDESCRIPTOR_2;
    private static final String HANDLE_MIN = HANDLE + "Min";
    private static final String HANDLE_STATE = Handles.CONTEXT_2;;
    private static final String HANDLE_STATE_MIN = HANDLE_STATE + "Min";


    PatientContextStateRoundTrip(MdibDescriptionModifications modifications) {
        bigSet(modifications);
        minimalSet(modifications);
    }

    void bigSet(MdibDescriptionModifications modifications) {

    }

    void minimalSet(MdibDescriptionModifications modifications) {
        var descriptor = new PatientContextDescriptor();
        {
            descriptor.setHandle(HANDLE_MIN);
        }
        var state = new PatientContextState();
        {
            state.setDescriptorHandle(HANDLE_MIN);
            state.setHandle(HANDLE_STATE_MIN);
            // this needs to be set, otherwise MdibEntityImpl drops the state
            state.setContextAssociation(ContextAssociation.ASSOC);
        }

        modifications.insert(descriptor, state, Handles.SYSTEMCONTEXT_0);
    }

    @Override
    public void accept(final LocalMdibAccess localMdibAccess, final RemoteMdibAccess remoteMdibAccess) {
//        {
//            var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE, PatientContextDescriptor.class);
//            var expectedState = localMdibAccess.getState(HANDLE_STATE, PatientContextState.class);
//            var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE, PatientContextDescriptor.class);
//            var actualState = remoteMdibAccess.getState(HANDLE_STATE, PatientContextState.class);
//
//            assertEquals(expectedDescriptor, actualDescriptor);
//            assertEquals(expectedState, actualState);
//        }
        {
            var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE_MIN, PatientContextDescriptor.class);
            var expectedState = localMdibAccess.getState(HANDLE_STATE_MIN, PatientContextState.class);
            var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE_MIN, PatientContextDescriptor.class);
            var actualState = remoteMdibAccess.getState(HANDLE_STATE_MIN, PatientContextState.class);

            // if everything is empty, everything is equal...
            assertFalse(actualDescriptor.isEmpty());
            assertFalse(actualState.isEmpty());

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
    }
}
