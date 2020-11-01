package org.somda.sdc.proto.mapping.participant;

import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.model.participant.AbstractOperationDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractOperationState;
import org.somda.sdc.biceps.model.participant.AbstractSetStateOperationDescriptor;
import org.somda.sdc.biceps.model.participant.NumericMetricDescriptor;
import org.somda.sdc.biceps.model.participant.NumericMetricState;
import org.somda.sdc.biceps.model.participant.OperatingMode;
import org.somda.sdc.biceps.model.participant.SafetyClassification;
import org.somda.sdc.biceps.model.participant.SetMetricStateOperationState;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.testutil.Handles;
import org.somda.sdc.proto.mapping.TypeCollection;

import java.math.BigInteger;
import java.time.Duration;
import java.util.Arrays;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

abstract public class OperationRoundTrip<
        T extends AbstractSetStateOperationDescriptor,
        V extends AbstractOperationState> implements BiConsumer<LocalMdibAccess, RemoteMdibAccess> {
    private final Class<T> descrClass;
    private final Class<V> stateClass;

    private final String handle;
    private final String handleMin;

    protected OperationRoundTrip(MdibDescriptionModifications modifications,
                       String handle,
                       Class<T> descrClass,
                       Class<V> stateClass) {
        this.descrClass = descrClass;
        this.stateClass = stateClass;
        this.handle = handle;
        this.handleMin = handle + "Min";

        bigSet(modifications);
        minimalSet(modifications);
    }

    private void bigSet(MdibDescriptionModifications modifications) {
        try {
            var descriptor = descrClass.getConstructor().newInstance();
            {
                descriptor.setHandle(handle);
                descriptor.setDescriptorVersion(BigInteger.valueOf(62391));
                descriptor.setSafetyClassification(SafetyClassification.MED_C);
                descriptor.setOperationTarget(Handles.MDS_0);
                descriptor.setMaxTimeToFinish(Duration.ofMinutes(12));
                descriptor.setInvocationEffectiveTimeout(Duration.ofMinutes(1));
                descriptor.setRetriggerable(false);
                descriptor.setAccessLevel(AbstractOperationDescriptor.AccessLevel.RO);

                descriptor.setType(TypeCollection.CODED_VALUE);
                descriptor.setModifiableData(Arrays.asList("a", "b", "c"));

                // TODO: Extension
//                descriptor.setExtension();
            }

            var state = stateClass.getConstructor().newInstance();
            {
                state.setStateVersion(BigInteger.valueOf(23480));
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

    private void minimalSet(MdibDescriptionModifications modifications) {
        try {
            var descriptor = descrClass.getConstructor().newInstance();
            {
                descriptor.setHandle(handleMin);
                descriptor.setOperationTarget(Handles.MDS_0);
            }

            var state = stateClass.getConstructor().newInstance();
            {
                state.setDescriptorHandle(descriptor.getHandle());
                state.setOperatingMode(OperatingMode.DIS);
            }
            modifications.insert(descriptor, state, Handles.SCO_0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void accept(final LocalMdibAccess localMdibAccess, final RemoteMdibAccess remoteMdibAccess) {
        {
            var expectedDescriptor = localMdibAccess.getDescriptor(handle, descrClass).get();
            var expectedState = localMdibAccess.getState(handle, stateClass).get();
            var actualDescriptor = remoteMdibAccess.getDescriptor(handle, descrClass).get();
            var actualState = remoteMdibAccess.getState(handle, stateClass).get();

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
        {
            var expectedDescriptor = localMdibAccess.getDescriptor(handleMin, descrClass).get();
            var expectedState = localMdibAccess.getState(handleMin, stateClass).get();
            var actualDescriptor = remoteMdibAccess.getDescriptor(handleMin, descrClass).get();
            var actualState = remoteMdibAccess.getState(handleMin, stateClass).get();

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
    }
}
