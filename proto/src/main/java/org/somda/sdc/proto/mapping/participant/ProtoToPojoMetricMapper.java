package org.somda.sdc.proto.mapping.participant;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.google.protobuf.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.common.util.TimestampAdapter;
import org.somda.sdc.proto.mapping.Util;
import org.somda.sdc.proto.model.biceps.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class ProtoToPojoMetricMapper {
    private static final Logger LOG = LogManager.getLogger(ProtoToPojoMetricMapper.class);
    private final Logger instanceLogger;
    private final TimestampAdapter timestampAdapter;
    private final ProtoToPojoBaseMapper baseMapper;
    private final Provider<ProtoToPojoOneOfMapper> oneOfMapperProvider;
    private ProtoToPojoOneOfMapper oneOfMapper;

    @Inject
    ProtoToPojoMetricMapper(@Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier,
                            TimestampAdapter timestampAdapter,
                            ProtoToPojoBaseMapper baseMapper,
                            Provider<ProtoToPojoOneOfMapper> oneOfMapperProvider) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.timestampAdapter = timestampAdapter;
        this.baseMapper = baseMapper;
        this.oneOfMapperProvider = oneOfMapperProvider;
        this.oneOfMapper = null;
    }

    public ProtoToPojoOneOfMapper getOneOfMapper() {
        if (this.oneOfMapper == null) {
            this.oneOfMapper = oneOfMapperProvider.get();
        }
        return oneOfMapper;
    }

    public RealTimeSampleArrayMetricDescriptor map(RealTimeSampleArrayMetricDescriptorMsg protoMsg) {
        var pojo = new RealTimeSampleArrayMetricDescriptor();
        map(pojo, protoMsg.getAbstractMetricDescriptor());
        pojo.setTechnicalRange(protoMsg.getTechnicalRangeList().stream().map(baseMapper::map).collect(Collectors.toList()));
        pojo.setResolution(new BigDecimal(protoMsg.getAResolution()));
        Util.doIfNotNull(Util.optional(protoMsg, "ASamplePeriod", Duration.class), period ->
                pojo.setSamplePeriod(Util.fromProtoDuration(period)));
        return pojo;
    }

    public RealTimeSampleArrayMetricState map(RealTimeSampleArrayMetricStateMsg protoMsg) {
        var pojo = new RealTimeSampleArrayMetricState();
        map(pojo, protoMsg.getAbstractMetricState());
        Util.doIfNotNull(Util.optional(protoMsg, "MetricValue", SampleArrayValueMsg.class),
                value -> pojo.setMetricValue(map(value)));
        pojo.setPhysiologicalRange(protoMsg.getPhysiologicalRangeList().stream().map(baseMapper::map).collect(Collectors.toList()));
        return pojo;
    }

    public NumericMetricDescriptor map(NumericMetricDescriptorMsg protoMsg) {
        var pojo = new NumericMetricDescriptor();
        map(pojo, protoMsg.getAbstractMetricDescriptor());
        pojo.setTechnicalRange(protoMsg.getTechnicalRangeList().stream().map(baseMapper::map).collect(Collectors.toList()));
        pojo.setResolution(new BigDecimal(protoMsg.getAResolution()));
        Util.doIfNotNull(Util.optional(protoMsg, "AAveragingPeriod", Duration.class), period ->
                pojo.setAveragingPeriod(Util.fromProtoDuration(period)));
        return pojo;
    }

    public NumericMetricState map(NumericMetricStateMsg protoMsg) {
        var pojo = new NumericMetricState();
        map(pojo, protoMsg.getAbstractMetricState());
        pojo.setPhysiologicalRange(protoMsg.getPhysiologicalRangeList().stream().map(baseMapper::map).collect(Collectors.toList()));
        Util.doIfNotNull(Util.optional(protoMsg, "AActiveAveragingPeriod", Duration.class), period ->
                pojo.setActiveAveragingPeriod(Util.fromProtoDuration(period))
        );
        Util.doIfNotNull(Util.optional(protoMsg, "MetricValue", NumericMetricValueMsg.class),
                value -> pojo.setMetricValue(map(value)));

        return pojo;
    }

    public EnumStringMetricDescriptor map(EnumStringMetricDescriptorMsg protoMsg) {
        var pojo = new EnumStringMetricDescriptor();
        map(pojo, protoMsg.getStringMetricDescriptor());
        pojo.setAllowedValue(protoMsg.getAllowedValueList().stream().map(this::map).collect(Collectors.toList()));
        return pojo;
    }

    public EnumStringMetricState map(EnumStringMetricStateMsg protoMsg) {
        var pojo = new EnumStringMetricState();
        map(pojo, protoMsg.getStringMetricState());
        return pojo;
    }

    public EnumStringMetricDescriptor.AllowedValue map(EnumStringMetricDescriptorMsg.AllowedValueMsg protoMsg) {
        var pojo = new EnumStringMetricDescriptor.AllowedValue();
        pojo.setValue(protoMsg.getValue());
        Util.doIfNotNull(
                Util.optional(protoMsg, "Characteristic", MeasurementMsg.class),
                measurement -> pojo.setCharacteristic(baseMapper.map(measurement))
        );
        Util.doIfNotNull(
                Util.optional(protoMsg, "Identification", InstanceIdentifierOneOfMsg.class),
                identifier -> pojo.setIdentification(getOneOfMapper().map(identifier))
        );
        Util.doIfNotNull(
                Util.optional(protoMsg, "Type", CodedValueMsg.class),
                codedValue ->  pojo.setType(baseMapper.map(codedValue))
        );
        return pojo;
    }

    public StringMetricDescriptor map(StringMetricDescriptorMsg protoMsg) {
        var pojo = new StringMetricDescriptor();
        map(pojo, protoMsg);
        return pojo;
    }

    public StringMetricState map(StringMetricStateMsg protoMsg) {
        var pojoState = new StringMetricState();
        map(pojoState, protoMsg);
        return pojoState;
    }

    public SampleArrayValue map(SampleArrayValueMsg protoMsg) {
        var pojo = new SampleArrayValue();
        pojo.setSamples(
                protoMsg.getASamples().getRealTimeValueTypeList().stream()
                        .map(BigDecimal::new)
                        .collect(Collectors.toList())
        );
//        pojo.setApplyAnnotation();
        return pojo;
    }

    public StringMetricValue map(StringMetricValueMsg protoMsg) {
        var pojoValue = new StringMetricValue();
        map(pojoValue, protoMsg.getAbstractMetricValue());
        pojoValue.setValue(Util.optionalStr(protoMsg, "AValue"));
        return pojoValue;
    }

    public NumericMetricValue map(NumericMetricValueMsg protoMsg) {
        var pojoValue = new NumericMetricValue();
        map(pojoValue, protoMsg.getAbstractMetricValue());
        pojoValue.setValue(Util.optionalBigDecimalOfString(protoMsg, "AValue"));
        return pojoValue;
    }

    private void map(AbstractMetricValue pojo, AbstractMetricValueMsg protoMsg) {
        pojo.setStartTime(timestampAdapter.unmarshal(
                Util.optionalBigIntOfLong(protoMsg, "AStartTime")));
        pojo.setStopTime(timestampAdapter.unmarshal(
                Util.optionalBigIntOfLong(protoMsg, "AStopTime")));
        pojo.setDeterminationTime(timestampAdapter.unmarshal(
                Util.optionalBigIntOfLong(protoMsg, "ADeterminationTime")));
        pojo.setMetricQuality(map(protoMsg.getMetricQuality()));
        protoMsg.getAnnotationList().forEach(it -> pojo.getAnnotation().add(map(it)));
    }

    private AbstractMetricValue.MetricQuality map(AbstractMetricValueMsg.MetricQualityMsg protoMsg) {
        var pojo = new AbstractMetricValue.MetricQuality();
        Util.doIfNotNull(protoMsg.getAMode(), mode ->
                pojo.setMode(Util.mapToPojoEnum(protoMsg, "AMode", GenerationMode.class))
        );
        Util.doIfNotNull(protoMsg.getAValidity(), validity ->
                pojo.setValidity(Util.mapToPojoEnum(protoMsg, "AValidity", MeasurementValidity.class))
        );
        pojo.setQi(Util.optionalBigDecimalOfString(protoMsg, "AQi"));
        return pojo;
    }

    private AbstractMetricValue.Annotation map(AbstractMetricValueMsg.AnnotationMsg protoMsg) {
        var pojo = new AbstractMetricValue.Annotation();
        pojo.setType(baseMapper.map(protoMsg.getType()));
        return pojo;
    }

    private void map(StringMetricDescriptor pojo, StringMetricDescriptorMsg protoMsg) {
        map(pojo, protoMsg.getAbstractMetricDescriptor());
    }

    private void map(StringMetricState state, StringMetricStateMsg protoMsg) {
        Util.doIfNotNull(
                Util.optional(protoMsg, "MetricValue", StringMetricValueMsg.class),
                value -> state.setMetricValue(map(value))
        );
        map(state, protoMsg.getAbstractMetricState());
    }

    private void map(AbstractMetricDescriptor pojo, AbstractMetricDescriptorMsg protoMsg) {
        baseMapper.map(pojo, protoMsg.getAbstractDescriptor());
        Util.doIfNotNull(
                protoMsg.getAMetricCategory(),
                category -> pojo.setMetricCategory(Util.mapToPojoEnum(protoMsg, "AMetricCategory", MetricCategory.class))
        );
        Util.doIfNotNull(Util.optional(protoMsg, "ADerivationMethod", DerivationMethodMsg.class),
                method -> pojo.setDerivationMethod(Util.mapToPojoEnum(protoMsg, "ADerivationMethod", DerivationMethod.class)));
        Util.doIfNotNull(
                protoMsg.getAMetricAvailability(),
                availability -> pojo.setMetricAvailability(Util.mapToPojoEnum(protoMsg, "AMetricAvailability", MetricAvailability.class))
        );
        Util.doIfNotNull(Util.optional(protoMsg, "AMaxMeasurementTime", Duration.class), duration ->
                pojo.setMaxMeasurementTime(Util.fromProtoDuration(duration)));
        Util.doIfNotNull(Util.optional(protoMsg, "AMaxDelayTime", Duration.class), duration ->
                pojo.setMaxDelayTime(Util.fromProtoDuration(duration)));
        Util.doIfNotNull(Util.optional(protoMsg, "ADeterminationPeriod", Duration.class), duration ->
                pojo.setDeterminationPeriod(Util.fromProtoDuration(duration)));
        Util.doIfNotNull(Util.optional(protoMsg, "ALifeTimePeriod", Duration.class), duration ->
                pojo.setLifeTimePeriod(Util.fromProtoDuration(duration)));
        Util.doIfNotNull(Util.optional(protoMsg, "AActivationDuration", Duration.class), duration ->
                pojo.setActivationDuration(Util.fromProtoDuration(duration)));

        pojo.setUnit(baseMapper.map(protoMsg.getUnit()));
        pojo.setBodySite(protoMsg.getBodySiteList().stream().map(baseMapper::map).collect(Collectors.toList()));
        pojo.setRelation(protoMsg.getRelationList().stream().map(this::map).collect(Collectors.toList()));

    }

    private void map(AbstractMetricState state, AbstractMetricStateMsg protoMsg) {
        Util.doIfNotNull(
                protoMsg.getAActivationState(), aState ->
                        state.setActivationState(Util.mapToPojoEnum(protoMsg, "AActivationState", ComponentActivation.class))
        );
        Util.doIfNotNull(
                Util.optional(protoMsg, "AActiveDeterminationPeriod", Duration.class),
                period -> state.setActiveDeterminationPeriod(Util.fromProtoDuration(period))
        );
        Util.doIfNotNull(
                Util.optional(protoMsg, "ALifeTimePeriod", Duration.class),
                period -> state.setLifeTimePeriod(Util.fromProtoDuration(period))
        );

        protoMsg.getBodySiteList().forEach(codedValueMsg -> state.getBodySite().add(baseMapper.map(codedValueMsg)));
        Util.doIfNotNull(
                Util.optional(protoMsg,"PhysicalConnector", PhysicalConnectorInfoMsg.class),
                connector -> state.setPhysicalConnector(baseMapper.map(connector))
        );

        baseMapper.map(state, protoMsg.getAbstractState());
    }

    private AbstractMetricDescriptor.Relation map(AbstractMetricDescriptorMsg.RelationMsg protoMsg) {
        var pojo = new AbstractMetricDescriptor.Relation();
        pojo.setKind(Util.mapToPojoEnum(protoMsg, "AKind", AbstractMetricDescriptor.Relation.Kind.class));
        pojo.setEntries(new ArrayList<>(protoMsg.getAEntries().getEntryRefList()));

        Util.doIfNotNull(Util.optional(protoMsg, "Code", CodedValueMsg.class), codedValueMsg ->
                pojo.setCode(baseMapper.map(codedValueMsg)));
        Util.doIfNotNull(Util.optional(protoMsg, "Identification", InstanceIdentifierOneOfMsg.class),
                instanceIdentifierOneOfMsg -> pojo.setIdentification(getOneOfMapper().map(instanceIdentifierOneOfMsg)));

        return pojo;
    }

}
