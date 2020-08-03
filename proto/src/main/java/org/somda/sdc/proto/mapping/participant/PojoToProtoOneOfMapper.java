package org.somda.sdc.proto.mapping.participant;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.participant.AbstractAlertState;
import org.somda.sdc.biceps.model.participant.AbstractComplexDeviceComponentState;
import org.somda.sdc.biceps.model.participant.AbstractContextState;
import org.somda.sdc.biceps.model.participant.AbstractDeviceComponentState;
import org.somda.sdc.biceps.model.participant.AbstractMetricState;
import org.somda.sdc.biceps.model.participant.AbstractMultiState;
import org.somda.sdc.biceps.model.participant.AbstractOperationState;
import org.somda.sdc.biceps.model.participant.AbstractState;
import org.somda.sdc.biceps.model.participant.ActivateOperationState;
import org.somda.sdc.biceps.model.participant.AlertConditionState;
import org.somda.sdc.biceps.model.participant.AlertSignalState;
import org.somda.sdc.biceps.model.participant.AlertSystemState;
import org.somda.sdc.biceps.model.participant.ChannelState;
import org.somda.sdc.biceps.model.participant.EnsembleContextState;
import org.somda.sdc.biceps.model.participant.EnumStringMetricState;
import org.somda.sdc.biceps.model.participant.LimitAlertConditionState;
import org.somda.sdc.biceps.model.participant.LocationContextState;
import org.somda.sdc.biceps.model.participant.MdsState;
import org.somda.sdc.biceps.model.participant.NumericMetricState;
import org.somda.sdc.biceps.model.participant.RealTimeSampleArrayMetricState;
import org.somda.sdc.biceps.model.participant.ScoState;
import org.somda.sdc.biceps.model.participant.SetAlertStateOperationState;
import org.somda.sdc.biceps.model.participant.SetComponentStateOperationState;
import org.somda.sdc.biceps.model.participant.SetContextStateOperationState;
import org.somda.sdc.biceps.model.participant.SetMetricStateOperationState;
import org.somda.sdc.biceps.model.participant.SetStringOperationState;
import org.somda.sdc.biceps.model.participant.SetValueOperationState;
import org.somda.sdc.biceps.model.participant.StringMetricState;
import org.somda.sdc.biceps.model.participant.SystemContextState;
import org.somda.sdc.biceps.model.participant.VmdState;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.proto.model.biceps.AbstractAlertStateOneOfMsg;
import org.somda.sdc.proto.model.biceps.AbstractComplexDeviceComponentStateOneOfMsg;
import org.somda.sdc.proto.model.biceps.AbstractContextStateOneOfMsg;
import org.somda.sdc.proto.model.biceps.AbstractDeviceComponentStateOneOfMsg;
import org.somda.sdc.proto.model.biceps.AbstractMetricStateOneOfMsg;
import org.somda.sdc.proto.model.biceps.AbstractMultiStateOneOfMsg;
import org.somda.sdc.proto.model.biceps.AbstractOperationStateOneOfMsg;
import org.somda.sdc.proto.model.biceps.AbstractStateOneOfMsg;
import org.somda.sdc.proto.model.biceps.AlertConditionStateOneOfMsg;
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
        if (state instanceof AbstractDeviceComponentState) {
            builder.setAbstractDeviceComponentStateOneOf(
                    mapAbstractDeviceComponentStateOneOf((AbstractDeviceComponentState) state));
        } else if (state instanceof AbstractMetricState) {
            builder.setAbstractMetricStateOneOf(mapAbstractMetricStateOneOf((AbstractMetricState) state));
        } else if (state instanceof AbstractMultiState) {
            builder.setAbstractMultiStateOneOf(mapAbstractMultiStateOneOf((AbstractMultiState) state));
        } else if (state instanceof AbstractAlertState) {
            builder.setAbstractAlertStateOneOf(mapAbstractAlertStateOneOf((AbstractAlertState) state));
        } else if (state instanceof AbstractOperationState) {
            builder.setAbstractOperationStateOneOf(mapAbstractOperationStateOneOf((AbstractOperationState) state));
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
        } else if (state instanceof NumericMetricState) {
            builder.setNumericMetricState(metricMapper.mapNumericMetricState((NumericMetricState) state));
        } else if (state instanceof RealTimeSampleArrayMetricState) {
            builder.setRealTimeSampleArrayMetricState(metricMapper.mapRealTimeSampleArrayMetricState((RealTimeSampleArrayMetricState) state));
        } else {
            LOG.error("Class {} not supported", state.getClass());
        }
        return builder.build();
    }

    public StringMetricStateOneOfMsg mapStringMetricStateOneOf(StringMetricState state) {
        var builder = StringMetricStateOneOfMsg.newBuilder();
        if (state instanceof EnumStringMetricState) {
            builder.setEnumStringMetricState(metricMapper.mapEnumStringMetricState((EnumStringMetricState) state));
        } else {
            builder.setStringMetricState(metricMapper.mapStringMetricState(state));
        }
        return builder.build();
    }

    public AbstractOperationStateOneOfMsg mapAbstractOperationStateOneOf(AbstractOperationState state) {
        var builder = AbstractOperationStateOneOfMsg.newBuilder();
        if (state instanceof ActivateOperationState) {
            builder.setActivateOperationState(
                    operationMapper.mapActivateOperationState((ActivateOperationState) state));
        } else if (state instanceof SetMetricStateOperationState) {
            builder.setSetMetricStateOperationState(
                    operationMapper.mapSetMetricStateOperationState((SetMetricStateOperationState) state));
        } else if (state instanceof SetStringOperationState) {
            builder.setSetStringOperationState(
                    operationMapper.mapSetStringOperationState((SetStringOperationState) state));
        } else if (state instanceof SetValueOperationState) {
            builder.setSetValueOperationState(
                    operationMapper.mapSetValueOperationState((SetValueOperationState) state));
        } else if (state instanceof SetComponentStateOperationState) {
            builder.setSetComponentStateOperationState(
                    operationMapper.mapSetComponentStateOperationState((SetComponentStateOperationState) state));
        } else if (state instanceof SetAlertStateOperationState) {
            builder.setSetAlertStateOperationState(
                    operationMapper.mapSetAlertStateOperationState((SetAlertStateOperationState) state));
        } else if (state instanceof SetContextStateOperationState) {
            builder.setSetContextStateOperationState(
                    operationMapper.mapSetContextStateOperationState((SetContextStateOperationState) state));
        } else {
            LOG.error("Class {} not supported", state.getClass());
        }
        return builder.build();
    }


    public AbstractAlertStateOneOfMsg mapAbstractAlertStateOneOf(AbstractAlertState state) {
        var builder = AbstractAlertStateOneOfMsg.newBuilder();
        if (state instanceof AlertSystemState) {
            builder.setAlertSystemState(alertMapper.mapAlertSystemState((AlertSystemState) state));
        } else if (state instanceof AlertConditionState) {
            builder.setAlertConditionStateOneOf(mapAlertConditionStateOneOf((AlertConditionState) state));
        } else if (state instanceof AlertSignalState) {
            builder.setAlertSignalState(alertMapper.mapAlertSignalState((AlertSignalState) state));
        } else {
            LOG.error("Class {} not supported", state.getClass());
        }
        return builder.build();
    }

    public AlertConditionStateOneOfMsg mapAlertConditionStateOneOf(AlertConditionState state) {
        var builder = AlertConditionStateOneOfMsg.newBuilder();
        if (state instanceof LimitAlertConditionState) {
            LOG.error("Class {} not supported", state.getClass());
        } else {
            builder.setAlertConditionState(alertMapper.mapAlertConditionState(state));
        }
        return builder.build();
    }

    public AbstractMultiStateOneOfMsg mapAbstractMultiStateOneOf(AbstractMultiState state) {
        var builder = AbstractMultiStateOneOfMsg.newBuilder();
        if (state instanceof AbstractContextState) {
            builder.setAbstractContextStateOneOf(mapAbstractContextStateOneOf((AbstractContextState) state));
        } else {
            LOG.error("Class {} not supported", state.getClass());
        }
        return builder.build();
    }

    public AbstractContextStateOneOfMsg mapAbstractContextStateOneOf(AbstractContextState state) {
        var builder = AbstractContextStateOneOfMsg.newBuilder();
        if (state instanceof EnsembleContextState) {
            builder.setEnsembleContextState(contextMapper.mapEnsembleContextState((EnsembleContextState) state));
        } else if (state instanceof LocationContextState) {
            builder.setLocationContextState(contextMapper.mapLocationContextState((LocationContextState) state));
        } else {
            LOG.error("Class {} not supported", state.getClass());
        }
        return builder.build();
    }

    public AbstractDeviceComponentStateOneOfMsg mapAbstractDeviceComponentStateOneOf(
            AbstractDeviceComponentState state) {
        var builder = AbstractDeviceComponentStateOneOfMsg.newBuilder();
        if (state instanceof ChannelState) {
            builder.setChannelState(componentMapper.mapChannelState((ChannelState) state));
        } else if (state instanceof ScoState) {
            builder.setScoState(componentMapper.mapScoState((ScoState) state));
        } else if (state instanceof AbstractComplexDeviceComponentState) {
            builder.setAbstractComplexDeviceComponentStateOneOf(
                    mapAbstractComplexDeviceComponentStateOneOf((AbstractComplexDeviceComponentState) state));
        } else if (state instanceof SystemContextState) {
            builder.setSystemContextState(componentMapper.mapSystemContextState((SystemContextState) state));
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