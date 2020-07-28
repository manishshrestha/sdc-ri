package org.somda.sdc.proto.mapping;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.participant.AbstractComplexDeviceComponentState;
import org.somda.sdc.biceps.model.participant.AbstractDeviceComponentState;
import org.somda.sdc.biceps.model.participant.AbstractMetricState;
import org.somda.sdc.biceps.model.participant.AbstractState;
import org.somda.sdc.biceps.model.participant.ChannelState;
import org.somda.sdc.biceps.model.participant.EnumStringMetricState;
import org.somda.sdc.biceps.model.participant.MdsState;
import org.somda.sdc.biceps.model.participant.StringMetricState;
import org.somda.sdc.biceps.model.participant.VmdState;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.proto.model.biceps.AbstractComplexDeviceComponentStateOneOfMsg;
import org.somda.sdc.proto.model.biceps.AbstractDeviceComponentStateOneOfMsg;
import org.somda.sdc.proto.model.biceps.AbstractMetricStateOneOfMsg;
import org.somda.sdc.proto.model.biceps.AbstractStateOneOfMsg;
import org.somda.sdc.proto.model.biceps.StringMetricStateOneOfMsg;

public class PojoToProtoOneOfMapper {
    private static final Logger LOG = LogManager.getLogger(PojoToProtoOneOfMapper.class);
    private final Logger instanceLogger;
    private final PojoToProtoBaseMapper baseMapper;
    private final PojoToProtoAlertMapper alertMapper;
    private final PojoToProtoComponentMapper componentMapper;
    private final PojoToProtoContextMapper contextMapper;
    private final PojoToProtoMetricMapper metricMapper;
    private final PojoToProtoOperationMapper operationMapper;

    @Inject
    PojoToProtoOneOfMapper(@Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier,
                           PojoToProtoBaseMapper baseMapper,
                           PojoToProtoAlertMapper alertMapper,
                           PojoToProtoComponentMapper componentMapper,
                           PojoToProtoContextMapper contextMapper,
                           PojoToProtoMetricMapper metricMapper,
                           PojoToProtoOperationMapper operationMapper) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.baseMapper = baseMapper;
        this.alertMapper = alertMapper;
        this.componentMapper = componentMapper;
        this.contextMapper = contextMapper;
        this.metricMapper = metricMapper;
        this.operationMapper = operationMapper;
    }

    public AbstractStateOneOfMsg mapAbstractStateOneOf(AbstractState state) {
        var builder = AbstractStateOneOfMsg.newBuilder();
        if (state instanceof org.somda.sdc.biceps.model.participant.AbstractDeviceComponentState) {
            builder.setAbstractDeviceComponentStateOneOf(
                    mapAbstractDeviceComponentStateOneOf((AbstractDeviceComponentState) state));
        } else if (state instanceof org.somda.sdc.biceps.model.participant.StringMetricState) {
            builder.setAbstractMetricStateOneOf(mapAbstractMetricStateOneOf((AbstractMetricState) state));
        } else {
            LOG.error("Class {} not supported", state.getClass());
            //throw new IllegalArgumentException(String.format("Class %s not supported", state.getClass()));
        }

        return builder.build();
    }

    public AbstractMetricStateOneOfMsg mapAbstractMetricStateOneOf(AbstractMetricState state) {
        var builder = AbstractMetricStateOneOfMsg.newBuilder();
        if (state instanceof StringMetricState) {
            builder.setStringMetricStateOneOf(mapStringMetricStateOneOf((StringMetricState) state));
        } else {
            LOG.error("Class {} not supported", state.getClass());
        }
        return builder.build();
    }

    public StringMetricStateOneOfMsg mapStringMetricStateOneOf(StringMetricState state) {
        var builder = StringMetricStateOneOfMsg.newBuilder();
        if (state instanceof EnumStringMetricState) {
            LOG.error("Class {} not supported", state.getClass());
        } else {
            builder.setStringMetricState(metricMapper.mapStringMetricState(state));
        }
        return builder.build();
    }


    public AbstractDeviceComponentStateOneOfMsg mapAbstractDeviceComponentStateOneOf(
            AbstractDeviceComponentState state) {
        var builder = AbstractDeviceComponentStateOneOfMsg.newBuilder();
        if (state instanceof ChannelState) {
            builder.setChannelState(componentMapper.mapChannelState((ChannelState) state));
        } else if (state instanceof AbstractComplexDeviceComponentState) {
            builder.setAbstractComplexDeviceComponentStateOneOf(
                    mapAbstractComplexDeviceComponentStateOneOf((AbstractComplexDeviceComponentState) state));
        } else {
            LOG.error("Class {} not supported", state.getClass());
            // throw new IllegalArgumentException(String.format("Class %s not supported", state.getClass()));
        }

        return builder.build();
    }

    public AbstractComplexDeviceComponentStateOneOfMsg mapAbstractComplexDeviceComponentStateOneOf(
            AbstractComplexDeviceComponentState state) {
        var builder = AbstractComplexDeviceComponentStateOneOfMsg.newBuilder();
        if (state instanceof MdsState) {
            builder.setMdsState(componentMapper.mapMdsState((MdsState) state));
        } else if (state instanceof VmdState) {
            builder.setVmdState(componentMapper.mapVmdState((VmdState) state));
        } else {
            LOG.error("Class {} not supported", state.getClass());
            // throw new IllegalArgumentException(String.format("Class %s not supported", state.getClass()));
        }

        return builder.build();
    }
}
