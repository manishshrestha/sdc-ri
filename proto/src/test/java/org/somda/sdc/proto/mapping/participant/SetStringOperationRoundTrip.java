package org.somda.sdc.proto.mapping.participant;

import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.model.participant.AbstractOperationDescriptor;
import org.somda.sdc.biceps.model.participant.ActivateOperationDescriptor;
import org.somda.sdc.biceps.model.participant.ActivateOperationState;
import org.somda.sdc.biceps.model.participant.OperatingMode;
import org.somda.sdc.biceps.model.participant.SetStringOperationDescriptor;
import org.somda.sdc.biceps.model.participant.SetStringOperationState;
import org.somda.sdc.biceps.model.participant.factory.CodedValueFactory;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.testutil.Handles;

import javax.xml.namespace.QName;
import java.time.Duration;
import java.util.Arrays;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SetStringOperationRoundTrip implements BiConsumer<LocalMdibAccess, RemoteMdibAccess> {
    private static final String HANDLE = Handles.OPERATION_5;
    private static final String HANDLE_MIN = HANDLE + "Min";

    SetStringOperationRoundTrip(MdibDescriptionModifications modifications) {
        bigSet(modifications);
        minimalSet(modifications);
    }

    private void bigSet(MdibDescriptionModifications modifications) {
        try {
            // TODO: Complete
            var descriptor = new SetStringOperationDescriptor();
            {
                descriptor.setHandle(HANDLE);
                descriptor.setOperationTarget(Handles.MDS_0);
                descriptor.setAccessLevel(AbstractOperationDescriptor.AccessLevel.RO);
                descriptor.setInvocationEffectiveTimeout(Duration.ofMinutes(1));
                descriptor.setMaxTimeToFinish(Duration.ofMinutes(12));
            }

            var state = new SetStringOperationState();
            {
                state.setOperatingMode(OperatingMode.DIS);
            }
            modifications.insert(descriptor, state, Handles.SCO_0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ActivateOperationDescriptor.Argument createArg(String label) {
        var arg = new ActivateOperationDescriptor.Argument();
        arg.setArgName(CodedValueFactory.createIeeeCodedValue(label));
        arg.setArg(new QName("http://test", label));
        return arg;
    }

    private void minimalSet(MdibDescriptionModifications modifications) {
        try {
            var descriptor = new SetStringOperationDescriptor();
            {
                descriptor.setHandle(HANDLE_MIN);
                descriptor.setOperationTarget(Handles.MDS_0);
            }

            var state = new SetStringOperationState();
            {
            }
            modifications.insert(descriptor, state, Handles.SCO_0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void accept(final LocalMdibAccess localMdibAccess, final RemoteMdibAccess remoteMdibAccess) {
        {
            var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE, SetStringOperationDescriptor.class);
            var expectedState = localMdibAccess.getState(HANDLE, SetStringOperationState.class);
            var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE, SetStringOperationDescriptor.class);
            var actualState = remoteMdibAccess.getState(HANDLE, SetStringOperationState.class);

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
        {
            var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE_MIN, SetStringOperationDescriptor.class);
            var expectedState = localMdibAccess.getState(HANDLE_MIN, SetStringOperationState.class);
            var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE_MIN, SetStringOperationDescriptor.class);
            var actualState = remoteMdibAccess.getState(HANDLE_MIN, SetStringOperationState.class);

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
    }
}
