package org.somda.sdc.proto.mapping.participant;

import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.model.participant.ApprovedJurisdictions;
import org.somda.sdc.biceps.model.participant.ChannelDescriptor;
import org.somda.sdc.biceps.model.participant.ChannelState;
import org.somda.sdc.biceps.model.participant.ComponentActivation;
import org.somda.sdc.biceps.model.participant.OperatingJurisdiction;
import org.somda.sdc.biceps.model.participant.SafetyClassification;
import org.somda.sdc.biceps.model.participant.VmdDescriptor;
import org.somda.sdc.biceps.model.participant.VmdState;
import org.somda.sdc.biceps.model.participant.factory.InstanceIdentifierFactory;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.testutil.Handles;
import org.somda.sdc.proto.mapping.TypeCollection;

import java.math.BigInteger;
import java.util.List;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VmdRoundTrip implements BiConsumer<LocalMdibAccess, RemoteMdibAccess> {

    private static final String HANDLE = Handles.VMD_0;
    private static final String HANDLE_MIN = HANDLE + "Min";

    public VmdRoundTrip(MdibDescriptionModifications modifications) {
        bigSet(modifications);
        minimalSet(modifications);
    }

    private void bigSet(MdibDescriptionModifications modifications) {
        var descriptor = new VmdDescriptor();
        {
            descriptor.setHandle(HANDLE);
            descriptor.setDescriptorVersion(BigInteger.valueOf(8678));
            descriptor.setSafetyClassification(SafetyClassification.MED_C);

            descriptor.setType(TypeCollection.CODED_VALUE);
            descriptor.setProductionSpecification(List.of(TypeCollection.PRODUCTION_SPECIFICATION));

            var approvedJurisdictions = new ApprovedJurisdictions();
            approvedJurisdictions.getApprovedJurisdiction()
                    .add(InstanceIdentifierFactory.createInstanceIdentifier("http://test/", "extension"));
            approvedJurisdictions.getApprovedJurisdiction().add(TypeCollection.INSTANCE_IDENTIFIER);

            descriptor.setApprovedJurisdictions(approvedJurisdictions);

            // TODO: Extension
//            descriptor.setExtension();
        }

        var state = new VmdState();
        {
            state.setStateVersion(BigInteger.valueOf(23890));
            state.setDescriptorHandle(descriptor.getHandle());
            state.setDescriptorVersion(descriptor.getDescriptorVersion());
            state.setActivationState(ComponentActivation.OFF);
            state.setOperatingHours(5L);
            state.setOperatingCycles(1000);

            state.setCalibrationInfo(TypeCollection.CALIBRATION_INFO);
            state.setNextCalibration(TypeCollection.CALIBRATION_INFO);
            state.setPhysicalConnector(TypeCollection.PHYSICAL_CONNECTOR_INFO);

            var operatingJurisdiction = new OperatingJurisdiction();
            operatingJurisdiction.setRootName("http://full-qualifying-root");
            state.setOperatingJurisdiction(operatingJurisdiction);

            // TODO: Extension
//            state.setExtension();
//            operatingJurisdiction.setExtension();
        }

        modifications.insert(descriptor, state, Handles.MDS_0);
    }

    private void minimalSet(MdibDescriptionModifications modifications) {
        var descriptor = new VmdDescriptor();
        {
            descriptor.setHandle(HANDLE_MIN);
        }

        var state = new VmdState();
        {
            state.setDescriptorHandle(descriptor.getHandle());
        }

        modifications.insert(descriptor, state, Handles.MDS_0);
    }

    @Override
    public void accept(LocalMdibAccess localMdibAccess, RemoteMdibAccess remoteMdibAccess) {
        {
            var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE, VmdDescriptor.class).get();
            var expectedState = localMdibAccess.getState(HANDLE, VmdState.class).get();
            var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE, VmdDescriptor.class).get();
            var actualState = remoteMdibAccess.getState(HANDLE, VmdState.class).get();

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
        {
            var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE_MIN, VmdDescriptor.class).get();
            var expectedState = localMdibAccess.getState(HANDLE_MIN, VmdState.class).get();
            var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE_MIN, VmdDescriptor.class).get();
            var actualState = remoteMdibAccess.getState(HANDLE_MIN, VmdState.class).get();

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
    }
}