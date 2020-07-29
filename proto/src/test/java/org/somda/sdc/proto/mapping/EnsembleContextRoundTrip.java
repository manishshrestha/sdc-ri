package org.somda.sdc.proto.mapping;

import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.model.participant.SystemContextDescriptor;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.biceps.model.participant.factory.CodedValueFactory;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.testutil.Handles;
import org.somda.sdc.proto.UnitTestUtil;

import java.math.BigInteger;
import java.time.Instant;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EnsembleContextRoundTrip implements BiConsumer<LocalMdibAccess, RemoteMdibAccess> {
    public EnsembleContextRoundTrip(MdibDescriptionModifications modifications) {
        var descriptor = new EnsembleContextDescriptor();
        {
            descriptor.setHandle(Handles.CONTEXTDESCRIPTOR_0);
            descriptor.setSafetyClassification(SafetyClassification.MED_B);
            descriptor.setType(CodedValueFactory.createIeeeCodedValue("12345"));
        }

        var state = new EnsembleContextState();
        {
            state.setHandle(Handles.CONTEXT_0);
            state.setContextAssociation(ContextAssociation.DIS);
            state.setBindingMdibVersion(BigInteger.TWO);
            state.setBindingStartTime(UnitTestUtil.makeTestTimestamp());
        }

        modifications.insert(descriptor, state, Handles.SYSTEMCONTEXT_0);
    }

    @Override
    public void accept(LocalMdibAccess localMdibAccess, RemoteMdibAccess remoteMdibAccess) {
        var expectedDescriptor = localMdibAccess.getDescriptor(Handles.CONTEXTDESCRIPTOR_0, EnsembleContextDescriptor.class);
        var expectedState = localMdibAccess.getState(Handles.CONTEXT_0, EnsembleContextState.class);
        var actualDescriptor = remoteMdibAccess.getDescriptor(Handles.CONTEXTDESCRIPTOR_0, EnsembleContextDescriptor.class);
        var actualState = remoteMdibAccess.getState(Handles.CONTEXT_0, EnsembleContextState.class);

        assertEquals(expectedDescriptor, actualDescriptor);
        assertEquals(expectedState, actualState);
    }
}