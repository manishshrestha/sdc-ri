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

import java.util.stream.Collectors;

public class ProtoToPojoComponentMapper {
    private static final Logger LOG = LogManager.getLogger(ProtoToPojoComponentMapper.class);
    private final Logger instanceLogger;
    private final ProtoToPojoBaseMapper baseMapper;
    private final Provider<ProtoToPojoOneOfMapper> oneOfMapperProvider;
    private final TimestampAdapter timestampAdapter;
    private final AnyDateTimeAdapter anyDateTimeAdapter;
    private ProtoToPojoOneOfMapper oneOfMapper;

    @Inject
    ProtoToPojoComponentMapper(@Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier,
                               ProtoToPojoBaseMapper baseMapper,
                               Provider<ProtoToPojoOneOfMapper> oneOfMapperProvider,
                               TimestampAdapter timestampAdapter,
                               AnyDateTimeAdapter anyDateTimeAdapter) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.baseMapper = baseMapper;
        this.oneOfMapperProvider = oneOfMapperProvider;
        this.oneOfMapper = null;
        this.timestampAdapter = timestampAdapter;
        this.anyDateTimeAdapter = anyDateTimeAdapter;
    }

    public ProtoToPojoOneOfMapper getOneOfMapper() {
        if (this.oneOfMapper == null) {
            this.oneOfMapper = oneOfMapperProvider.get();
        }
        return oneOfMapper;
    }

    public MdsDescriptor map(MdsDescriptorMsg protoMsg) {
        var pojo = new MdsDescriptor();
        if (protoMsg.hasMetaData()) {
            pojo.setMetaData(map(protoMsg.getMetaData()));
        }
        if (protoMsg.hasApprovedJurisdictions()) {
            pojo.setApprovedJurisdictions(map(protoMsg.getApprovedJurisdictions()));
        }
        map(pojo, protoMsg.getAbstractComplexDeviceComponentDescriptor());
        return pojo;
    }

    public MdsState map(MdsStateMsg protoMsg) {
        var pojo = new MdsState();
        pojo.setLang(Util.optionalStr(protoMsg, "ALang"));
        pojo.setOperatingMode(Util.mapToPojoEnum(protoMsg, "AOperatingMode", MdsOperatingMode.class));
        pojo.setOperatingJurisdiction(baseMapper.map(
                Util.optional(protoMsg, "OperatingJurisdiction", OperatingJurisdictionMsg.class)));
        map(pojo, protoMsg.getAbstractComplexDeviceComponentState());
        return pojo;
    }

    public VmdDescriptor map(VmdDescriptorMsg protoMsg) {
        var pojo = new VmdDescriptor();
        if (protoMsg.hasApprovedJurisdictions()) {
            pojo.setApprovedJurisdictions(map(protoMsg.getApprovedJurisdictions()));
        }
        map(pojo, protoMsg.getAbstractComplexDeviceComponentDescriptor());
        return pojo;
    }

    public VmdState map(VmdStateMsg protoMsg) {
        var pojo = new VmdState();
        pojo.setOperatingJurisdiction(baseMapper.map(
                Util.optional(protoMsg, "OperatingJurisdiction", OperatingJurisdictionMsg.class)));
        map(pojo, protoMsg.getAbstractComplexDeviceComponentState());
        return pojo;
    }

    private ApprovedJurisdictions map(ApprovedJurisdictionsMsg protoMsg) {
        var apj = new ApprovedJurisdictions();
        apj.setApprovedJurisdiction(baseMapper.mapInstanceIdentifiers(protoMsg.getApprovedJurisdictionList()));
        return apj;
    }

    private MdsDescriptor.MetaData map(MdsDescriptorMsg.MetaDataMsg protoMsg) {
        var pojo = new MdsDescriptor.MetaData();

        protoMsg.getUdiList().forEach(it -> pojo.getUdi().add(map(it)));
        Util.doIfNotNull(Util.optionalStr(protoMsg, "LotNumber"), pojo::setLotNumber);
        protoMsg.getManufacturerList().forEach(it -> pojo.getManufacturer().add(baseMapper.map(it)));
        Util.doIfNotNull(Util.optionalStr(protoMsg, "ManufactureDate"), it ->
                pojo.setManufactureDate(anyDateTimeAdapter.unmarshal(it)));
        Util.doIfNotNull(Util.optionalStr(protoMsg, "ExpirationDate"), it ->
                pojo.setExpirationDate(anyDateTimeAdapter.unmarshal(it)));
        protoMsg.getModelNameList().forEach(it -> pojo.getModelName().add(baseMapper.map(it)));
        Util.doIfNotNull(Util.optionalStr(protoMsg, "ModelNumber"), pojo::setModelNumber);
        protoMsg.getSerialNumberList().forEach(pojo.getSerialNumber()::add);

        return pojo;
    }

    private MdsDescriptor.MetaData.Udi map(MdsDescriptorMsg.MetaDataMsg.UdiMsg protoMsg) {
        var pojo = new MdsDescriptor.MetaData.Udi();

        pojo.setDeviceIdentifier(protoMsg.getDeviceIdentifier());
        pojo.setHumanReadableForm(protoMsg.getHumanReadableForm());
        pojo.setIssuer(getOneOfMapper().map(protoMsg.getIssuer()));
        Util.doIfNotNull(
                Util.optional(protoMsg, "Jurisdiction", InstanceIdentifierOneOfMsg.class),
                it -> pojo.setJurisdiction(getOneOfMapper().map(it))
        );

        return pojo;
    }

    public ChannelDescriptor map(ChannelDescriptorMsg protoMsg) {
        var pojo = new ChannelDescriptor();
        map(pojo, protoMsg.getAbstractDeviceComponentDescriptor());
        return pojo;
    }

    public ChannelState map(ChannelStateMsg protoMsg) {
        var pojo = new ChannelState();
        map(pojo, protoMsg.getAbstractDeviceComponentState());
        return pojo;
    }

    ScoDescriptor map(ScoDescriptorMsg protoMsg) {
        var pojo = new ScoDescriptor();
        map(pojo, protoMsg.getAbstractDeviceComponentDescriptor());
        return pojo;
    }

    ScoState map(ScoStateMsg protoMsg) {
        var pojo = new ScoState();
        pojo.setInvocationRequested(protoMsg.getAInvocationRequested().getOperationRefList());
        pojo.setInvocationRequired(protoMsg.getAInvocationRequired().getOperationRefList());
        map(pojo, protoMsg.getAbstractDeviceComponentState());
        return pojo;
    }

    SystemContextDescriptor map(SystemContextDescriptorMsg protoMsg) {
        var pojo = new SystemContextDescriptor();
        map(pojo, protoMsg.getAbstractDeviceComponentDescriptor());
        return pojo;
    }

    SystemContextState map(SystemContextStateMsg protoMsg) {
        var pojo = new SystemContextState();
        map(pojo, protoMsg.getAbstractDeviceComponentState());
        return pojo;
    }

    private void map(AbstractComplexDeviceComponentDescriptor pojo,
                     AbstractComplexDeviceComponentDescriptorMsg protoMsg) {
        map(pojo, protoMsg.getAbstractDeviceComponentDescriptor());
    }

    private void map(AbstractComplexDeviceComponentState pojo, AbstractComplexDeviceComponentStateMsg protoMsg) {
        map(pojo, protoMsg.getAbstractDeviceComponentState());
    }

    private void map(AbstractDeviceComponentDescriptor pojo, AbstractDeviceComponentDescriptorMsg protoMsg) {
        pojo.setProductionSpecification(protoMsg.getProductionSpecificationList()
                .stream().map(this::map).collect(Collectors.toList()));
        baseMapper.map(pojo, protoMsg.getAbstractDescriptor());
    }

    private void map(AbstractDeviceComponentState pojo, AbstractDeviceComponentStateMsg protoMsg) {
        pojo.setActivationState(Util.mapToPojoEnum(protoMsg, "AActivationState", ComponentActivation.class));
        pojo.setOperatingCycles(Util.optionalIntOfInt(protoMsg, "AOperatingCycles"));
        pojo.setOperatingHours(Util.optionalLongOfInt(protoMsg, "AOperatingHours"));

        Util.doIfNotNull(
                Util.optional(protoMsg, "CalibrationInfo", CalibrationInfoMsg.class),
                calibrationInfoMsg -> pojo.setCalibrationInfo(map(calibrationInfoMsg))
        );
        Util.doIfNotNull(
                Util.optional(protoMsg, "NextCalibration", CalibrationInfoMsg.class),
                calibrationInfoMsg -> pojo.setNextCalibration(map(calibrationInfoMsg))
        );

        Util.doIfNotNull(
                Util.optional(protoMsg, "PhysicalConnector", PhysicalConnectorInfoMsg.class),
                connector -> pojo.setPhysicalConnector(baseMapper.map(connector))
        );

        baseMapper.map(pojo, protoMsg.getAbstractState());
    }

    public AbstractDeviceComponentDescriptor.ProductionSpecification map(
            AbstractDeviceComponentDescriptorMsg.ProductionSpecificationMsg protoMsg) {
        var pojo = new AbstractDeviceComponentDescriptor.ProductionSpecification();
        pojo.setProductionSpec(protoMsg.getProductionSpec());
        pojo.setSpecType(baseMapper.map(Util.optional(protoMsg, "SpecType", CodedValueMsg.class)));
        Util.doIfNotNull(Util.optional(protoMsg, "ComponentId", InstanceIdentifierOneOfMsg.class), component ->
                pojo.setComponentId(getOneOfMapper().map(component)));
        return pojo;
    }

    public CalibrationInfo map(CalibrationInfoMsg protoMsg) {
        var pojo = new CalibrationInfo();

        pojo.setComponentCalibrationState(Util.mapToPojoEnum(protoMsg, "AComponentCalibrationState", CalibrationState.class));
        pojo.setType(Util.mapToPojoEnum(protoMsg, "AType", CalibrationType.class));
        Util.doIfNotNull(
                Util.optionalBigIntOfLong(protoMsg, "ATime"),
                it -> pojo.setTime(timestampAdapter.unmarshal(it))
        );
        protoMsg.getCalibrationDocumentationList().forEach(doc -> pojo.getCalibrationDocumentation().add(map(doc)));

        return pojo;
    }

    public CalibrationInfo.CalibrationDocumentation map(CalibrationInfoMsg.CalibrationDocumentationMsg protoMsg) {
        var pojo = new CalibrationInfo.CalibrationDocumentation();
        pojo.setDocumentation(baseMapper.mapLocalizedTexts(protoMsg.getDocumentationList()));
        protoMsg.getCalibrationResultList().forEach(result -> pojo.getCalibrationResult().add(map(result)));
        return pojo;
    }

    public CalibrationInfo.CalibrationDocumentation.CalibrationResult map(
            CalibrationInfoMsg.CalibrationDocumentationMsg.CalibrationResultMsg protoMsg
    ) {
        var pojo = new CalibrationInfo.CalibrationDocumentation.CalibrationResult();
        pojo.setCode(baseMapper.map(protoMsg.getCode()));
        pojo.setValue(baseMapper.map(protoMsg.getValue()));
        return pojo;
    }
}
