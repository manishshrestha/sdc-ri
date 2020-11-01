package org.somda.sdc.proto.mapping.participant;

import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.model.participant.SystemContextDescriptor;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.biceps.model.participant.factory.CodedValueFactory;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.testutil.Handles;
import org.somda.sdc.proto.UnitTestUtil;
import org.somda.sdc.proto.mapping.TypeCollection;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class EnsembleContextRoundTrip implements BiConsumer<LocalMdibAccess, RemoteMdibAccess> {
    private static final String HANDLE = Handles.CONTEXTDESCRIPTOR_0;
    private static final String HANDLE_MIN = HANDLE + "Min";
    private static final String HANDLE_STATE = Handles.CONTEXT_0;;
    private static final String HANDLE_STATE_MIN = HANDLE_STATE + "Min";

    public EnsembleContextRoundTrip(MdibDescriptionModifications modifications) {
        bigSet(modifications);
        minimalSet(modifications);
    }

    private void bigSet(MdibDescriptionModifications modifications) {
        var descriptor = new EnsembleContextDescriptor();
        {
            descriptor.setHandle(HANDLE);
            descriptor.setDescriptorVersion(BigInteger.TEN);
            descriptor.setSafetyClassification(SafetyClassification.MED_B);

            descriptor.setType(CodedValueFactory.createIeeeCodedValue("12345"));

            // TODO: Extension
//            descriptor.setExtension();
        }

        var state = new EnsembleContextState();
        {
            state.setStateVersion(BigInteger.valueOf(24));
            state.setDescriptorHandle(descriptor.getHandle());
            state.setDescriptorVersion(descriptor.getDescriptorVersion());
            state.setHandle(HANDLE_STATE);
            state.setContextAssociation(ContextAssociation.ASSOC);
            state.setBindingMdibVersion(BigInteger.TWO);
            state.setUnbindingMdibVersion(BigInteger.valueOf(4));
            state.setBindingStartTime(UnitTestUtil.makeTestTimestamp());
            state.setBindingEndTime(UnitTestUtil.makeTestTimestamp());

            state.setCategory(TypeCollection.CODED_VALUE);

            state.setValidator(List.of(TypeCollection.INSTANCE_IDENTIFIER));
            state.setIdentification(List.of(TypeCollection.INSTANCE_IDENTIFIER, TypeCollection.INSTANCE_IDENTIFIER));

            // TODO: Extension
//            state.setExtension();
        }

        modifications.insert(descriptor, state, Handles.SYSTEMCONTEXT_0);
    }

    private void minimalSet(MdibDescriptionModifications modifications) {
        var descriptor = new EnsembleContextDescriptor();
        {
            descriptor.setHandle(HANDLE_MIN);
        }

        var state = new EnsembleContextState();
        {
            state.setDescriptorHandle(descriptor.getHandle());
            state.setHandle(HANDLE_STATE_MIN);
            // this needs to be set, otherwise MdibEntityImpl drops the state
            state.setContextAssociation(ContextAssociation.ASSOC);
        }

        modifications.insert(descriptor, state, Handles.SYSTEMCONTEXT_1);
    }


    @Override
    public void accept(LocalMdibAccess localMdibAccess, RemoteMdibAccess remoteMdibAccess) {
        {
            var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE, EnsembleContextDescriptor.class);
            var expectedState = localMdibAccess.getState(HANDLE_STATE, EnsembleContextState.class);
            var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE, EnsembleContextDescriptor.class);
            var actualState = remoteMdibAccess.getState(HANDLE_STATE, EnsembleContextState.class);

            // if everything is empty, everything is equal...
            assertFalse(actualDescriptor.isEmpty());
            assertFalse(actualState.isEmpty());

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
        {
            var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE_MIN, EnsembleContextDescriptor.class);
            var expectedState = localMdibAccess.getState(HANDLE_STATE_MIN, EnsembleContextState.class);
            var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE_MIN, EnsembleContextDescriptor.class);
            var actualState = remoteMdibAccess.getState(HANDLE_STATE_MIN, EnsembleContextState.class);

            // if everything is empty, everything is equal...
            assertFalse(actualDescriptor.isEmpty());
            assertFalse(actualState.isEmpty());

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
    }
}