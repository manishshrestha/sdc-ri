package org.somda.sdc.proto.mapping;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.participant.AbstractMetricDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractMetricState;
import org.somda.sdc.biceps.model.participant.AbstractMetricValue;
import org.somda.sdc.biceps.model.participant.StringMetricDescriptor;
import org.somda.sdc.biceps.model.participant.StringMetricState;
import org.somda.sdc.biceps.model.participant.StringMetricValue;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.proto.model.biceps.AbstractMetricDescriptorMsg;
import org.somda.sdc.proto.model.biceps.AbstractMetricStateMsg;
import org.somda.sdc.proto.model.biceps.AbstractMetricValueMsg;
import org.somda.sdc.proto.model.biceps.ComponentActivationMsg;
import org.somda.sdc.proto.model.biceps.MetricCategoryMsg;
import org.somda.sdc.proto.model.biceps.StringMetricDescriptorMsg;
import org.somda.sdc.proto.model.biceps.StringMetricStateMsg;
import org.somda.sdc.proto.model.biceps.StringMetricValueMsg;

import java.math.BigInteger;

public class PojoToProtoMetricMapper {
    private static final Logger LOG = LogManager.getLogger(PojoToProtoMetricMapper.class);
    private final Logger instanceLogger;
    private final PojoToProtoBaseMapper baseMapper;

    @Inject
    PojoToProtoMetricMapper(@Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier,
                           PojoToProtoBaseMapper baseMapper) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.baseMapper = baseMapper;
    }

    public StringMetricDescriptorMsg.Builder mapStringMetricDescriptor(StringMetricDescriptor stringMetricDescriptor) {
        return StringMetricDescriptorMsg.newBuilder()
                .setAbstractMetricDescriptor(mapAbstractMetricDescriptor(stringMetricDescriptor));
    }

    private AbstractMetricDescriptorMsg.Builder mapAbstractMetricDescriptor(
            AbstractMetricDescriptor abstractMetricDescriptor) {
        var builder = AbstractMetricDescriptorMsg.newBuilder()
                .setAbstractDescriptor(baseMapper.mapAbstractDescriptor(abstractMetricDescriptor));
        Util.doIfNotNull(
                abstractMetricDescriptor.getMetricCategory(),
                category -> builder.setAMetricCategory(Util.mapToProtoEnum(category, MetricCategoryMsg.class))
        );

        return builder;
    }

    public StringMetricStateMsg mapStringMetricState(StringMetricState state) {
        var builder = StringMetricStateMsg.newBuilder();
        Util.doIfNotNull(state.getMetricValue(), value -> builder.setMetricValue(mapStringMetricValue(value)));
        builder.setAbstractMetricState(mapAbstractMetricState(state));

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
        return builder.build();
    }
    private StringMetricValueMsg mapStringMetricValue(StringMetricValue value) {
        var builder = StringMetricValueMsg.newBuilder();
        builder.setAValue(Util.toStringValue(value.getValue()));
        builder.setAbstractMetricValue(mapAbstractMetricValue(value));
        return builder.build();
    }

    private AbstractMetricValueMsg mapAbstractMetricValue(AbstractMetricValue value) {
        var builder = AbstractMetricValueMsg.newBuilder();
        Util.doIfNotNull(
                value.getDeterminationTime(),
                time -> builder.setADeterminationTime(Util.toUInt64(BigInteger.valueOf(time.toEpochMilli())))
        );
        Util.doIfNotNull(
                value.getStartTime(),
                time -> builder.setAStartTime(Util.toUInt64(BigInteger.valueOf(time.toEpochMilli())))
        );
        Util.doIfNotNull(
                value.getStopTime(),
                time -> builder.setAStopTime(Util.toUInt64(BigInteger.valueOf(time.toEpochMilli())))
        );
        Util.doIfNotNull(value.getMetricQuality(), quality ->
                builder.setMetricQuality(Util.mapToProtoEnum(quality, AbstractMetricValueMsg.MetricQualityMsg.class)));
        return builder.build();
    }
}
