package org.somda.sdc.proto.mapping.participant;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.participant.AbstractContextState;
import org.somda.sdc.biceps.model.participant.ContextAssociation;
import org.somda.sdc.biceps.model.participant.EnsembleContextState;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.common.util.TimestampAdapter;
import org.somda.sdc.proto.mapping.Util;
import org.somda.sdc.proto.model.biceps.*;

public class ProtoToPojoContextMapper {
    private static final Logger LOG = LogManager.getLogger(ProtoToPojoContextMapper.class);
    private final Logger instanceLogger;
    private final ProtoToPojoBaseMapper baseMapper;
    private final TimestampAdapter timestampAdapter;
    private final Provider<ProtoToPojoOneOfMapper> oneOfMapperProvider;
    private ProtoToPojoOneOfMapper oneOfMapper;

    @Inject
    ProtoToPojoContextMapper(@Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier,
                             ProtoToPojoBaseMapper baseMapper,
                             Provider<ProtoToPojoOneOfMapper> oneOfMapperProvider,
                             TimestampAdapter timestampAdapter) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.baseMapper = baseMapper;
        this.oneOfMapperProvider = oneOfMapperProvider;
        this.timestampAdapter = timestampAdapter;
        this.oneOfMapper = null;
    }

    public ProtoToPojoOneOfMapper getOneOfMapper() {
        if (this.oneOfMapper == null) {
            this.oneOfMapper = oneOfMapperProvider.get();
        }
        return oneOfMapper;
    }

    public EnsembleContextDescriptor map(EnsembleContextDescriptorMsg protoMsg) {
        var pojoDescriptor = new EnsembleContextDescriptor();
        map(pojoDescriptor, protoMsg.getAbstractContextDescriptor());
        return pojoDescriptor;
    }

    public EnsembleContextState map(EnsembleContextStateMsg protoMsg) {
        var pojoState = new EnsembleContextState();
        map(pojoState, protoMsg.getAbstractContextState());
        return pojoState;
    }

    public LocationContextDescriptor map(LocationContextDescriptorMsg protoMsg) {
        var pojoDescriptor = new LocationContextDescriptor();
        map(pojoDescriptor, protoMsg.getAbstractContextDescriptor());
        return pojoDescriptor;
    }

    public LocationContextState map(LocationContextStateMsg protoMsg) {
        var pojoState = new LocationContextState();
        if (protoMsg.hasLocationDetail()) {
            pojoState.setLocationDetail(baseMapper.map(protoMsg.getLocationDetail()));
        }
        map(pojoState, protoMsg.getAbstractContextState());
        return pojoState;
    }

    public PatientContextDescriptor map(PatientContextDescriptorMsg protoMsg) {
        var pojo = new PatientContextDescriptor();
        map(pojo, protoMsg.getAbstractContextDescriptor());
        return pojo;
    }

    public PatientContextState map(PatientContextStateMsg protoMsg) {
        var pojo = new PatientContextState();
        map(pojo, protoMsg.getAbstractContextState());
        Util.doIfNotNull(Util.optional(protoMsg, "CoreData", PatientDemographicsCoreDataOneOfMsg.class),
                data -> pojo.setCoreData(getOneOfMapper().map(data)));
        return pojo;
    }

    private void map(AbstractContextDescriptor pojo, AbstractContextDescriptorMsg protoMsg) {
        baseMapper.map(pojo, protoMsg.getAbstractDescriptor());
    }

    private void map(AbstractContextState pojo, AbstractContextStateMsg protoMsg) {
        pojo.setBindingEndTime(timestampAdapter.unmarshal(Util.optionalBigIntOfLong(protoMsg, "ABindingEndTime")));
        pojo.setBindingStartTime(timestampAdapter.unmarshal(Util.optionalBigIntOfLong(protoMsg, "ABindingStartTime")));
        pojo.setBindingMdibVersion(Util.optionalBigIntOfLong(protoMsg, "ABindingMdibVersion"));
        pojo.setUnbindingMdibVersion(Util.optionalBigIntOfLong(protoMsg, "AUnbindingMdibVersion"));
        pojo.setContextAssociation(Util.mapToPojoEnum(protoMsg, "AContextAssociation", ContextAssociation.class));
        pojo.setIdentification(baseMapper.mapInstanceIdentifiers(protoMsg.getIdentificationList()));
        pojo.setValidator(baseMapper.mapInstanceIdentifiers(protoMsg.getValidatorList()));
        baseMapper.map(pojo, protoMsg.getAbstractMultiState());
    }


    public PersonReference map(PersonReferenceMsg protoMsg) {
        var pojo = new PersonReference();
        map(pojo, protoMsg);
        return pojo;
    }

    public PersonParticipation map(PersonParticipationMsg protoMsg) {
        var pojo = new PersonParticipation();
        map(pojo, protoMsg.getPersonReference());
        protoMsg.getRoleList().forEach(role -> pojo.getRole().add(baseMapper.map(role)));
        return pojo;
    }

    private void map(PersonReference pojo, PersonReferenceMsg protoMsg) {
        protoMsg.getIdentificationList()
                .forEach(identification ->
                        pojo.getIdentification().add(getOneOfMapper().map(identification)));
        Util.doIfNotNull(Util.optional(protoMsg, "Name", BaseDemographicsOneOfMsg.class),
                name -> pojo.setName(getOneOfMapper().map(name)));
    }

    public NeonatalPatientDemographicsCoreData map(NeonatalPatientDemographicsCoreDataMsg protoMsg) {
        var pojo = new NeonatalPatientDemographicsCoreData();
        map(pojo, protoMsg.getPatientDemographicsCoreData());
        Util.doIfNotNull(Util.optional(protoMsg, "GestationalAge", MeasurementMsg.class),
                age -> pojo.setGestationalAge(baseMapper.map(age)));
        Util.doIfNotNull(Util.optional(protoMsg, "BirthLength", MeasurementMsg.class),
                length -> pojo.setBirthLength(baseMapper.map(length)));
        Util.doIfNotNull(Util.optional(protoMsg, "BirthWeight", MeasurementMsg.class),
                weight -> pojo.setBirthWeight(baseMapper.map(weight)));
        Util.doIfNotNull(Util.optional(protoMsg, "HeadCircumference", MeasurementMsg.class),
                head -> pojo.setHeadCircumference(baseMapper.map(head)));
        Util.doIfNotNull(Util.optional(protoMsg, "Mother", PersonReferenceOneOfMsg.class),
                mother -> pojo.setMother(getOneOfMapper().map(mother)));
        return pojo;
    }

    public PatientDemographicsCoreData map(PatientDemographicsCoreDataMsg protoMsg) {
        var pojo = new PatientDemographicsCoreData();
        map(pojo, protoMsg);
        return pojo;
    }

    private void map(PatientDemographicsCoreData pojo, PatientDemographicsCoreDataMsg protoMsg) {
        baseMapper.map(pojo, protoMsg.getBaseDemographics());
        pojo.setSex(Util.mapToPojoEnum(protoMsg, "Sex", Sex.class));
        pojo.setPatientType(Util.mapToPojoEnum(protoMsg, "PatientType", PatientType.class));
        // TODO: Handle DoB
//        pojo.setDateOfBirth();
        Util.doIfNotNull(Util.optional(protoMsg, "Height", MeasurementMsg.class),
                height -> pojo.setHeight(baseMapper.map(height)));
        Util.doIfNotNull(Util.optional(protoMsg, "Weight", MeasurementMsg.class),
                weight -> pojo.setWeight(baseMapper.map(weight)));
        Util.doIfNotNull(Util.optional(protoMsg, "Race", CodedValueMsg.class),
                value -> pojo.setRace(baseMapper.map(value)));
    }
}
