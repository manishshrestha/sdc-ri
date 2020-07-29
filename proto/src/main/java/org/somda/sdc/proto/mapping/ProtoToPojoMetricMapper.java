package org.somda.sdc.proto.mapping;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.common.util.TimestampAdapter;
import org.somda.sdc.proto.model.biceps.*;

import java.math.BigDecimal;
import java.util.stream.Collectors;

public class ProtoToPojoMetricMapper {
    private static final Logger LOG = LogManager.getLogger(ProtoToPojoMetricMapper.class);
    private final Logger instanceLogger;
    private final TimestampAdapter timestampAdapter;
    private final ProtoToPojoBaseMapper baseMapper;

    @Inject
    ProtoToPojoMetricMapper(@Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier,
                            TimestampAdapter timestampAdapter,
                            ProtoToPojoBaseMapper baseMapper) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.timestampAdapter = timestampAdapter;
        this.baseMapper = baseMapper;
    }

    public RealTimeSampleArrayMetricDescriptor map(RealTimeSampleArrayMetricDescriptorMsg protoMsg) {
        var pojo = new RealTimeSampleArrayMetricDescriptor();
        map(pojo, protoMsg.getAbstractMetricDescriptor());
        pojo.setTechnicalRange(protoMsg.getTechnicalRangeList().stream().map(this::map).collect(Collectors.toList()));
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
        pojo.setPhysiologicalRange(protoMsg.getPhysiologicalRangeList().stream().map(this::map).collect(Collectors.toList()));
        return pojo;
    }

    public NumericMetricDescriptor map(NumericMetricDescriptorMsg protoMsg) {
        var pojo = new NumericMetricDescriptor();
        map(pojo, protoMsg.getAbstractMetricDescriptor());
        pojo.setTechnicalRange(protoMsg.getTechnicalRangeList().stream().map(this::map).collect(Collectors.toList()));
        pojo.setResolution(new BigDecimal(protoMsg.getAResolution()));
        Util.doIfNotNull(Util.optional(protoMsg, "AAveragingPeriod", Duration.class), period ->
                pojo.setAveragingPeriod(Util.fromProtoDuration(period)));
        return pojo;
    }

    public NumericMetricState map(NumericMetricStateMsg protoMsg) {
        var pojo = new NumericMetricState();
        map(pojo, protoMsg.getAbstractMetricState());
        pojo.setPhysiologicalRange(protoMsg.getPhysiologicalRangeList().stream().map(this::map).collect(Collectors.toList()));
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
//        pojo.setCharacteristic();
//        pojo.setIdentification();
//        pojo.setType();
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

    public Range map(RangeMsg protoMsg) {
        var pojoValue = new Range();
        pojoValue.setStepWidth(Util.optionalBigDecimalOfString(protoMsg, "AStepWidth"));
        pojoValue.setRelativeAccuracy(Util.optionalBigDecimalOfString(protoMsg, "ARelativeAccuracy"));
        pojoValue.setLower(Util.optionalBigDecimalOfString(protoMsg, "ALower"));
        pojoValue.setUpper(Util.optionalBigDecimalOfString(protoMsg, "AUpper"));
        pojoValue.setAbsoluteAccuracy(Util.optionalBigDecimalOfString(protoMsg, "AAbsoluteAccuracy"));
        return pojoValue;
    }

    private void map(AbstractMetricValue pojo, AbstractMetricValueMsg protoMsg) {
        Util.doIfNotNull(Util.optional(protoMsg, "MetricQuality", AbstractMetricValueMsg.MetricQualityMsg.class),
                quality -> pojo.setMetricQuality(map(quality)));
        pojo.setDeterminationTime(timestampAdapter.unmarshal(
                Util.optionalBigIntOfLong(protoMsg, "ADeterminationTime")));
        pojo.setStartTime(timestampAdapter.unmarshal(
                Util.optionalBigIntOfLong(protoMsg, "AStartTime")));
        pojo.setStopTime(timestampAdapter.unmarshal(
                Util.optionalBigIntOfLong(protoMsg, "AStopTime")));
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
        Util.doIfNotNull(
                protoMsg.getAMetricAvailability(),
                availability -> pojo.setMetricAvailability(Util.mapToPojoEnum(protoMsg, "AMetricAvailability", MetricAvailability.class))
        );
        Util.doIfNotNull(Util.optional(protoMsg, "ADerivationMethod", DerivationMethodMsg.class),
                method -> pojo.setDerivationMethod(Util.mapToPojoEnum(protoMsg, "ADerivationMethod", DerivationMethod.class)));
        Util.doIfNotNull(Util.optional(protoMsg, "AMaxMeasurementTime", Duration.class), duration ->
                pojo.setMaxMeasurementTime(Util.fromProtoDuration(duration)));
        Util.doIfNotNull(Util.optional(protoMsg, "AMaxDelayTime", Duration.class), duration ->
                pojo.setMaxDelayTime(Util.fromProtoDuration(duration)));
        Util.doIfNotNull(Util.optional(protoMsg, "ALifeTimePeriod", Duration.class), duration ->
                pojo.setLifeTimePeriod(Util.fromProtoDuration(duration)));
        Util.doIfNotNull(Util.optional(protoMsg, "AActivationDuration", Duration.class), duration ->
                pojo.setActivationDuration(Util.fromProtoDuration(duration)));

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
        baseMapper.map(state, protoMsg.getAbstractState());
    }
}
