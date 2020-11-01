package org.somda.sdc.proto.mapping.participant;

import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.model.participant.ComponentActivation;
import org.somda.sdc.biceps.model.participant.EnumStringMetricDescriptor;
import org.somda.sdc.biceps.model.participant.EnumStringMetricState;
import org.somda.sdc.biceps.model.participant.MetricAvailability;
import org.somda.sdc.biceps.model.participant.MetricCategory;
import org.somda.sdc.biceps.model.participant.OperatingMode;
import org.somda.sdc.biceps.model.participant.SafetyClassification;
import org.somda.sdc.biceps.model.participant.ScoDescriptor;
import org.somda.sdc.biceps.model.participant.ScoState;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.testutil.Handles;
import org.somda.sdc.proto.mapping.TypeCollection;

import java.math.BigInteger;
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
        var descriptor = new ScoDescriptor();
        {
            descriptor.setHandle(HANDLE);
            descriptor.setDescriptorVersion(BigInteger.valueOf(523));
            descriptor.setSafetyClassification(SafetyClassification.MED_B);

            descriptor.setType(TypeCollection.CODED_VALUE);
            descriptor.setProductionSpecification(List.of(TypeCollection.PRODUCTION_SPECIFICATION));

            // TODO: Extension
//            descriptor.setExtension();
        }

        var state = new ScoState();
        {
            state.setStateVersion(BigInteger.valueOf(634534));
            state.setDescriptorHandle(descriptor.getHandle());
            state.setDescriptorVersion(descriptor.getDescriptorVersion());
            state.setActivationState(ComponentActivation.NOT_RDY);
            state.setOperatingHours(122312L);
            state.setOperatingCycles(7777);

            state.setCalibrationInfo(TypeCollection.CALIBRATION_INFO);
            state.setNextCalibration(TypeCollection.CALIBRATION_INFO);
            state.setPhysicalConnector(TypeCollection.PHYSICAL_CONNECTOR_INFO);

            state.setInvocationRequested(Arrays.asList("d", "e", "f"));
            state.setInvocationRequired(Arrays.asList("a", "b", "c"));

            var operationGroup = new ScoState.OperationGroup();
            operationGroup.setOperatingMode(OperatingMode.NA);
            operationGroup.setOperations(List.of("bra", "bri", "bro"));
            operationGroup.setType(TypeCollection.CODED_VALUE);

            var operationGroup2 = new ScoState.OperationGroup();
            operationGroup2.setType(TypeCollection.CODED_VALUE);

            state.setOperationGroup(List.of(operationGroup, operationGroup2));

            // TODO: Extension
//            state.setExtension();
//            operationGroup.setExtension();
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
            state.setDescriptorHandle(descriptor.getHandle());
        }
        modifications.insert(descriptor, state, Handles.VMD_0);
    }

    @Override
    public void accept(LocalMdibAccess localMdibAccess, RemoteMdibAccess remoteMdibAccess) {
        {
            var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE, ScoDescriptor.class).get();
            var expectedState = localMdibAccess.getState(HANDLE, ScoState.class).get();
            var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE, ScoDescriptor.class).get();
            var actualState = remoteMdibAccess.getState(HANDLE, ScoState.class).get();

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
        {
            var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE_MIN, ScoDescriptor.class).get();
            var expectedState = localMdibAccess.getState(HANDLE_MIN, ScoState.class).get();
            var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE_MIN, ScoDescriptor.class).get();
            var actualState = remoteMdibAccess.getState(HANDLE_MIN, ScoState.class).get();

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
    }
}