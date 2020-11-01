package org.somda.sdc.proto.mapping.participant;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.common.util.TimestampAdapter;
import org.somda.sdc.proto.mapping.Util;
import org.somda.sdc.proto.model.biceps.*;

import java.util.List;
import java.util.stream.Collectors;

public class PojoToProtoMetricMapper {
    private static final Logger LOG = LogManager.getLogger(PojoToProtoMetricMapper.class);
    private final Logger instanceLogger;
    private final TimestampAdapter timestampAdapter;
    private final PojoToProtoBaseMapper baseMapper;

    @Inject
    PojoToProtoMetricMapper(@Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier,
                            TimestampAdapter timestampAdapter,
                            PojoToProtoBaseMapper baseMapper) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.timestampAdapter = timestampAdapter;
        this.baseMapper = baseMapper;
    }

    public RealTimeSampleArrayMetricDescriptorMsg.Builder mapRealTimeSampleArrayMetricDescriptor(RealTimeSampleArrayMetricDescriptor rtsDescriptor) {
        var builder = RealTimeSampleArrayMetricDescriptorMsg.newBuilder()
                .setAbstractMetricDescriptor(mapAbstractMetricDescriptor(rtsDescriptor));

        builder.setAResolution(rtsDescriptor.getResolution().toPlainString());
        builder.setASamplePeriod(Util.fromJavaDuration(rtsDescriptor.getSamplePeriod()));
        builder.addAllTechnicalRange(rtsDescriptor.getTechnicalRange().stream().map(baseMapper::mapRange).collect(Collectors.toList()));

        return builder;
    }

    public NumericMetricDescriptorMsg.Builder mapNumericMetricDescriptor(NumericMetricDescriptor numericMetricDescriptor) {
        var builder = NumericMetricDescriptorMsg.newBuilder()
                .setAbstractMetricDescriptor(mapAbstractMetricDescriptor(numericMetricDescriptor));

        Util.doIfNotNull(numericMetricDescriptor.getAveragingPeriod(), period ->
                builder.setAAveragingPeriod(Util.fromJavaDuration(period))
                );
        builder.setAResolution(numericMetricDescriptor.getResolution().toPlainString());
        builder.addAllTechnicalRange(numericMetricDescriptor.getTechnicalRange().stream().map(baseMapper::mapRange).collect(Collectors.toList()));

        return builder;
    }

    public EnumStringMetricDescriptorMsg.Builder mapEnumStringMetricDescriptor(EnumStringMetricDescriptor enumStringMetricDescriptor) {
        var builder = EnumStringMetricDescriptorMsg.newBuilder()
                .setStringMetricDescriptor(mapStringMetricDescriptor(enumStringMetricDescriptor));

        enumStringMetricDescriptor.getAllowedValue().forEach(allowedValue -> {
            var valueBuilder = EnumStringMetricDescriptorMsg.AllowedValueMsg.newBuilder();
            valueBuilder.setValue(allowedValue.getValue());

            Util.doIfNotNull(allowedValue.getCharacteristic(), measurement -> {
                valueBuilder.setCharacteristic(baseMapper.mapMeasurement(measurement));
            });
            Util.doIfNotNull(allowedValue.getIdentification(), instanceIdentifier -> {
                valueBuilder.setIdentification(baseMapper.mapInstanceIdentifierOneOf(instanceIdentifier));
            });
            Util.doIfNotNull(allowedValue.getType(), codedValue -> {
                valueBuilder.setType(baseMapper.mapCodedValue(codedValue));
            });
            builder.addAllowedValue(valueBuilder);
        });

        return builder;
    }

    public StringMetricDescriptorMsg.Builder mapStringMetricDescriptor(StringMetricDescriptor stringMetricDescriptor) {
        return StringMetricDescriptorMsg.newBuilder()
                .setAbstractMetricDescriptor(mapAbstractMetricDescriptor(stringMetricDescriptor));
    }

    private AbstractMetricDescriptorMsg.Builder mapAbstractMetricDescriptor(
            AbstractMetricDescriptor abstractMetricDescriptor) {
        var builder = AbstractMetricDescriptorMsg.newBuilder()
                .setAbstractDescriptor(baseMapper.mapAbstractDescriptor(abstractMetricDescriptor));
        Util.doIfNotNull(abstractMetricDescriptor.getMetricCategory(), category ->
                builder.setAMetricCategory(Util.mapToProtoEnum(category, MetricCategoryMsg.class))
        );
        Util.doIfNotNull(abstractMetricDescriptor.getDerivationMethod(), derivation ->
                builder.setADerivationMethod(Util.mapToProtoEnum(derivation, DerivationMethodMsg.class)));
        Util.doIfNotNull(abstractMetricDescriptor.getMetricAvailability(), availability ->
                builder.setAMetricAvailability(Util.mapToProtoEnum(availability, MetricAvailabilityMsg.class))
        );
        Util.doIfNotNull(abstractMetricDescriptor.getMaxMeasurementTime(), time ->
                builder.setAMaxMeasurementTime(Util.fromJavaDuration(time)));
        Util.doIfNotNull(abstractMetricDescriptor.getMaxDelayTime(), time ->
                builder.setAMaxDelayTime(Util.fromJavaDuration(time)));
        Util.doIfNotNull(abstractMetricDescriptor.getDeterminationPeriod(), period ->
                builder.setADeterminationPeriod(Util.fromJavaDuration(period)));
        Util.doIfNotNull(abstractMetricDescriptor.getLifeTimePeriod(), period ->
                builder.setALifeTimePeriod(Util.fromJavaDuration(period)));
        Util.doIfNotNull(abstractMetricDescriptor.getActivationDuration(), duration ->
                builder.setAActivationDuration(Util.fromJavaDuration(duration)));

        builder.setUnit(baseMapper.mapCodedValue(abstractMetricDescriptor.getUnit()));
        abstractMetricDescriptor.getBodySite().forEach(codedValue -> builder.addBodySite(baseMapper.mapCodedValue(codedValue)));
        abstractMetricDescriptor.getRelation().forEach(relation -> builder.addRelation(mapRelation(relation)));


        return builder;
    }


    public RealTimeSampleArrayMetricStateMsg mapRealTimeSampleArrayMetricState(RealTimeSampleArrayMetricState state) {
        var builder = RealTimeSampleArrayMetricStateMsg.newBuilder()
                .setAbstractMetricState(mapAbstractMetricState(state));

        Util.doIfNotNull(
                state.getMetricValue(),
                value -> builder.setMetricValue(mapSampleArrayMetricValue(value))
        );
        builder.addAllPhysiologicalRange(
                state.getPhysiologicalRange().stream().map(baseMapper::mapRange).collect(Collectors.toList())
        );
        return builder.build();
    }

    public NumericMetricStateMsg mapNumericMetricState(NumericMetricState state) {
        var builder = NumericMetricStateMsg.newBuilder()
                .setAbstractMetricState(mapAbstractMetricState(state));

        Util.doIfNotNull(
                state.getActiveAveragingPeriod(),
                period -> builder.setAActiveAveragingPeriod(Util.fromJavaDuration(period))
        );
        Util.doIfNotNull(
                state.getMetricValue(),
                value -> builder.setMetricValue(mapNumericMetricValue(value))
        );
        builder.addAllPhysiologicalRange(
                state.getPhysiologicalRange().stream().map(baseMapper::mapRange).collect(Collectors.toList())
        );
        return builder.build();
    }

    public StringMetricStateMsg mapStringMetricState(StringMetricState state) {
        var builder = StringMetricStateMsg.newBuilder();
        Util.doIfNotNull(state.getMetricValue(), value -> builder.setMetricValue(mapStringMetricValue(value)));
        builder.setAbstractMetricState(mapAbstractMetricState(state));

        return builder.build();
    }

    public EnumStringMetricStateMsg mapEnumStringMetricState(EnumStringMetricState state) {
        var builder = EnumStringMetricStateMsg.newBuilder();
        builder.setStringMetricState(mapStringMetricState(state));
        return builder.build();
    }


    private AbstractMetricStateMsg mapAbstractMetricState(AbstractMetricState state) {
        var builder = AbstractMetricStateMsg.newBuilder();
        builder.setAbstractState(baseMapper.mapAbstractState(state));
        Util.doIfNotNull(
                state.getActivationState(),
                activation -> builder.setAActivationState(Util.mapToProtoEnum(activation, ComponentActivationMsg.class))
        );
        Util.doIfNotNull(
                state.getActiveDeterminationPeriod(),
                period -> builder.setAActiveDeterminationPeriod(Util.fromJavaDuration(period))
        );
        Util.doIfNotNull(
                state.getLifeTimePeriod(),
                period -> builder.setALifeTimePeriod(Util.fromJavaDuration(period))
        );

        state.getBodySite().forEach(site -> builder.addBodySite(baseMapper.mapCodedValue(site)));
        Util.doIfNotNull(state.getPhysicalConnector(), physicalConnectorInfo ->
                builder.setPhysicalConnector(baseMapper.mapPhysicalConnectorInfo(physicalConnectorInfo)));

        return builder.build();
    }

    private SampleArrayValueMsg mapSampleArrayMetricValue(SampleArrayValue value) {
        var builder = SampleArrayValueMsg.newBuilder()
                .setAbstractMetricValue(mapAbstractMetricValue(value));

        var valueBuilder = RealTimeValueTypeMsg.newBuilder();
        value.getSamples().forEach(sample -> valueBuilder.addRealTimeValueType(sample.toPlainString()));
        builder.setASamples(valueBuilder);

        return builder.build();
    }

    private NumericMetricValueMsg mapNumericMetricValue(NumericMetricValue value) {
        var builder = NumericMetricValueMsg.newBuilder()
                .setAbstractMetricValue(mapAbstractMetricValue(value));
        // this being a string really doesn't feel right, but is reasonable considering it's xsd:decimal
        Util.doIfNotNull(value.getValue(), val ->
                builder.setAValue(Util.toStringValue(val.toPlainString()))
        );
        return builder.build();
    }

    private StringMetricValueMsg mapStringMetricValue(StringMetricValue value) {
        var builder = StringMetricValueMsg.newBuilder();

        builder.setAbstractMetricValue(mapAbstractMetricValue(value));
        Util.doIfNotNull(value.getValue(), it -> builder.setAValue(Util.toStringValue(it)));

        return builder.build();
    }

    private AbstractMetricValueMsg mapAbstractMetricValue(AbstractMetricValue value) {
        var builder = AbstractMetricValueMsg.newBuilder();
        Util.doIfNotNull(
                value.getStartTime(),
                time -> builder.setAStartTime(Util.toUInt64(timestampAdapter.marshal(time)))
        );
        Util.doIfNotNull(
                value.getStopTime(),
                time -> builder.setAStopTime(Util.toUInt64(timestampAdapter.marshal(time)))
        );
        Util.doIfNotNull(
                value.getDeterminationTime(),
                time -> builder.setADeterminationTime(Util.toUInt64(timestampAdapter.marshal(time)))
        );
        builder.setMetricQuality(mapMetricQuality(value.getMetricQuality()));
        value.getAnnotation().forEach(it -> builder.addAnnotation(mapAnnotation(it)));
        return builder.build();
    }

    private AbstractMetricValueMsg.MetricQualityMsg mapMetricQuality(AbstractMetricValue.MetricQuality quality) {
        var builder = AbstractMetricValueMsg.MetricQualityMsg.newBuilder();
        Util.doIfNotNull(quality.getMode(), mode ->
                builder.setAMode(Util.mapToProtoEnum(mode, GenerationModeMsg.class)));
        Util.doIfNotNull(quality.getValidity(), validity ->
                builder.setAValidity(Util.mapToProtoEnum(validity, MeasurementValidityMsg.class)));
        Util.doIfNotNull(quality.getQi(), qi -> builder.setAQi(Util.toStringValue(qi.toPlainString())));

        return builder.build();
    }

    private AbstractMetricValueMsg.AnnotationMsg mapAnnotation(AbstractMetricValue.Annotation annotation) {
        var builder = AbstractMetricValueMsg.AnnotationMsg.newBuilder();
        builder.setType(baseMapper.mapCodedValue(annotation.getType()));
        return builder.build();
    }

    private AbstractMetricDescriptorMsg.RelationMsg mapRelation(AbstractMetricDescriptor.Relation relation) {
        var builder = AbstractMetricDescriptorMsg.RelationMsg.newBuilder();

        builder.setRelation(Util.mapToProtoEnum(relation.getKind(), AbstractMetricDescriptorMsg.RelationMsg.KindMsg.class));
        builder.setAEntries(mapEntry(relation.getEntries()));
        Util.doIfNotNull(relation.getCode(), codedValue -> builder.setCode(baseMapper.mapCodedValue(codedValue)));
        Util.doIfNotNull(relation.getIdentification(), instanceIdentifier ->
                builder.setIdentification(baseMapper.mapInstanceIdentifierOneOf(instanceIdentifier)));

        return builder.build();
    }

    private EntryRefMsg mapEntry(List<String> entries) {
        var builder = EntryRefMsg.newBuilder();
        builder.addAllEntryRef(entries);
        return builder.build();
    }



}
