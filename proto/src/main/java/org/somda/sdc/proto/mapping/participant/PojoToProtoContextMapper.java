package org.somda.sdc.proto.mapping.participant;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.participant.AbstractContextDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractContextState;
import org.somda.sdc.biceps.model.participant.EnsembleContextDescriptor;
import org.somda.sdc.biceps.model.participant.EnsembleContextState;
import org.somda.sdc.biceps.model.participant.LocationContextDescriptor;
import org.somda.sdc.biceps.model.participant.LocationContextState;
import org.somda.sdc.biceps.model.participant.LocationDetail;
import org.somda.sdc.biceps.model.participant.NeonatalPatientDemographicsCoreData;
import org.somda.sdc.biceps.model.participant.PatientContextDescriptor;
import org.somda.sdc.biceps.model.participant.PatientContextState;
import org.somda.sdc.biceps.model.participant.PatientDemographicsCoreData;
import org.somda.sdc.biceps.model.participant.PersonParticipation;
import org.somda.sdc.biceps.model.participant.PersonReference;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.common.util.TimestampAdapter;
import org.somda.sdc.proto.mapping.Util;
import org.somda.sdc.proto.model.biceps.*;

public class PojoToProtoContextMapper {
    private static final Logger LOG = LogManager.getLogger(PojoToProtoContextMapper.class);
    private final Logger instanceLogger;
    private final TimestampAdapter timestampAdapter;
    private final PojoToProtoBaseMapper baseMapper;
    private final Provider<PojoToProtoOneOfMapper> oneOfMapperProvider;
    private PojoToProtoOneOfMapper oneOfMapper;

