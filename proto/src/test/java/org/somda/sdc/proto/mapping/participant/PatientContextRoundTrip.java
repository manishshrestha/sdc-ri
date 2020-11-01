package org.somda.sdc.proto.mapping.participant;

import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.model.participant.BaseDemographics;
import org.somda.sdc.biceps.model.participant.CodedValue;
import org.somda.sdc.biceps.model.participant.ContextAssociation;
import org.somda.sdc.biceps.model.participant.InstanceIdentifier;
import org.somda.sdc.biceps.model.participant.Measurement;
import org.somda.sdc.biceps.model.participant.NeonatalPatientDemographicsCoreData;
import org.somda.sdc.biceps.model.participant.PatientContextDescriptor;
import org.somda.sdc.biceps.model.participant.PatientContextState;
import org.somda.sdc.biceps.model.participant.PatientType;
import org.somda.sdc.biceps.model.participant.PersonParticipation;
import org.somda.sdc.biceps.model.participant.PersonReference;
import org.somda.sdc.biceps.model.participant.SafetyClassification;
import org.somda.sdc.biceps.model.participant.Sex;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.biceps.testutil.Handles;
import org.somda.sdc.proto.UnitTestUtil;
import org.somda.sdc.proto.mapping.TypeCollection;
import org.somda.sdc.proto.model.biceps.CodedValueMsg;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class PatientContextRoundTrip implements BiConsumer<LocalMdibAccess, RemoteMdibAccess> {
    private static final String HANDLE = Handles.CONTEXTDESCRIPTOR_2;
    private static final String HANDLE_MIN = HANDLE + "Min";
    private static final String HANDLE_STATE = Handles.CONTEXT_2;;
    private static final String HANDLE_STATE_MIN = HANDLE_STATE + "Min";


    PatientContextRoundTrip(MdibDescriptionModifications modifications) {
        bigSet(modifications);
        minimalSet(modifications);
    }

    void bigSet(MdibDescriptionModifications modifications) {
        var descriptor = new PatientContextDescriptor();
        {
            descriptor.setHandle(HANDLE);
            descriptor.setDescriptorVersion(BigInteger.valueOf(11231235));
            descriptor.setSafetyClassification(SafetyClassification.MED_C);

            descriptor.setType(TypeCollection.CODED_VALUE);

            // TODO: Extension
//            descriptor.setExtension();
        }
        var state = new PatientContextState();
        {
            state.setStateVersion(BigInteger.valueOf(42));
            state.setDescriptorHandle(descriptor.getHandle());
            state.setDescriptorVersion(descriptor.getDescriptorVersion());
            state.setHandle(HANDLE_STATE);
            state.setContextAssociation(ContextAssociation.PRE);
            state.setBindingMdibVersion(BigInteger.valueOf(4332));
            state.setUnbindingMdibVersion(BigInteger.valueOf(4333));
            state.setBindingStartTime(UnitTestUtil.makeTestTimestamp());
            state.setBindingEndTime(UnitTestUtil.makeTestTimestamp());

            state.setCategory(TypeCollection.CODED_VALUE);
            state.setValidator(List.of(TypeCollection.INSTANCE_IDENTIFIER));
            state.setIdentification(List.of(TypeCollection.INSTANCE_IDENTIFIER, TypeCollection.INSTANCE_IDENTIFIER));

            var coreData = new NeonatalPatientDemographicsCoreData();
            coreData.setGivenname("Leia");
            coreData.getMiddlename().add("Organa");
            coreData.setFamilyname("Solo");
            coreData.setBirthname("Skywalker");
            coreData.setTitle("Princess");
            coreData.setSex(Sex.F);
            coreData.setPatientType(PatientType.AD);
            coreData.setHeight(TypeCollection.MEASUREMENT);
            coreData.setWeight(TypeCollection.MEASUREMENT);
            coreData.setRace(TypeCollection.CODED_VALUE);

            coreData.setGestationalAge(TypeCollection.MEASUREMENT);
            coreData.setBirthLength(TypeCollection.MEASUREMENT);
            coreData.setBirthWeight(TypeCollection.MEASUREMENT);
            coreData.setHeadCircumference(TypeCollection.MEASUREMENT);

            var iAmMother = new PersonParticipation();
            iAmMother.setIdentification(List.of(TypeCollection.INSTANCE_IDENTIFIER));
            iAmMother.setRole(List.of(TypeCollection.CODED_VALUE));

            var motherDemographics = new BaseDemographics();
            motherDemographics.setGivenname("Padmé");
            motherDemographics.setMiddlename(List.of("Skywalker"));
            motherDemographics.setFamilyname("Amidala");
            motherDemographics.setBirthname("Naberrie");
            motherDemographics.setTitle("Senator");
            iAmMother.setName(motherDemographics);

            coreData.setMother(iAmMother);

            state.setCoreData(coreData);

            // TODO: Extension
//            state.setExtension();
//            coreData.setExtension();
//            motherDemographics.setExtension();
        }

        modifications.insert(descriptor, state, Handles.SYSTEMCONTEXT_0);
    }

    void minimalSet(MdibDescriptionModifications modifications) {
        var descriptor = new PatientContextDescriptor();
        {
            descriptor.setHandle(HANDLE_MIN);
        }
        var state = new PatientContextState();
        {
            state.setDescriptorHandle(descriptor.getHandle());
            state.setHandle(HANDLE_STATE_MIN);
        }

        modifications.insert(descriptor, state, Handles.SYSTEMCONTEXT_1);
    }

    @Override
    public void accept(final LocalMdibAccess localMdibAccess, final RemoteMdibAccess remoteMdibAccess) {
        {
            var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE, PatientContextDescriptor.class);
            var expectedState = localMdibAccess.getState(HANDLE_STATE, PatientContextState.class);
            var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE, PatientContextDescriptor.class);
            var actualState = remoteMdibAccess.getState(HANDLE_STATE, PatientContextState.class);

            // if everything is empty, everything is equal...
            assertFalse(actualDescriptor.isEmpty());
            assertFalse(actualState.isEmpty());

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
        {
            var expectedDescriptor = localMdibAccess.getDescriptor(HANDLE_MIN, PatientContextDescriptor.class);
            var expectedState = localMdibAccess.getState(HANDLE_STATE_MIN, PatientContextState.class);
            var actualDescriptor = remoteMdibAccess.getDescriptor(HANDLE_MIN, PatientContextDescriptor.class);
            var actualState = remoteMdibAccess.getState(HANDLE_STATE_MIN, PatientContextState.class);

            // if everything is empty, everything is equal...
            assertFalse(actualDescriptor.isEmpty());
            assertFalse(actualState.isEmpty());

            assertEquals(expectedDescriptor, actualDescriptor);
            assertEquals(expectedState, actualState);
        }
    }
}
