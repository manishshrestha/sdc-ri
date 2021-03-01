package org.somda.sdc.proto.mapping.participant;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.common.util.AnyDateTimeAdapter;
import org.somda.sdc.common.util.TimestampAdapter;
import org.somda.sdc.proto.mapping.Util;
import org.somda.sdc.proto.model.biceps.*;

import java.util.List;
import java.util.stream.Collectors;

public class PojoToProtoComponentMapper {
    private static final Logger LOG = LogManager.getLogger(PojoToProtoComponentMapper.class);
    private final Logger instanceLogger;
    private final PojoToProtoBaseMapper baseMapper;
    private final PojoToProtoAlertMapper alertMapper;
    private final Provider<PojoToProtoOneOfMapper> oneOfMapperProvider;
    private final TimestampAdapter timestampAdapter;
    private final AnyDateTimeAdapter anyDateTimeAdapter;
    private PojoToProtoOneOfMapper oneOfMapper;

    @Inject
    PojoToProtoComponentMapper(@Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier,
                               PojoToProtoBaseMapper baseMapper,
                               PojoToProtoAlertMapper alertMapper,
                               Provider<PojoToProtoOneOfMapper> oneOfMapperProvider,
                               TimestampAdapter timestampAdapter,
                               AnyDateTimeAdapter anyDateTimeAdapter) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.baseMapper = baseMapper;
        this.alertMapper = alertMapper;
        this.oneOfMapperProvider = oneOfMapperProvider;
        this.oneOfMapper = null;
        this.timestampAdapter = timestampAdapter;
        this.anyDateTimeAdapter = anyDateTimeAdapter;
    }

    public PojoToProtoOneOfMapper getOneOfMapper() {
        if (this.oneOfMapper == null) {
            this.oneOfMapper = oneOfMapperProvider.get();
        }
        return oneOfMapper;
    }

    public SystemContextDescriptorMsg.Builder mapSystemContextDescriptor(SystemContextDescriptor systemContextDescriptor) {
        return SystemContextDescriptorMsg.newBuilder()
                .setAbstractDeviceComponentDescriptor(
                        mapAbstractDeviceComponentDescriptor(systemContextDescriptor));
    }

    public SystemContextStateMsg mapSystemContextState(SystemContextState systemContextState) {
        return SystemContextStateMsg.newBuilder()
                .setAbstractDeviceComponentState(mapAbstractDeviceComponentState(systemContextState)).build();
    }

    public ScoDescriptorMsg.Builder mapScoDescriptor(ScoDescriptor scoDescriptor) {
        return ScoDescriptorMsg.newBuilder()
                .setAbstractDeviceComponentDescriptor(mapAbstractDeviceComponentDescriptor(scoDescriptor));
    }

    public ScoStateMsg mapScoState(ScoState scoState) {
        var builder = ScoStateMsg.newBuilder();

        if (!scoState.getInvocationRequested().isEmpty()) {
            builder.setAInvocationRequested(mapOperationRefs(scoState.getInvocationRequested()));
        }
        if (!scoState.getInvocationRequired().isEmpty()) {
            builder.setAInvocationRequired(mapOperationRefs(scoState.getInvocationRequired()));
        }

        scoState.getOperationGroup().forEach(it -> builder.addOperationGroup(mapOperationGroup(it)));

        return builder.setAbstractDeviceComponentState(mapAbstractDeviceComponentState(scoState)).build();
    }

    public OperationRefMsg mapOperationRefs(List<String> opRefs) {
        var builder = OperationRefMsg.newBuilder();
        opRefs.forEach(ref -> builder.addHandleRef(baseMapper.mapHandleRef(ref)));
        return builder.build();
    }

    public ScoStateMsg.OperationGroupMsg mapOperationGroup(ScoState.OperationGroup group) {
        var builder = ScoStateMsg.OperationGroupMsg.newBuilder();
        Util.doIfNotNull(group.getOperatingMode(), it -> builder.setAOperatingMode(Util.mapToProtoEnum(it, OperatingModeMsg.class)));
        if (!group.getOperations().isEmpty()) {
            builder.setAOperations(mapOperationRefs(group.getOperations()));
        }
        builder.setType(baseMapper.mapCodedValue(group.getType()));
        return builder.build();
    }

    public MdsDescriptorMsg.Builder mapMdsDescriptor(MdsDescriptor mdsDescriptor) {
        var builder = MdsDescriptorMsg.newBuilder();
        Util.doIfNotNull(mdsDescriptor.getMetaData(), it ->
                builder.setMetaData(mapMetadata(it)));
        Util.doIfNotNull(mdsDescriptor.getApprovedJurisdictions(), it ->
                builder.setApprovedJurisdictions(mapApprovedJurisdictions(it)));
        return builder.setAbstractComplexDeviceComponentDescriptor(
                mapAbstractComplexDeviceComponentDescriptor(mdsDescriptor));
    }

    public MdsStateMsg mapMdsState(MdsState mdsState) {
        var builder = MdsStateMsg.newBuilder();
        builder.setAbstractComplexDeviceComponentState(mapAbstractComplexDeviceComponentState(mdsState));
        Util.doIfNotNull(mdsState.getLang(), it ->
                builder.setALang(Util.toStringValue(mdsState.getLang())));
        Util.doIfNotNull(mdsState.getOperatingMode(), mdsOperatingMode ->
                builder.setAOperatingMode(Util.mapToProtoEnum(mdsOperatingMode, MdsOperatingModeMsg.class)));
        Util.doIfNotNull(mdsState.getOperatingJurisdiction(), operatingJurisdiction ->
                builder.setOperatingJurisdiction(baseMapper.mapOperatingJurisdiction(operatingJurisdiction)));
        return builder.build();
    }

    public VmdDescriptorMsg.Builder mapVmdDescriptor(VmdDescriptor vmdDescriptor) {
        var builder = VmdDescriptorMsg.newBuilder();
        Util.doIfNotNull(vmdDescriptor.getApprovedJurisdictions(), it ->
                builder.setApprovedJurisdictions(mapApprovedJurisdictions(it)));
        return builder.setAbstractComplexDeviceComponentDescriptor(
                mapAbstractComplexDeviceComponentDescriptor(vmdDescriptor));
    }

    public VmdStateMsg mapVmdState(VmdState vmdState) {
        var builder = VmdStateMsg.newBuilder();
        Util.doIfNotNull(vmdState.getOperatingJurisdiction(), operatingJurisdiction ->
                builder.setOperatingJurisdiction(baseMapper.mapOperatingJurisdiction(operatingJurisdiction)));
        return builder.setAbstractComplexDeviceComponentState(mapAbstractComplexDeviceComponentState(vmdState)).build();
    }

    private ApprovedJurisdictionsMsg mapApprovedJurisdictions(ApprovedJurisdictions approvedJurisdictions) {
        return ApprovedJurisdictionsMsg.newBuilder()
                .addAllApprovedJurisdiction(baseMapper
                        .mapInstanceIdentifiers(approvedJurisdictions.getApprovedJurisdiction())).build();

    }

    public ChannelDescriptorMsg.Builder mapChannelDescriptor(ChannelDescriptor channelDescriptor) {
        return ChannelDescriptorMsg.newBuilder()
                .setAbstractDeviceComponentDescriptor(
                        mapAbstractDeviceComponentDescriptor(channelDescriptor));
    }

    public ChannelStateMsg mapChannelState(ChannelState channelState) {
        return ChannelStateMsg.newBuilder()
                .setAbstractDeviceComponentState(mapAbstractDeviceComponentState(channelState)).build();
    }

    public AbstractDeviceComponentDescriptorMsg mapAbstractDeviceComponentDescriptor(
            AbstractDeviceComponentDescriptor componentDescriptor) {
        var builder = AbstractDeviceComponentDescriptorMsg.newBuilder();
        builder.setAbstractDescriptor(baseMapper.mapAbstractDescriptor(componentDescriptor));
        componentDescriptor.getProductionSpecification().forEach(productionSpecification ->
                builder.addProductionSpecification(mapProductionSpecification(productionSpecification)));
        return builder.build();
    }

    private AbstractDeviceComponentStateMsg mapAbstractDeviceComponentState(AbstractDeviceComponentState state) {
        var builder = AbstractDeviceComponentStateMsg.newBuilder();
        builder.setAbstractState(baseMapper.mapAbstractState(state));
        Util.doIfNotNull(state.getActivationState(), componentActivation ->
                builder.setAActivationState(Util.mapToProtoEnum(componentActivation, ComponentActivationMsg.class)));
        Util.doIfNotNull(state.getOperatingHours(), it -> builder.setAOperatingHours(Util.toUInt32(it)));
        Util.doIfNotNull(state.getOperatingCycles(), it -> builder.setAOperatingCycles(Util.toInt32(it)));
        Util.doIfNotNull(state.getCalibrationInfo(), it -> builder.setCalibrationInfo(mapCalibrationInfo(it)));
        Util.doIfNotNull(state.getNextCalibration(), it -> builder.setNextCalibration(mapCalibrationInfo(it)));
        Util.doIfNotNull(state.getPhysicalConnector(), it -> builder.setPhysicalConnector(baseMapper.mapPhysicalConnectorInfo(it)));
        return builder.build();
    }

    private AbstractComplexDeviceComponentDescriptorMsg.Builder mapAbstractComplexDeviceComponentDescriptor(
            AbstractComplexDeviceComponentDescriptor complexComponentDescriptor) {
        var builder = AbstractComplexDeviceComponentDescriptorMsg.newBuilder();
        builder.setAbstractDeviceComponentDescriptor(mapAbstractDeviceComponentDescriptor(complexComponentDescriptor));
        return builder;
    }

    private AbstractComplexDeviceComponentStateMsg mapAbstractComplexDeviceComponentState(
            AbstractComplexDeviceComponentState state) {
        return AbstractComplexDeviceComponentStateMsg.newBuilder()
                .setAbstractDeviceComponentState(mapAbstractDeviceComponentState(state)).build();
    }

    public AbstractDeviceComponentDescriptorMsg.ProductionSpecificationMsg mapProductionSpecification(
            AbstractDeviceComponentDescriptor.ProductionSpecification productionSpecification) {
        var builder = AbstractDeviceComponentDescriptorMsg.ProductionSpecificationMsg
                .newBuilder();
        Util.doIfNotNull(productionSpecification.getComponentId(), instanceIdentifier ->
                builder.setComponentId(getOneOfMapper().mapInstanceIdentifier(instanceIdentifier)));
        Util.doIfNotNull(productionSpecification.getProductionSpec(), builder::setProductionSpec);
        Util.doIfNotNull(productionSpecification.getSpecType(), codedValue ->
                builder.setSpecType(baseMapper.mapCodedValue(codedValue)));
        return builder.build();
    }

    public CalibrationInfoMsg mapCalibrationInfo(CalibrationInfo calibrationInfo) {
        var builder = CalibrationInfoMsg.newBuilder();

        Util.doIfNotNull(calibrationInfo.getComponentCalibrationState(), state ->
                builder.setAComponentCalibrationState(Util.mapToProtoEnum(state, CalibrationStateMsg.class)));
        Util.doIfNotNull(calibrationInfo.getType(), type ->
                builder.setAType(Util.mapToProtoEnum(type, CalibrationTypeMsg.class)));
        Util.doIfNotNull(calibrationInfo.getTime(), time ->
                builder.setATime(baseMapper.mapTimestamp(timestampAdapter.marshal(time))));

        calibrationInfo.getCalibrationDocumentation().forEach(doc ->
                builder.addCalibrationDocumentation(mapCalibrationDocumentation(doc)));

        return builder.build();
    }

    public CalibrationInfoMsg.CalibrationDocumentationMsg mapCalibrationDocumentation(
            CalibrationInfo.CalibrationDocumentation documentation
    ) {
        var builder = CalibrationInfoMsg.CalibrationDocumentationMsg.newBuilder();
        builder.addAllDocumentation(baseMapper.mapLocalizedTexts(documentation.getDocumentation()));
        builder.addAllCalibrationResult(documentation.getCalibrationResult().stream().map(this::mapCalibrationResult).collect(Collectors.toList()));
        return builder.build();
    }

    public CalibrationInfoMsg.CalibrationDocumentationMsg.CalibrationResultMsg mapCalibrationResult(
            CalibrationInfo.CalibrationDocumentation.CalibrationResult result
    ) {
        var builder = CalibrationInfoMsg.CalibrationDocumentationMsg.CalibrationResultMsg.newBuilder();
        builder.setCode(baseMapper.mapCodedValue(result.getCode()));
        builder.setValue(baseMapper.mapMeasurement(result.getValue()));
        return builder.build();
    }

    public MdsDescriptorMsg.MetaDataMsg mapMetadata(MdsDescriptor.MetaData metadata) {
        var builder = MdsDescriptorMsg.MetaDataMsg.newBuilder();

        metadata.getUdi().forEach(it -> builder.addUdi(mapUdi(it)));
        Util.doIfNotNull(metadata.getLotNumber(), it -> builder.setLotNumber(Util.toStringValue(it)));
        metadata.getManufacturer().forEach(it -> builder.addManufacturer(baseMapper.mapLocalizedText(it)));
        Util.doIfNotNull(metadata.getManufactureDate(), it ->
                builder.setManufactureDate(Util.toStringValue(anyDateTimeAdapter.marshal(it))));
        Util.doIfNotNull(metadata.getExpirationDate(), it ->
                builder.setExpirationDate(Util.toStringValue(anyDateTimeAdapter.marshal(it))));
        metadata.getModelName().forEach(it -> builder.addModelName(baseMapper.mapLocalizedText(it)));
        Util.doIfNotNull(metadata.getModelNumber(), it -> builder.setModelNumber(Util.toStringValue(it)));
        builder.addAllSerialNumber(metadata.getSerialNumber());

        return builder.build();
    }

    public MdsDescriptorMsg.MetaDataMsg.UdiMsg mapUdi(MdsDescriptor.MetaData.Udi udi) {
        var builder = MdsDescriptorMsg.MetaDataMsg.UdiMsg.newBuilder();

        builder.setDeviceIdentifier(udi.getDeviceIdentifier());
        builder.setHumanReadableForm(udi.getHumanReadableForm());
        builder.setIssuer(getOneOfMapper().mapInstanceIdentifier(udi.getIssuer()));
        Util.doIfNotNull(udi.getJurisdiction(), it ->
                builder.setJurisdiction(getOneOfMapper().mapInstanceIdentifier(it)));

        return builder.build();
    }
}