    @Inject
    PojoToProtoContextMapper(@Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier,
                             TimestampAdapter timestampAdapter,
                             PojoToProtoBaseMapper baseMapper,
                             Provider<PojoToProtoOneOfMapper> oneOfMapperProvider) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.timestampAdapter = timestampAdapter;
        this.baseMapper = baseMapper;
        this.oneOfMapperProvider = oneOfMapperProvider;
        this.oneOfMapper = null;
    }

    public PojoToProtoOneOfMapper getOneOfMapper() {
        if (this.oneOfMapper == null) {
            this.oneOfMapper = oneOfMapperProvider.get();
        }
        return oneOfMapper;
    }

    public EnsembleContextDescriptorMsg.Builder mapEnsembleContextDescriptor(
            EnsembleContextDescriptor ensembleContextDescriptor) {
        return EnsembleContextDescriptorMsg.newBuilder()
                .setAbstractContextDescriptor(mapAbstractContextDescriptor(ensembleContextDescriptor));
    }

    public EnsembleContextStateMsg.Builder mapEnsembleContextState(
            EnsembleContextState ensembleContextState) {
        return EnsembleContextStateMsg.newBuilder()
                .setAbstractContextState(mapAbstractContextState(ensembleContextState));
    }

    public LocationContextDescriptorMsg.Builder mapLocationContextDescriptor(
            LocationContextDescriptor locationContextDescriptor) {
        return LocationContextDescriptorMsg.newBuilder()
                .setAbstractContextDescriptor(mapAbstractContextDescriptor(locationContextDescriptor));
    }

    public LocationContextStateMsg.Builder mapLocationContextState(
            LocationContextState locationContextState) {
        var builder = LocationContextStateMsg.newBuilder()
                .setAbstractContextState(mapAbstractContextState(locationContextState));

        Util.doIfNotNull(locationContextState.getLocationDetail(), detail ->
                builder.setLocationDetail(mapLocationDetail(detail)));
        return builder;
    }

    public LocationDetailMsg mapLocationDetail(LocationDetail locationDetail) {
        var builder = LocationDetailMsg.newBuilder();

        Util.doIfNotNull(locationDetail.getPoC(), poc -> builder.setAPoC(Util.toStringValue(poc)));
        Util.doIfNotNull(locationDetail.getRoom(), room -> builder.setARoom(Util.toStringValue(room)));
        Util.doIfNotNull(locationDetail.getBed(), bed -> builder.setABed(Util.toStringValue(bed)));
        Util.doIfNotNull(locationDetail.getFacility(), facility -> builder.setAFacility(Util.toStringValue(facility)));
        Util.doIfNotNull(locationDetail.getBuilding(), building -> builder.setABuilding(Util.toStringValue(building)));
        Util.doIfNotNull(locationDetail.getFloor(), floor -> builder.setAFloor(Util.toStringValue(floor)));

        return builder.build();
    }

    public PatientContextDescriptorMsg.Builder mapPatientContextDescriptor(PatientContextDescriptor descriptor) {
        return PatientContextDescriptorMsg.newBuilder()
                .setAbstractContextDescriptor(mapAbstractContextDescriptor(descriptor));
    }

    public PatientContextStateMsg.Builder mapPatientContextState(PatientContextState state) {
        var builder = PatientContextStateMsg.newBuilder()
                .setAbstractContextState(mapAbstractContextState(state));
        Util.doIfNotNull(state.getCoreData(),
                coreData -> builder.setCoreData(getOneOfMapper().mapPatientDemographicsCoreDataOneOf(coreData)));
        return builder;
    }

    public AbstractContextDescriptorMsg mapAbstractContextDescriptor(
            AbstractContextDescriptor contextDescriptor) {
        return AbstractContextDescriptorMsg.newBuilder()
                .setAbstractDescriptor(baseMapper.mapAbstractDescriptor(contextDescriptor)).build();
    }

    public AbstractContextStateMsg mapAbstractContextState(AbstractContextState contextState) {
        var builder = AbstractContextStateMsg.newBuilder();
        Util.doIfNotNull(contextState.getBindingStartTime(), it ->
                builder.setABindingStartTime(baseMapper.mapTimestamp(timestampAdapter.marshal(it))));
        Util.doIfNotNull(contextState.getBindingEndTime(), it ->
                builder.setABindingEndTime(baseMapper.mapTimestamp(timestampAdapter.marshal(it))));
        Util.doIfNotNull(contextState.getBindingMdibVersion(), it ->
                builder.setABindingMdibVersion(baseMapper.mapReferencedVersion(it)));
        Util.doIfNotNull(contextState.getUnbindingMdibVersion(), it ->
                builder.setAUnbindingMdibVersion(baseMapper.mapReferencedVersion(it)));
        Util.doIfNotNull(
                contextState.getContextAssociation(),
                assoc -> builder.setAContextAssociation(Util.mapToProtoEnum(assoc, ContextAssociationMsg.class))
        );
        Util.doIfNotNull(contextState.getIdentification(), it ->
                builder.addAllIdentification(baseMapper.mapInstanceIdentifiers(it)));
        Util.doIfNotNull(contextState.getValidator(), it ->
                builder.addAllValidator(baseMapper.mapInstanceIdentifiers(it)));
        builder.setAbstractMultiState(baseMapper.mapAbstractMultiState(contextState));
        return builder.build();
    }

    PersonParticipationMsg mapPersonParticipation(PersonParticipation participation) {
        var builder = PersonParticipationMsg.newBuilder()
                .setPersonReference(mapPersonReference(participation));
        participation.getRole().forEach(role -> builder.addRole(baseMapper.mapCodedValue(role)));
        return builder.build();
    }

    PersonReferenceMsg mapPersonReference(PersonReference personReference) {
        var builder = PersonReferenceMsg.newBuilder();
        personReference.getIdentification()
                .forEach(identification -> builder.addIdentification(getOneOfMapper()
                        .mapInstanceIdentifier(identification)));
        Util.doIfNotNull(personReference.getName(), name ->
                builder.setName(oneOfMapper.mapBaseDemographics(personReference.getName())));
        return builder.build();
    }


    PatientDemographicsCoreDataMsg mapPatientDemographicsCoreData(PatientDemographicsCoreData coreData) {
        var builder = PatientDemographicsCoreDataMsg.newBuilder()
                .setBaseDemographics(baseMapper.mapBaseDemographics(coreData));
        Util.doIfNotNull(coreData.getSex(), sex -> builder.setSex(Util.mapToProtoEnum(sex, SexMsg.class)));
        Util.doIfNotNull(coreData.getPatientType(),
                patientType -> builder.setPatientType(Util.mapToProtoEnum(patientType, PatientTypeMsg.class)));
        // TODO: What do we do with this oddity?
//        coreData.getDateOfBirth() Date
        Util.doIfNotNull(coreData.getWeight(), weight -> builder.setWeight(baseMapper.mapMeasurement(weight)));
        Util.doIfNotNull(coreData.getHeight(), height -> builder.setHeight(baseMapper.mapMeasurement(height)));
        Util.doIfNotNull(coreData.getRace(), race -> builder.setRace(baseMapper.mapCodedValue(race)));
        return builder.build();
    }

    NeonatalPatientDemographicsCoreDataMsg mapNeonatalPatientDemographicsCoreData(
            NeonatalPatientDemographicsCoreData coreData
    ) {
        var builder = NeonatalPatientDemographicsCoreDataMsg.newBuilder()
                .setPatientDemographicsCoreData(mapPatientDemographicsCoreData(coreData));

        Util.doIfNotNull(coreData.getGestationalAge(), age -> builder.setGestationalAge(baseMapper.mapMeasurement(age)));
        Util.doIfNotNull(coreData.getBirthLength(), length -> builder.setBirthLength(baseMapper.mapMeasurement(length)));
        Util.doIfNotNull(coreData.getBirthWeight(), weight -> builder.setBirthWeight(baseMapper.mapMeasurement(weight)));
        Util.doIfNotNull(coreData.getHeadCircumference(), circ -> builder.setHeadCircumference(baseMapper.mapMeasurement(circ)));
        Util.doIfNotNull(coreData.getMother(), mother -> builder.setMother(getOneOfMapper()
                .mapPersonReferenceOneOf(mother)));

        return builder.build();
    }
}
