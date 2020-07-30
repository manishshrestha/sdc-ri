package org.somda.sdc.proto.mapping;

import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.model.participant.ComponentActivation;
import org.somda.sdc.biceps.model.participant.EnumStringMetricDescriptor;
import org.somda.sdc.biceps.model.participant.EnumStringMetricState;
import org.somda.sdc.biceps.model.participant.MetricAvailability;
import org.somda.sdc.biceps.model.participant.MetricCategory;
import org.somda.sdc.biceps.model.participant.ScoDescriptor;
import org.somda.sdc.biceps.model.participant.ScoState;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.testutil.Handles;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ScoRoundTrip implements BiConsumer<LocalMdibAccess, RemoteMdibAccess> {
    private static final String HANDLE = Handles.SCO_0;
    private static final String HANDLE_MIN = HANDLE + "Min";

    public ScoRoundTrip(MdibDescriptionModifications modifications) {
        bigSet(modifications);
        minimalSet(modifications);
    }

    public void bigSet(MdibDescriptionModifications modifications) {
        // TODO: Complete
        var descriptor = new ScoDescriptor();
        {
            descriptor.setHandle(HANDLE);
        }

        var state = new ScoState();
        {
            state.setActivationState(ComponentActivation.NOT_RDY);
            state.setInvocationRequired(Arrays.asList("a", "b", "c"));
            state.setInvocationRequested(Arrays.asList("d", "e", "f"));
        }
        modifications.insert(descriptor, state, Handles.MDS_0);
    }

    public void minimalSet(MdibDescriptionModifications modifications) {
        var descriptor = new ScoDescriptor();
        {
            descriptor.setHandle(HANDLE_MIN);
        }

        var state = new ScoState();
        {
        }
        modifications.insert(descriptor, state, Handles.VMD_0);
    }

    @Override
    public void accept(LocalMdibAccess localMdibAccess, RemoteMdibAccess remoteMdibAccess) {
        {
            var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE, ScoDescriptor.class);
            var expectedState = localMdibAccess.getState(HANDLE, ScoState.class);
            var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE, ScoDescriptor.class);
            var actualState = remoteMdibAccess.getState(HANDLE, ScoState.class);

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
        {
            var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE_MIN, ScoDescriptor.class);
            var expectedState = localMdibAccess.getState(HANDLE_MIN, ScoState.class);
            var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE_MIN, ScoDescriptor.class);
            var actualState = remoteMdibAccess.getState(HANDLE_MIN, ScoState.class);

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
    }
}