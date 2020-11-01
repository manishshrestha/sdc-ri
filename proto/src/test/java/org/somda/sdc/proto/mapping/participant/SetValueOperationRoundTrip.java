package org.somda.sdc.proto.mapping.participant;

import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.model.participant.AbstractOperationDescriptor;
import org.somda.sdc.biceps.model.participant.ActivateOperationDescriptor;
import org.somda.sdc.biceps.model.participant.OperatingMode;
import org.somda.sdc.biceps.model.participant.Range;
import org.somda.sdc.biceps.model.participant.SafetyClassification;
import org.somda.sdc.biceps.model.participant.SetValueOperationDescriptor;
import org.somda.sdc.biceps.model.participant.SetValueOperationState;
import org.somda.sdc.biceps.model.participant.factory.CodedValueFactory;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.testutil.Handles;
import org.somda.sdc.proto.mapping.TypeCollection;

import javax.xml.namespace.QName;
import java.math.BigInteger;
import java.time.Duration;
import java.util.List;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SetValueOperationRoundTrip implements BiConsumer<LocalMdibAccess, RemoteMdibAccess> {
    private static final String HANDLE = Handles.OPERATION_6;
    private static final String HANDLE_MIN = HANDLE + "Min";

    SetValueOperationRoundTrip(MdibDescriptionModifications modifications) {
        bigSet(modifications);
        minimalSet(modifications);
    }

    private void bigSet(MdibDescriptionModifications modifications) {
        try {
            var descriptor = new SetValueOperationDescriptor();
            {
                descriptor.setHandle(HANDLE);
                descriptor.setDescriptorVersion(BigInteger.valueOf(52345));
                descriptor.setSafetyClassification(SafetyClassification.INF);
                descriptor.setOperationTarget(Handles.MDS_0);
                descriptor.setMaxTimeToFinish(Duration.ofMinutes(12));
                descriptor.setInvocationEffectiveTimeout(Duration.ofMinutes(1));
                descriptor.setRetriggerable(true);
                descriptor.setAccessLevel(AbstractOperationDescriptor.AccessLevel.RO);

                // TODO: Extension
//                descriptor.setExtension();
            }

            var state = new SetValueOperationState();
            {
                state.setStateVersion(BigInteger.valueOf(1423));
                state.setDescriptorHandle(descriptor.getHandle());
                state.setDescriptorVersion(descriptor.getDescriptorVersion());
                state.setOperatingMode(OperatingMode.DIS);

                state.setAllowedRange(List.of(TypeCollection.RANGE, new Range()));

                // TODO: Extension
//                state.setExtension();
            }
            modifications.insert(descriptor, state, Handles.SCO_0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void minimalSet(MdibDescriptionModifications modifications) {
        try {
            var descriptor = new SetValueOperationDescriptor();
            {
                descriptor.setHandle(HANDLE_MIN);
                descriptor.setOperationTarget(Handles.MDS_0);
            }

            var state = new SetValueOperationState();
            {
                state.setDescriptorHandle(descriptor.getHandle());
                state.setOperatingMode(OperatingMode.NA);
            }
            modifications.insert(descriptor, state, Handles.SCO_0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void accept(final LocalMdibAccess localMdibAccess, final RemoteMdibAccess remoteMdibAccess) {
        {
            var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE, SetValueOperationDescriptor.class).get();
            var expectedState = localMdibAccess.getState(HANDLE, SetValueOperationState.class).get();
            var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE, SetValueOperationDescriptor.class).get();
            var actualState = remoteMdibAccess.getState(HANDLE, SetValueOperationState.class).get();

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
        {
            var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE_MIN, SetValueOperationDescriptor.class).get();
            var expectedState = localMdibAccess.getState(HANDLE_MIN, SetValueOperationState.class).get();
            var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE_MIN, SetValueOperationDescriptor.class).get();
            var actualState = remoteMdibAccess.getState(HANDLE_MIN, SetValueOperationState.class).get();

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
    }
}
