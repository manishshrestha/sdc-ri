package org.somda.sdc.proto.mapping.participant;

import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.model.participant.ApprovedJurisdictions;
import org.somda.sdc.biceps.model.participant.ChannelDescriptor;
import org.somda.sdc.biceps.model.participant.ChannelState;
import org.somda.sdc.biceps.model.participant.ComponentActivation;
import org.somda.sdc.biceps.model.participant.MdsDescriptor;
import org.somda.sdc.biceps.model.participant.MdsOperatingMode;
import org.somda.sdc.biceps.model.participant.MdsState;
import org.somda.sdc.biceps.model.participant.OperatingJurisdiction;
import org.somda.sdc.biceps.model.participant.SafetyClassification;
import org.somda.sdc.biceps.model.participant.factory.InstanceIdentifierFactory;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.testutil.Handles;
import org.somda.sdc.common.util.AnyDateTime;
import org.somda.sdc.proto.UnitTestUtil;
import org.somda.sdc.proto.mapping.TypeCollection;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class MdsRoundTrip implements BiConsumer<LocalMdibAccess, RemoteMdibAccess> {
    private static final String HANDLE = Handles.MDS_0;
    private static final String HANDLE_MIN = Handles.MDS_1;
    private static final String HANDLE_MED = Handles.MDS_2;

    public MdsRoundTrip(MdibDescriptionModifications modifications) {
        bigSet(modifications);
        mediumSet(modifications);
        minimalSet(modifications);
    }

    // everything fully formed
    private void bigSet(MdibDescriptionModifications modifications) {
        var descriptor = new MdsDescriptor();
        {
            descriptor.setHandle(HANDLE);
            descriptor.setDescriptorVersion(BigInteger.valueOf(444));
            descriptor.setSafetyClassification(SafetyClassification.INF);

            descriptor.setType(TypeCollection.CODED_VALUE);

            descriptor.setProductionSpecification(List.of(TypeCollection.PRODUCTION_SPECIFICATION));

            var udi = new MdsDescriptor.MetaData.Udi();
            udi.setDeviceIdentifier("☃");
            udi.setHumanReadableForm("Snowman");
            udi.setIssuer(TypeCollection.INSTANCE_IDENTIFIER);
            udi.setJurisdiction(TypeCollection.INSTANCE_IDENTIFIER);

            var udi2 = new MdsDescriptor.MetaData.Udi();
            udi2.setDeviceIdentifier("☕");
            udi2.setHumanReadableForm("Coffee");
            udi2.setIssuer(TypeCollection.INSTANCE_IDENTIFIER);

            var metadata = new MdsDescriptor.MetaData();
            metadata.setUdi(List.of(udi, udi2));
            metadata.setLotNumber("4");
            metadata.setManufacturer(List.of(TypeCollection.LOCALIZED_TEXT));
            metadata.setManufactureDate(AnyDateTime.create(LocalDateTime.now()));
            metadata.setExpirationDate(AnyDateTime.create(LocalDateTime.now()));
            metadata.setModelName(List.of(TypeCollection.LOCALIZED_TEXT, TypeCollection.LOCALIZED_TEXT));
            metadata.setModelNumber("123-321");
            metadata.setSerialNumber(List.of("123", "321"));

            descriptor.setMetaData(metadata);

            var approvedJurisdictions = new ApprovedJurisdictions();
            approvedJurisdictions.getApprovedJurisdiction()
                    .add(InstanceIdentifierFactory.createInstanceIdentifier("http://test/", "extension"));

            descriptor.setApprovedJurisdictions(approvedJurisdictions);

            // TODO: Extension
//            descriptor.setExtension();
//            metadata.setExpirationDate();
//            udi.setExtension();
        }

        var state = new MdsState();
        {
            state.setStateVersion(BigInteger.valueOf(5456));
            state.setDescriptorHandle(descriptor.getHandle());
            state.setDescriptorVersion(descriptor.getDescriptorVersion());
            state.setActivationState(ComponentActivation.NOT_RDY);
            state.setOperatingHours(5L);
            state.setOperatingCycles(1000);

            state.setCalibrationInfo(TypeCollection.CALIBRATION_INFO);
            state.setNextCalibration(TypeCollection.CALIBRATION_INFO);
            state.setPhysicalConnector(TypeCollection.PHYSICAL_CONNECTOR_INFO);

            state.setLang("de");
            state.setOperatingMode(MdsOperatingMode.MTN);

            var operatingJurisdiction = new OperatingJurisdiction();
            operatingJurisdiction.setRootName("http://full-qualifying-root");

            state.setOperatingJurisdiction(operatingJurisdiction);

            // TODO: Extension
//            state.setExtension();
        }

        modifications.insert(descriptor, state);
    }

    // every optional field in optional mds children (metadata, approved jurisdictions) empty
    private void mediumSet(MdibDescriptionModifications modifications) {
        var descriptor = new MdsDescriptor();
        {
            descriptor.setHandle(HANDLE_MED);

            var metadata = new MdsDescriptor.MetaData();

            descriptor.setMetaData(metadata);

            var approvedJurisdictions = new ApprovedJurisdictions();
            descriptor.setApprovedJurisdictions(approvedJurisdictions);
        }

        var state = new MdsState();
        {
            state.setDescriptorHandle(descriptor.getHandle());
        }

        modifications.insert(descriptor, state);
    }

    // everything optional empty
    private void minimalSet(MdibDescriptionModifications modifications) {
        var descriptor = new MdsDescriptor();
        {
            descriptor.setHandle(HANDLE_MIN);
        }

        var state = new MdsState();
        {
            state.setDescriptorHandle(descriptor.getHandle());
        }

        modifications.insert(descriptor, state);
    }

    @Override
    public void accept(LocalMdibAccess localMdibAccess, RemoteMdibAccess remoteMdibAccess) {
        {
            var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE, MdsDescriptor.class);
            var expectedState = localMdibAccess.getState(HANDLE, MdsState.class);
            var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE, MdsDescriptor.class);
            var actualState = remoteMdibAccess.getState(HANDLE, MdsState.class);

            // if everything is empty, everything is equal...
            assertFalse(actualDescriptor.isEmpty());
            assertFalse(actualState.isEmpty());

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
        {
            var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE_MED, MdsDescriptor.class);
            var expectedState = localMdibAccess.getState(HANDLE_MED, MdsState.class);
            var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE_MED, MdsDescriptor.class);
            var actualState = remoteMdibAccess.getState(HANDLE_MED, MdsState.class);

            // if everything is empty, everything is equal...
            assertFalse(actualDescriptor.isEmpty());
            assertFalse(actualState.isEmpty());

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
        {
            var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE_MIN, MdsDescriptor.class);
            var expectedState = localMdibAccess.getState(HANDLE_MIN, MdsState.class);
            var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE_MIN, MdsDescriptor.class);
            var actualState = remoteMdibAccess.getState(HANDLE_MIN, MdsState.class);

            // if everything is empty, everything is equal...
            assertFalse(actualDescriptor.isEmpty());
            assertFalse(actualState.isEmpty());

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
    }
}
