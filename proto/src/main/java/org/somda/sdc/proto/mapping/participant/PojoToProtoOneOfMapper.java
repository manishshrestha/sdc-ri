package org.somda.sdc.proto.mapping.participant;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.participant.AbstractAlertDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractAlertState;
import org.somda.sdc.biceps.model.participant.AbstractComplexDeviceComponentDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractComplexDeviceComponentState;
import org.somda.sdc.biceps.model.participant.AbstractContextState;
import org.somda.sdc.biceps.model.participant.AbstractDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractDeviceComponentDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractDeviceComponentState;
import org.somda.sdc.biceps.model.participant.AbstractMetricDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractMetricState;
import org.somda.sdc.biceps.model.participant.AbstractMultiState;
import org.somda.sdc.biceps.model.participant.AbstractOperationDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractOperationState;
import org.somda.sdc.biceps.model.participant.AbstractSetStateOperationDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractState;
import org.somda.sdc.biceps.model.participant.ActivateOperationDescriptor;
import org.somda.sdc.biceps.model.participant.ActivateOperationState;
import org.somda.sdc.biceps.model.participant.AlertConditionDescriptor;
import org.somda.sdc.biceps.model.participant.AlertConditionState;
import org.somda.sdc.biceps.model.participant.AlertSignalDescriptor;
import org.somda.sdc.biceps.model.participant.AlertSignalState;
import org.somda.sdc.biceps.model.participant.AlertSystemDescriptor;
import org.somda.sdc.biceps.model.participant.AlertSystemState;
import org.somda.sdc.biceps.model.participant.BatteryDescriptor;
import org.somda.sdc.biceps.model.participant.ChannelDescriptor;
import org.somda.sdc.biceps.model.participant.ChannelState;
import org.somda.sdc.biceps.model.participant.ClockDescriptor;
import org.somda.sdc.biceps.model.participant.DistributionSampleArrayMetricDescriptor;
import org.somda.sdc.biceps.model.participant.EnsembleContextState;
import org.somda.sdc.biceps.model.participant.EnumStringMetricDescriptor;
import org.somda.sdc.biceps.model.participant.EnumStringMetricState;
import org.somda.sdc.biceps.model.participant.LimitAlertConditionDescriptor;
import org.somda.sdc.biceps.model.participant.LimitAlertConditionState;
import org.somda.sdc.biceps.model.participant.LocationContextState;
import org.somda.sdc.biceps.model.participant.MdsDescriptor;
import org.somda.sdc.biceps.model.participant.MdsState;
import org.somda.sdc.biceps.model.participant.NumericMetricDescriptor;
import org.somda.sdc.biceps.model.participant.NumericMetricState;
import org.somda.sdc.biceps.model.participant.RealTimeSampleArrayMetricDescriptor;
import org.somda.sdc.biceps.model.participant.RealTimeSampleArrayMetricState;
import org.somda.sdc.biceps.model.participant.ScoDescriptor;
import org.somda.sdc.biceps.model.participant.ScoState;
import org.somda.sdc.biceps.model.participant.SetAlertStateOperationDescriptor;
import org.somda.sdc.biceps.model.participant.SetAlertStateOperationState;
import org.somda.sdc.biceps.model.participant.SetComponentStateOperationDescriptor;
import org.somda.sdc.biceps.model.participant.SetComponentStateOperationState;
import org.somda.sdc.biceps.model.participant.SetContextStateOperationDescriptor;
import org.somda.sdc.biceps.model.participant.SetContextStateOperationState;
import org.somda.sdc.biceps.model.participant.SetMetricStateOperationDescriptor;
import org.somda.sdc.biceps.model.participant.SetMetricStateOperationState;
import org.somda.sdc.biceps.model.participant.SetStringOperationDescriptor;
import org.somda.sdc.biceps.model.participant.SetStringOperationState;
import org.somda.sdc.biceps.model.participant.SetValueOperationDescriptor;
import org.somda.sdc.biceps.model.participant.SetValueOperationState;
import org.somda.sdc.biceps.model.participant.StringMetricDescriptor;
import org.somda.sdc.biceps.model.participant.StringMetricState;
import org.somda.sdc.biceps.model.participant.SystemContextDescriptor;
import org.somda.sdc.biceps.model.participant.SystemContextState;
import org.somda.sdc.biceps.model.participant.VmdDescriptor;
import org.somda.sdc.biceps.model.participant.VmdState;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.proto.model.biceps.AbstractAlertDescriptorOneOfMsg;
import org.somda.sdc.proto.model.biceps.AbstractAlertStateOneOfMsg;
import org.somda.sdc.proto.model.biceps.AbstractComplexDeviceComponentDescriptorOneOfMsg;
import org.somda.sdc.proto.model.biceps.AbstractComplexDeviceComponentStateOneOfMsg;
import org.somda.sdc.proto.model.biceps.AbstractContextStateOneOfMsg;
import org.somda.sdc.proto.model.biceps.AbstractDescriptorOneOfMsg;
import org.somda.sdc.proto.model.biceps.AbstractDeviceComponentDescriptorOneOfMsg;
import org.somda.sdc.proto.model.biceps.AbstractDeviceComponentStateOneOfMsg;
import org.somda.sdc.proto.model.biceps.AbstractMetricDescriptorOneOfMsg;
import org.somda.sdc.proto.model.biceps.AbstractMetricStateOneOfMsg;
import org.somda.sdc.proto.model.biceps.AbstractMultiStateOneOfMsg;
import org.somda.sdc.proto.model.biceps.AbstractOperationDescriptorOneOfMsg;
import org.somda.sdc.proto.model.biceps.AbstractOperationStateOneOfMsg;
import org.somda.sdc.proto.model.biceps.AbstractSetStateOperationDescriptorOneOfMsg;
import org.somda.sdc.proto.model.biceps.AbstractStateOneOfMsg;
import org.somda.sdc.proto.model.biceps.AlertConditionDescriptorOneOfMsg;
import org.somda.sdc.proto.model.biceps.AlertConditionStateOneOfMsg;
import org.somda.sdc.proto.model.biceps.StringMetricDescriptorOneOfMsg;
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
            instanceLogger.error("Class {} not supported", state.getClass());
        }

        return builder.build();
    }

    public AbstractDescriptorOneOfMsg mapAbstractDescriptor(AbstractDescriptor descriptor) {
        var builder = AbstractDescriptorOneOfMsg.newBuilder();
        if (descriptor instanceof AbstractDeviceComponentDescriptor) {
            builder.setAbstractDeviceComponentDescriptorOneOf(
                    mapAbstractDeviceComponentDescriptorOneOf((AbstractDeviceComponentDescriptor) descriptor)
            );
        } else if (descriptor instanceof AbstractMetricDescriptor) {
            builder.setAbstractMetricDescriptorOneOf(
                    mapAbstractMetricDescriptor((AbstractMetricDescriptor) descriptor)
            );
        } else if (descriptor instanceof AbstractAlertDescriptor) {
            builder.setAbstractAlertDescriptorOneOf(mapAbstractAlertDescriptor((AbstractAlertDescriptor) descriptor));
        } else if (descriptor instanceof AbstractOperationDescriptor) {
            builder.setAbstractOperationDescriptorOneOf(
                    mapAbstractOperationDescriptor((AbstractOperationDescriptor) descriptor)
            );
        } else {
            instanceLogger.error("Class {} not supported", descriptor.getClass());
        }

        return builder.build();
    }

    public AbstractOperationDescriptorOneOfMsg mapAbstractOperationDescriptor(AbstractOperationDescriptor descriptor) {
        var builder = AbstractOperationDescriptorOneOfMsg.newBuilder();
        if (descriptor instanceof AbstractSetStateOperationDescriptor) {
            builder.setAbstractSetStateOperationDescriptorOneOf(
                    mapAbstractSetStateOperationDescriptor((AbstractSetStateOperationDescriptor) descriptor)
            );
        } else if (descriptor instanceof SetStringOperationDescriptor) {
            builder.setSetStringOperationDescriptor(
                    operationMapper.mapSetStringOperationDescriptor((SetStringOperationDescriptor) descriptor)
            );
        } else if (descriptor instanceof SetValueOperationDescriptor) {
            builder.setSetValueOperationDescriptor(
                    operationMapper.mapSetValueOperationDescriptor((SetValueOperationDescriptor) descriptor)
            );
        } else {
            instanceLogger.error("Class {} not supported", descriptor.getClass());
        }

        return builder.build();
    }

    private AbstractSetStateOperationDescriptorOneOfMsg mapAbstractSetStateOperationDescriptor(AbstractSetStateOperationDescriptor descriptor) {
        var builder = AbstractSetStateOperationDescriptorOneOfMsg.newBuilder();

        if (descriptor instanceof ActivateOperationDescriptor) {
            builder.setActivateOperationDescriptor(
                    operationMapper.mapActivateOperationDescriptor((ActivateOperationDescriptor) descriptor)
            );
        } else if (descriptor instanceof SetAlertStateOperationDescriptor) {
            builder.setSetAlertStateOperationDescriptor(
                    operationMapper.mapSetAlertStateOperationDescriptor((SetAlertStateOperationDescriptor) descriptor)
            );
        } else if (descriptor instanceof SetComponentStateOperationDescriptor) {
            builder.setSetComponentStateOperationDescriptor(
                    operationMapper.mapSetComponentStateOperationDescriptor((SetComponentStateOperationDescriptor) descriptor)
            );
        } else if (descriptor instanceof SetContextStateOperationDescriptor) {
            builder.setSetContextStateOperationDescriptor(
                    operationMapper.mapSetContextStateOperationDescriptor((SetContextStateOperationDescriptor) descriptor)
            );
        } else if (descriptor instanceof SetMetricStateOperationDescriptor) {
            builder.setSetMetricStateOperationDescriptor(
                    operationMapper.mapSetMetricStateOperationDescriptor((SetMetricStateOperationDescriptor) descriptor)
            );
        } else {
            instanceLogger.error("Class {} not supported", descriptor.getClass());
        }

        return builder.build();
    }

    private AbstractAlertDescriptorOneOfMsg mapAbstractAlertDescriptor(AbstractAlertDescriptor descriptor) {
        var builder = AbstractAlertDescriptorOneOfMsg.newBuilder();
        if (descriptor instanceof AlertConditionDescriptor) {
            builder.setAlertConditionDescriptorOneOf(mapAlertConditionDescriptor((AlertConditionDescriptor) descriptor));
        } else if (descriptor instanceof AlertSignalDescriptor) {
            builder.setAlertSignalDescriptor(alertMapper.mapAlertSignalDescriptor((AlertSignalDescriptor) descriptor));
        } else if (descriptor instanceof AlertSystemDescriptor) {
            builder.setAlertSystemDescriptor(alertMapper.mapAlertSystemDescriptor((AlertSystemDescriptor) descriptor));
        } else {
            instanceLogger.error("Class {} not supported", descriptor.getClass());
        }
        return builder.build();
    }

    public AlertConditionDescriptorOneOfMsg mapAlertConditionDescriptor(AlertConditionDescriptor descriptor) {
        var builder = AlertConditionDescriptorOneOfMsg.newBuilder();
        if (descriptor instanceof LimitAlertConditionDescriptor) {
            instanceLogger.error("Class {} not supported", descriptor.getClass());
        } else {
            builder.setAlertConditionDescriptor(alertMapper.mapAlertConditionDescriptor(descriptor));
        }
        return builder.build();
    }

    public AbstractMetricDescriptorOneOfMsg mapAbstractMetricDescriptor(AbstractMetricDescriptor descriptor) {
        var builder = AbstractMetricDescriptorOneOfMsg.newBuilder();
        if (descriptor instanceof DistributionSampleArrayMetricDescriptor) {
            instanceLogger.error("Class {} not supported", descriptor.getClass());
        } else if (descriptor instanceof NumericMetricDescriptor) {
            builder.setNumericMetricDescriptor(
                    metricMapper.mapNumericMetricDescriptor((NumericMetricDescriptor) descriptor)
            );
        } else if (descriptor instanceof RealTimeSampleArrayMetricDescriptor) {
            builder.setRealTimeSampleArrayMetricDescriptor(
                    metricMapper.mapRealTimeSampleArrayMetricDescriptor((RealTimeSampleArrayMetricDescriptor) descriptor)
            );
        } else if (descriptor instanceof StringMetricDescriptor) {
            builder.setStringMetricDescriptorOneOf(
                    mapStringMetricDescriptor((StringMetricDescriptor) descriptor)
            );
        } else {
            instanceLogger.error("Class {} not supported", descriptor.getClass());
        }
        return builder.build();
    }

    private StringMetricDescriptorOneOfMsg mapStringMetricDescriptor(StringMetricDescriptor descriptor) {
        var builder = StringMetricDescriptorOneOfMsg.newBuilder();
        if (descriptor instanceof EnumStringMetricDescriptor) {
            builder.setEnumStringMetricDescriptor(
                    metricMapper.mapEnumStringMetricDescriptor((EnumStringMetricDescriptor) descriptor)
            );
        } else {
            builder.setStringMetricDescriptor(metricMapper.mapStringMetricDescriptor(descriptor));
        }
        return builder.build();
    }

    public AbstractDeviceComponentDescriptorOneOfMsg mapAbstractDeviceComponentDescriptorOneOf(
            AbstractDeviceComponentDescriptor descriptor
    ) {
        var builder = AbstractDeviceComponentDescriptorOneOfMsg.newBuilder();
        if (descriptor instanceof AbstractComplexDeviceComponentDescriptor) {
            builder.setAbstractComplexDeviceComponentDescriptorOneOf(
                    mapAbstractComplexDeviceComponentDescriptor((AbstractComplexDeviceComponentDescriptor) descriptor)
            );
        } else if (descriptor instanceof BatteryDescriptor) {
            instanceLogger.error("Class {} not supported", descriptor.getClass());
        } else if (descriptor instanceof ChannelDescriptor) {
            builder.setChannelDescriptor(componentMapper.mapChannelDescriptor((ChannelDescriptor) descriptor));
        } else if (descriptor instanceof ClockDescriptor) {
            instanceLogger.error("Class {} not supported", descriptor.getClass());
        } else if (descriptor instanceof ScoDescriptor) {
            builder.setScoDescriptor(componentMapper.mapScoDescriptor((ScoDescriptor) descriptor));
        } else if (descriptor instanceof SystemContextDescriptor) {
            builder.setSystemContextDescriptor(
                    componentMapper.mapSystemContextDescriptor((SystemContextDescriptor) descriptor)
            );
        } else {
            instanceLogger.error("Class {} not supported", descriptor.getClass());
        }
        return builder.build();
    }

    private AbstractComplexDeviceComponentDescriptorOneOfMsg mapAbstractComplexDeviceComponentDescriptor(
            AbstractComplexDeviceComponentDescriptor descriptor
    ) {
        var builder = AbstractComplexDeviceComponentDescriptorOneOfMsg.newBuilder();
        if (descriptor instanceof MdsDescriptor) {
            builder.setMdsDescriptor(componentMapper.mapMdsDescriptor((MdsDescriptor) descriptor));
        } else if (descriptor instanceof VmdDescriptor) {
            builder.setVmdDescriptor(componentMapper.mapVmdDescriptor((VmdDescriptor) descriptor));
        } else {
            instanceLogger.error("Class {} not supported", descriptor.getClass());
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
            instanceLogger.error("Class {} not supported", state.getClass());
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
            instanceLogger.error("Class {} not supported", state.getClass());
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
            instanceLogger.error("Class {} not supported", state.getClass());
        }
        return builder.build();
    }

    public AlertConditionStateOneOfMsg mapAlertConditionStateOneOf(AlertConditionState state) {
        var builder = AlertConditionStateOneOfMsg.newBuilder();
        if (state instanceof LimitAlertConditionState) {
            instanceLogger.error("Class {} not supported", state.getClass());
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
            instanceLogger.error("Class {} not supported", state.getClass());
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
            instanceLogger.error("Class {} not supported", state.getClass());
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
            instanceLogger.error("Class {} not supported", state.getClass());
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
            instanceLogger.error("Class {} not supported", state.getClass());
            // throw new IllegalArgumentException(String.format("Class %s not supported", state.getClass()));
        }

        return builder.build();
    }
}
