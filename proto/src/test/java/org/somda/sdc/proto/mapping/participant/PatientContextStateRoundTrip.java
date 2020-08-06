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
import org.somda.sdc.proto.model.biceps.CodedValueMsg;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class PatientContextStateRoundTrip implements BiConsumer<LocalMdibAccess, RemoteMdibAccess> {
    private static final String HANDLE = Handles.CONTEXTDESCRIPTOR_2;
    private static final String HANDLE_MIN = HANDLE + "Min";
    private static final String HANDLE_STATE = Handles.CONTEXT_2;;
    private static final String HANDLE_STATE_MIN = HANDLE_STATE + "Min";


    PatientContextStateRoundTrip(MdibDescriptionModifications modifications) {
        bigSet(modifications);
        minimalSet(modifications);
    }

    void bigSet(MdibDescriptionModifications modifications) {
        var descriptor = new PatientContextDescriptor();
        {
            descriptor.setHandle(HANDLE);
            descriptor.setDescriptorVersion(BigInteger.valueOf(11231235));
            descriptor.setSafetyClassification(SafetyClassification.MED_C);

            var descType = new CodedValue();
            descType.setCode("TopSecrit");
            descType.setCodingSystem("Secrit Codez");
            descType.setCodingSystemVersion("latest graitezt");
            descType.setSymbolicCodeName("Simba-lic");
//            descType.setCodingSystemName();
//            descType.setConceptDescription();
//            descType.setTranslation();
            descriptor.setType(descType);
        }
        var state = new PatientContextState();
        {
            state.setStateVersion(BigInteger.valueOf(42));
            state.setDescriptorHandle(HANDLE);
            state.setDescriptorVersion(descriptor.getDescriptorVersion());
            state.setHandle(HANDLE_STATE);
            // this needs to be set, otherwise MdibEntityImpl drops the state
            state.setContextAssociation(ContextAssociation.ASSOC);
            state.setBindingMdibVersion(BigInteger.valueOf(4332));
            state.setUnbindingMdibVersion(BigInteger.valueOf(4333));
            state.setBindingStartTime(UnitTestUtil.makeTestTimestamp());
            state.setBindingEndTime(UnitTestUtil.makeTestTimestamp());

            var category = new CodedValue();
            category.setCode("TopSecrit");
            category.setCodingSystem("Secrit Codez");
            category.setCodingSystemVersion("latest graitezt");
            category.setSymbolicCodeName("Simba-lic");
//            descType.setCodingSystemName();
//            descType.setConceptDescription();
//            descType.setTranslation();
            state.setCategory(category);
//            state.setValidator();
//            state.setIdentification();

            var coreData = new NeonatalPatientDemographicsCoreData();
            coreData.setGivenname("Leia");
            coreData.getMiddlename().add("Organa");
            coreData.setFamilyname("Solo");
            coreData.setBirthname("Skywalker");
            coreData.setTitle("Princess");
            coreData.setSex(Sex.F);
            coreData.setPatientType(PatientType.AD);
            var height = new Measurement();
            {
                var heightUnit = new CodedValue();
                heightUnit.setCode("km/s^2");
                height.setMeasurementUnit(heightUnit);
                height.setMeasuredValue(BigDecimal.valueOf(1.55));
            }
            coreData.setHeight(height);
            var weight = new Measurement();
            {
                var weightUnit = new CodedValue();
                weightUnit.setCode("kg");
                weight.setMeasurementUnit(weightUnit);
                weight.setMeasuredValue(BigDecimal.valueOf(51));
            }
            coreData.setWeight(weight);

            var noUnitsAvailable = new CodedValue();
            noUnitsAvailable.setCode("Syntax error");

            var iDontGiveAMeasurement = new Measurement();
            iDontGiveAMeasurement.setMeasurementUnit(noUnitsAvailable);
            iDontGiveAMeasurement.setMeasuredValue(BigDecimal.valueOf(1337));

            var identifyYourself = new InstanceIdentifier();
            identifyYourself.setRootName("root");
            identifyYourself.setExtensionName("ext");
            identifyYourself.setType(noUnitsAvailable);
//            identifyYourself.setIdentifierName();

            coreData.setGestationalAge(iDontGiveAMeasurement);
            coreData.setBirthLength(iDontGiveAMeasurement);
            coreData.setBirthWeight(iDontGiveAMeasurement);
            coreData.setHeadCircumference(iDontGiveAMeasurement);

            var iAmMother = new PersonParticipation();
            iAmMother.setIdentification(List.of(identifyYourself));
            iAmMother.setRole(List.of(noUnitsAvailable));

            var motherDemographics = new BaseDemographics();
            motherDemographics.setGivenname("Padmé");
            motherDemographics.setMiddlename(List.of("Skywalker"));
            motherDemographics.setFamilyname("Amidala");
            motherDemographics.setBirthname("Naberrie");
            motherDemographics.setTitle("Senator");
            iAmMother.setName(motherDemographics);

            coreData.setMother(iAmMother);

            state.setCoreData(coreData);
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
            state.setDescriptorHandle(HANDLE_MIN);
            state.setHandle(HANDLE_STATE_MIN);
            // this needs to be set, otherwise MdibEntityImpl drops the state
            state.setContextAssociation(ContextAssociation.ASSOC);
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
