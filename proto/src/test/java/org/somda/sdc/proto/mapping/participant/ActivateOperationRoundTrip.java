package org.somda.sdc.proto.mapping.participant;

import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.model.participant.AbstractOperationDescriptor;
import org.somda.sdc.biceps.model.participant.ActivateOperationDescriptor;
import org.somda.sdc.biceps.model.participant.ActivateOperationState;
import org.somda.sdc.biceps.model.participant.OperatingMode;
import org.somda.sdc.biceps.model.participant.SafetyClassification;
import org.somda.sdc.biceps.model.participant.factory.CodedValueFactory;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.testutil.Handles;
import org.somda.sdc.proto.mapping.TypeCollection;

import javax.xml.namespace.QName;
import java.math.BigInteger;
import java.time.Duration;
import java.util.Arrays;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ActivateOperationRoundTrip implements BiConsumer<LocalMdibAccess, RemoteMdibAccess> {
    private static final String HANDLE = Handles.OPERATION_4;
    private static final String HANDLE_MIN = HANDLE + "Min";

    ActivateOperationRoundTrip(MdibDescriptionModifications modifications) {
        bigSet(modifications);
        minimalSet(modifications);
    }

    private void bigSet(MdibDescriptionModifications modifications) {
        try {
            var descriptor = new ActivateOperationDescriptor();
            {
                descriptor.setHandle(HANDLE);
                descriptor.setDescriptorVersion(BigInteger.valueOf(5));
                descriptor.setSafetyClassification(SafetyClassification.MED_A);
                descriptor.setOperationTarget(Handles.MDS_0);
                descriptor.setMaxTimeToFinish(Duration.ofMinutes(12));
                descriptor.setInvocationEffectiveTimeout(Duration.ofMinutes(1));
                descriptor.setRetriggerable(false);
                descriptor.setAccessLevel(AbstractOperationDescriptor.AccessLevel.RO);

                descriptor.setType(TypeCollection.CODED_VALUE);
                descriptor.setModifiableData(Arrays.asList("a", "b", "c"));

                descriptor.setArgument(Arrays.asList(
                        createArg("a"),
                        createArg("b"),
                        createArg("c")));

                // TODO: Extension
//                descriptor.setExtension();
            }

            var state = new ActivateOperationState();
            {
                state.setStateVersion(BigInteger.TEN);
                state.setDescriptorHandle(descriptor.getHandle());
                state.setDescriptorVersion(descriptor.getDescriptorVersion());
                state.setOperatingMode(OperatingMode.DIS);

                // TODO: Extension
//                state.setExtension();
            }
            modifications.insert(descriptor, state, Handles.SCO_0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ActivateOperationDescriptor.Argument createArg(String label) {
        var arg = new ActivateOperationDescriptor.Argument();
        arg.setArgName(TypeCollection.CODED_VALUE);
        arg.setArg(new QName("http://test", label));
        return arg;
    }

    private void minimalSet(MdibDescriptionModifications modifications) {
        try {
            var descriptor = new ActivateOperationDescriptor();
            {
                descriptor.setHandle(HANDLE_MIN);
                descriptor.setOperationTarget(Handles.MDS_0);
            }

            var state = new ActivateOperationState();
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
            var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE, ActivateOperationDescriptor.class);
            var expectedState = localMdibAccess.getState(HANDLE, ActivateOperationState.class);
            var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE, ActivateOperationDescriptor.class);
            var actualState = remoteMdibAccess.getState(HANDLE, ActivateOperationState.class);

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
        {
            var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE_MIN, ActivateOperationDescriptor.class);
            var expectedState = localMdibAccess.getState(HANDLE_MIN, ActivateOperationState.class);
            var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE_MIN, ActivateOperationDescriptor.class);
            var actualState = remoteMdibAccess.getState(HANDLE_MIN, ActivateOperationState.class);

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
    }
}