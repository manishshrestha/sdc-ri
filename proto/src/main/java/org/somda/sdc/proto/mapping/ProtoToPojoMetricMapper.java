package org.somda.sdc.proto.mapping;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.proto.model.biceps.AbstractMetricDescriptorMsg;
import org.somda.sdc.proto.model.biceps.AbstractMetricStateMsg;
import org.somda.sdc.proto.model.biceps.AbstractMetricValueMsg;
import org.somda.sdc.proto.model.biceps.EnumStringMetricDescriptorMsg;
import org.somda.sdc.proto.model.biceps.EnumStringMetricStateMsg;
import org.somda.sdc.proto.model.biceps.NumericMetricDescriptorMsg;
import org.somda.sdc.proto.model.biceps.NumericMetricStateMsg;
import org.somda.sdc.proto.model.biceps.NumericMetricValueMsg;
import org.somda.sdc.proto.model.biceps.RangeMsg;
import org.somda.sdc.proto.model.biceps.StringMetricDescriptorMsg;
import org.somda.sdc.proto.model.biceps.StringMetricStateMsg;
import org.somda.sdc.proto.model.biceps.StringMetricValueMsg;

import java.math.BigDecimal;
import java.util.stream.Collectors;

public class ProtoToPojoMetricMapper {
    private static final Logger LOG = LogManager.getLogger(ProtoToPojoMetricMapper.class);
    private final Logger instanceLogger;
    private final ProtoToPojoBaseMapper baseMapper;

    @Inject
    ProtoToPojoMetricMapper(@Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier,
                            ProtoToPojoBaseMapper baseMapper) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.baseMapper = baseMapper;
    }

    public NumericMetricDescriptor map(NumericMetricDescriptorMsg protoMsg) {
        var pojo = new NumericMetricDescriptor();
        map(pojo, protoMsg.getAbstractMetricDescriptor());
        pojo.setTechnicalRange(protoMsg.getTechnicalRangeList().stream().map(this::map).collect(Collectors.toList()));
        pojo.setResolution(new BigDecimal(protoMsg.getAResolution()));
        Util.doIfNotNull(protoMsg.getAAveragingPeriod(), period ->
                pojo.setAveragingPeriod(Util.fromProtoDuration(period)));
        return pojo;
    }

    public NumericMetricState map(NumericMetricStateMsg protoMsg) {
        var pojo = new NumericMetricState();
        map(pojo, protoMsg.getAbstractMetricState());
        pojo.setPhysiologicalRange(protoMsg.getPhysiologicalRangeList().stream().map(this::map).collect(Collectors.toList()));
        Util.doIfNotNull(protoMsg.getAActiveAveragingPeriod(), period ->
                pojo.setActiveAveragingPeriod(Util.fromProtoDuration(period))
        );
        Util.doIfNotNull(protoMsg.getMetricValue(), value -> pojo.setMetricValue(map(value)));

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
        Util.doIfNotNull(protoMsg.getMetricQuality(), quality -> pojo.setMetricQuality(map(quality)));
        pojo.setDeterminationTime(Util.optionalInstantOfLong(protoMsg, "ADeterminationTime"));
        pojo.setStartTime(Util.optionalInstantOfLong(protoMsg, "AStartTime"));
        pojo.setStopTime(Util.optionalInstantOfLong(protoMsg, "AStopTime"));
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
        state.setMetricValue(map(protoMsg.getMetricValue()));
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
