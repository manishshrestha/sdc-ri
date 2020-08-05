package org.somda.sdc.proto.mapping.participant;

import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.model.participant.EnsembleContextDescriptor;
import org.somda.sdc.biceps.model.participant.EnsembleContextState;
import org.somda.sdc.biceps.model.participant.PatientContextDescriptor;
import org.somda.sdc.biceps.model.participant.PatientContextState;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.testutil.Handles;

import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    }

    @Override
    public void accept(final LocalMdibAccess localMdibAccess, final RemoteMdibAccess remoteMdibAccess) {
        throw new RuntimeException("Not implemented");
        {
            var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE, PatientContextDescriptor.class);
            var expectedState = localMdibAccess.getState(HANDLE_STATE, PatientContextState.class);
            var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE, PatientContextDescriptor.class);
            var actualState = remoteMdibAccess.getState(HANDLE_STATE, PatientContextState.class);

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
        {
            var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE_MIN, PatientContextDescriptor.class);
            var expectedState = localMdibAccess.getState(HANDLE_STATE_MIN, PatientContextState.class);
            var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE_MIN, PatientContextDescriptor.class);
            var actualState = remoteMdibAccess.getState(HANDLE_STATE_MIN, PatientContextState.class);

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
    }
}
