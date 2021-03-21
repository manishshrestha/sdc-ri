package org.somda.sdc.proto.mapping.participant;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.proto.mapping.Util;
import org.somda.protosdc.proto.model.biceps.*;

import javax.annotation.Nullable;
import java.util.function.Function;

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

    public PatientDemographicsCoreDataOneOfMsg mapPatientDemographicsCoreDataOneOf(PatientDemographicsCoreData coreData) {
        var builder = PatientDemographicsCoreDataOneOfMsg.newBuilder();

        if (coreData instanceof NeonatalPatientDemographicsCoreData) {
            builder.setNeonatalPatientDemographicsCoreData(
                    contextMapper.mapNeonatalPatientDemographicsCoreData((NeonatalPatientDemographicsCoreData) coreData)
            );
        } else {
            builder.setPatientDemographicsCoreData(
                    contextMapper.mapPatientDemographicsCoreData(coreData)
            );
        }

        return builder.build();
    }

    public <T> void buildAbstractComplexDeviceComponentState(
            AbstractDeviceComponentState state,
            Function<MdsStateMsg, T> mdsMapper,
            Function<VmdStateMsg, T> vmdMapper
    ) {
        if (state instanceof MdsState) {
            mdsMapper.apply(componentMapper.mapMdsState((MdsState) state));
        } else if (state instanceof VmdState) {
            vmdMapper.apply(componentMapper.mapVmdState((VmdState) state));
        } else {
            instanceLogger.error("Class {} not supported", state.getClass());
        }
    }

    public <T> void buildAbstractDeviceComponentState(
            AbstractDeviceComponentState state,
            Function<BatteryStateMsg, T> batteryMapper,
            Function<ChannelStateMsg, T> channelMapper,
            Function<ClockStateMsg, T> clockMapper,
            Function<ScoStateMsg, T> scoMapper,
            Function<SystemContextStateMsg, T> systemContextMapper
    ) {
        if (state instanceof BatteryState) {
            instanceLogger.error("BatteryState unsupported");
        } else if (state instanceof ChannelState) {
            channelMapper.apply(componentMapper.mapChannelState((ChannelState) state));
        } else if (state instanceof ClockState) {
            instanceLogger.error("ClockState unsupported");
        } else if (state instanceof ScoState) {
            scoMapper.apply(componentMapper.mapScoState((ScoState) state));
        } else if (state instanceof SystemContextState) {
            systemContextMapper.apply(componentMapper.mapSystemContextState((SystemContextState) state));
        } else {
            instanceLogger.error("Class {} not supported", state.getClass());
        }
    }

    public <T> void buildAbstractMetricState(
            AbstractMetricState state,
            Function<DistributionSampleArrayMetricStateMsg, T> distributionSampleMapper,
            Function<NumericMetricStateMsg, T> numericMetricMapper,
            Function<RealTimeSampleArrayMetricStateMsg, T> realTimeSampleMapper,
            Function<EnumStringMetricStateMsg, T> enumStringMetricMapper,
            Function<StringMetricStateMsg, T> stringMetricMapper
    ) {
        if (state instanceof DistributionSampleArrayMetricState) {
            instanceLogger.error("DistributionSampleArrayMetricState unsupported");
        } else if (state instanceof NumericMetricState) {
            numericMetricMapper.apply(metricMapper.mapNumericMetricState((NumericMetricState) state));
        } else if (state instanceof RealTimeSampleArrayMetricState) {
            realTimeSampleMapper.apply(metricMapper.mapRealTimeSampleArrayMetricState((RealTimeSampleArrayMetricState) state));
        } else if (state instanceof EnumStringMetricState) {
            enumStringMetricMapper.apply(metricMapper.mapEnumStringMetricState((EnumStringMetricState) state));
        } else if (state instanceof StringMetricState) {
            stringMetricMapper.apply(metricMapper.mapStringMetricState((StringMetricState) state));
        } else {
            instanceLogger.error("Class {} not supported", state.getClass());
        }
    }

    public <T> void buildAbstractContextState(
            AbstractContextState state,
            Function<EnsembleContextStateMsg, T> ensembleMapper,
            Function<LocationContextStateMsg, T> locationMapper,
            Function<MeansContextStateMsg, T> meansMapper,
            Function<OperatorContextStateMsg, T> operatorMapper,
            Function<PatientContextStateMsg, T> patientMapper,
            Function<WorkflowContextStateMsg, T> workflowMapper
    ) {
        if (state instanceof EnsembleContextState) {
            ensembleMapper.apply(contextMapper.mapEnsembleContextState((EnsembleContextState) state).build());
        } else if (state instanceof LocationContextState) {
            locationMapper.apply(contextMapper.mapLocationContextState((LocationContextState) state).build());
        } else if (state instanceof MeansContextState) {
            instanceLogger.error("MeansContextState unsupported");
        } else if (state instanceof OperatorContextState) {
            instanceLogger.error("OperatorContextState unsupported");
        } else if (state instanceof PatientContextState) {
            patientMapper.apply(contextMapper.mapPatientContextState((PatientContextState) state).build());
        } else if (state instanceof WorkflowContextState) {
            instanceLogger.error("WorkflowContextState unsupported");
        } else {
            instanceLogger.error("Cannot handle unknown type " + state);
        }
    }

    public <T> void buildAbstractAlertState(
            AbstractAlertState state,
            Function<LimitAlertConditionStateMsg, T> limitConditionMapper,
            Function<AlertConditionStateMsg, T> conditionMapper,
            Function<AlertSignalStateMsg, T> signalMapper,
            Function<AlertSystemStateMsg, T> systemMapper
    ) {
        if (state instanceof LimitAlertConditionState) {
            limitConditionMapper.apply(alertMapper.mapLimitAlertConditionState((LimitAlertConditionState) state));
        } else if (state instanceof AlertConditionState) {
            conditionMapper.apply(alertMapper.mapAlertConditionState((AlertConditionState) state));
        } else if (state instanceof AlertSignalState) {
            signalMapper.apply(alertMapper.mapAlertSignalState((AlertSignalState) state));
        } else if (state instanceof AlertSystemState) {
            systemMapper.apply(alertMapper.mapAlertSystemState((AlertSystemState) state));
        } else {
            instanceLogger.error("Class {} not supported", state.getClass());
        }
    }

    public <T> void buildAbstractOperationState(
            AbstractOperationState state,
            Function<ActivateOperationStateMsg, T> activateMapper,
            Function<SetAlertStateOperationStateMsg, T> setAlertStateMapper,
            Function<SetComponentStateOperationStateMsg, T> setComponentStateMapper,
            Function<SetContextStateOperationStateMsg, T> setContextStateMapper,
            Function<SetMetricStateOperationStateMsg, T> setMetricStateMapper,
            Function<SetStringOperationStateMsg, T> setStringMapper,
            Function<SetValueOperationStateMsg, T> setValueMapper
    ) {
        if (state instanceof ActivateOperationState) {
            activateMapper.apply(operationMapper.mapActivateOperationState((ActivateOperationState) state));
        } else if (state instanceof SetAlertStateOperationState) {
            setAlertStateMapper.apply(operationMapper.mapSetAlertStateOperationState((SetAlertStateOperationState) state));
        } else if (state instanceof SetComponentStateOperationState) {
            setComponentStateMapper.apply(operationMapper.mapSetComponentStateOperationState((SetComponentStateOperationState) state));
        } else if (state instanceof SetContextStateOperationState) {
            setContextStateMapper.apply(operationMapper.mapSetContextStateOperationState((SetContextStateOperationState) state));
        } else if (state instanceof SetMetricStateOperationState) {
            setMetricStateMapper.apply(operationMapper.mapSetMetricStateOperationState((SetMetricStateOperationState) state));
        } else if (state instanceof SetStringOperationState) {
            setStringMapper.apply(operationMapper.mapSetStringOperationState((SetStringOperationState) state));
        } else if (state instanceof SetValueOperationState) {
            setValueMapper.apply(operationMapper.mapSetValueOperationState((SetValueOperationState) state));
        } else {
            instanceLogger.error("Class {} not supported", state.getClass());
        }
    }

    public AbstractStateOneOfMsg mapAbstractStateOneOf(AbstractState state) {
        var builder = AbstractStateOneOfMsg.newBuilder();
        if (state instanceof AbstractComplexDeviceComponentState) {
            buildAbstractComplexDeviceComponentState(
                    (AbstractComplexDeviceComponentState) state,
                    builder::setMdsState,
                    builder::setVmdState
            );
        } else if (state instanceof AbstractDeviceComponentState) {
            buildAbstractDeviceComponentState(
                    (AbstractDeviceComponentState) state,
                    builder::setBatteryState,
                    builder::setChannelState,
                    builder::setClockState,
                    builder::setScoState,
                    builder::setSystemContextState
            );
        } else if (state instanceof AbstractMetricState) {
            buildAbstractMetricState(
                    (AbstractMetricState) state,
                    builder::setDistributionSampleArrayMetricState,
                    builder::setNumericMetricState,
                    builder::setRealTimeSampleArrayMetricState,
                    builder::setEnumStringMetricState,
                    builder::setStringMetricState
            );
        } else if (state instanceof AbstractMultiState) {
            if (state instanceof AbstractContextState) {
                buildAbstractContextState(
                        (AbstractContextState) state,
                        builder::setEnsembleContextState,
                        builder::setLocationContextState,
                        builder::setMeansContextState,
                        builder::setOperatorContextState,
                        builder::setPatientContextState,
                        builder::setWorkflowContextState
                );
            } else {
                instanceLogger.error("Class {} not supported", state.getClass());
            }
        } else if (state instanceof AbstractAlertState) {
            buildAbstractAlertState(
                    (AbstractAlertState) state,
                    builder::setLimitAlertConditionState,
                    builder::setAlertConditionState,
                    builder::setAlertSignalState,
                    builder::setAlertSystemState
            );
        } else if (state instanceof AbstractOperationState) {
            buildAbstractOperationState(
                    (AbstractOperationState) state,
                    builder::setActivateOperationState,
                    builder::setSetAlertStateOperationState,
                    builder::setSetComponentStateOperationState,
                    builder::setSetContextStateOperationState,
                    builder::setSetMetricStateOperationState,
                    builder::setSetStringOperationState,
                    builder::setSetValueOperationState
            );
        } else {
            instanceLogger.error("Class {} not supported", state.getClass());
        }

        return builder.build();
    }

    public <T> void buildAbstractComplexDeviceComponentDescriptor(
            AbstractComplexDeviceComponentDescriptor descriptor,
            Function<MdsDescriptorMsg, T> mdsMapper,
            Function<VmdDescriptorMsg, T> vmdMapper
    ) {
        if (descriptor instanceof MdsDescriptor) {
            mdsMapper.apply(componentMapper.mapMdsDescriptor((MdsDescriptor) descriptor).build());
        } else if (descriptor instanceof VmdDescriptor) {
            vmdMapper.apply(componentMapper.mapVmdDescriptor((VmdDescriptor) descriptor).build());
        } else {
            instanceLogger.error("Class {} not supported", descriptor.getClass());
        }
    }

    public <T> void buildAbstractDeviceComponentDescriptor(
            AbstractDeviceComponentDescriptor descriptor,
            Function<BatteryDescriptorMsg, T> batteryMapper,
            Function<ChannelDescriptorMsg, T> channelMapper,
            Function<ClockDescriptorMsg, T> clockMapper,
            Function<ScoDescriptorMsg, T> scoMapper,
            Function<SystemContextDescriptorMsg, T> systemContextMapper
    ) {
        if (descriptor instanceof BatteryDescriptor) {
            instanceLogger.error("BatteryDescriptor unsupported");
        } else if (descriptor instanceof ChannelDescriptor) {
            channelMapper.apply(componentMapper.mapChannelDescriptor((ChannelDescriptor) descriptor).build());
        } else if (descriptor instanceof ClockDescriptor) {
            instanceLogger.error("ClockDescriptor unsupported");
        } else if (descriptor instanceof ScoDescriptor) {
            scoMapper.apply(componentMapper.mapScoDescriptor((ScoDescriptor) descriptor).build());
        } else if (descriptor instanceof SystemContextDescriptor) {
            systemContextMapper.apply(componentMapper.mapSystemContextDescriptor((SystemContextDescriptor) descriptor).build());
        } else {
            instanceLogger.error("Class {} not supported", descriptor.getClass());
        }
    }

    public <T> void buildAbstractMetricDescriptor(
            AbstractMetricDescriptor descriptor,
            Function<DistributionSampleArrayMetricDescriptorMsg, T> distributionSampleMapper,
            Function<NumericMetricDescriptorMsg, T> numericMetricMapper,
            Function<RealTimeSampleArrayMetricDescriptorMsg, T> realTimeSampleMapper,
            Function<EnumStringMetricDescriptorMsg, T> enumStringMetricMapper,
            Function<StringMetricDescriptorMsg, T> stringMetricMapper
    ) {
        if (descriptor instanceof DistributionSampleArrayMetricDescriptor) {
            instanceLogger.error("DistributionSampleArrayMetricDescriptor unsupported");
        } else if (descriptor instanceof NumericMetricDescriptor) {
            numericMetricMapper.apply(metricMapper.mapNumericMetricDescriptor((NumericMetricDescriptor) descriptor).build());
        } else if (descriptor instanceof RealTimeSampleArrayMetricDescriptor) {
            realTimeSampleMapper.apply(metricMapper.mapRealTimeSampleArrayMetricDescriptor((RealTimeSampleArrayMetricDescriptor) descriptor).build());
        } else if (descriptor instanceof EnumStringMetricDescriptor) {
            enumStringMetricMapper.apply(metricMapper.mapEnumStringMetricDescriptor((EnumStringMetricDescriptor) descriptor).build());
        } else if (descriptor instanceof StringMetricDescriptor) {
            stringMetricMapper.apply(metricMapper.mapStringMetricDescriptor((StringMetricDescriptor) descriptor).build());
        } else {
            instanceLogger.error("Class {} not supported", descriptor.getClass());
        }
    }

    public <T> void buildAbstractContextDescriptor(
            AbstractContextDescriptor descriptor,
            Function<EnsembleContextDescriptorMsg, T> ensembleMapper,
            Function<LocationContextDescriptorMsg, T> locationMapper,
            Function<MeansContextDescriptorMsg, T> meansMapper,
            Function<OperatorContextDescriptorMsg, T> operatorMapper,
            Function<PatientContextDescriptorMsg, T> patientMapper,
            Function<WorkflowContextDescriptorMsg, T> workflowMapper
    ) {
        if (descriptor instanceof EnsembleContextDescriptor) {
            ensembleMapper.apply(contextMapper.mapEnsembleContextDescriptor((EnsembleContextDescriptor) descriptor).build());
        } else if (descriptor instanceof LocationContextDescriptor) {
            locationMapper.apply(contextMapper.mapLocationContextDescriptor((LocationContextDescriptor) descriptor).build());
        } else if (descriptor instanceof MeansContextDescriptor) {
            instanceLogger.error("MeansContextDescriptor unsupported");
        } else if (descriptor instanceof OperatorContextDescriptor) {
            instanceLogger.error("OperatorContextDescriptor unsupported");
        } else if (descriptor instanceof PatientContextDescriptor) {
            patientMapper.apply(contextMapper.mapPatientContextDescriptor((PatientContextDescriptor) descriptor).build());
        } else if (descriptor instanceof WorkflowContextDescriptor) {
            instanceLogger.error("WorkflowContextState unsupported");
        } else {
            instanceLogger.error("Cannot handle unknown type " + descriptor);
        }
    }

    public <T> void buildAbstractAlertDescriptor(
            AbstractAlertDescriptor descriptor,
            Function<AlertConditionDescriptorMsg, T> conditionMapper,
            Function<AlertSignalDescriptorMsg, T> signalMapper,
            Function<AlertSystemDescriptorMsg, T> systemMapper
    ) {
        if (descriptor instanceof AlertConditionDescriptor) {
            conditionMapper.apply(alertMapper.mapAlertConditionDescriptor((AlertConditionDescriptor) descriptor));
        } else if (descriptor instanceof AlertSignalDescriptor) {
            signalMapper.apply(alertMapper.mapAlertSignalDescriptor((AlertSignalDescriptor) descriptor));
        } else if (descriptor instanceof AlertSystemDescriptor) {
            systemMapper.apply(alertMapper.mapAlertSystemDescriptor((AlertSystemDescriptor) descriptor).build());
        } else {
            instanceLogger.error("Class {} not supported", descriptor.getClass());
        }
    }

    public <T> void buildAbstractOperationDescriptor(
            AbstractOperationDescriptor descriptor,
            Function<ActivateOperationDescriptorMsg, T> activateMapper,
            Function<SetAlertStateOperationDescriptorMsg, T> setAlertStateMapper,
            Function<SetComponentStateOperationDescriptorMsg, T> setComponentStateMapper,
            Function<SetContextStateOperationDescriptorMsg, T> setContextStateMapper,
            Function<SetMetricStateOperationDescriptorMsg, T> setMetricStateMapper,
            Function<SetStringOperationDescriptorMsg, T> setStringMapper,
            Function<SetValueOperationDescriptorMsg, T> setValueMapper
    ) {
        if (descriptor instanceof ActivateOperationDescriptor) {
            activateMapper.apply(operationMapper.mapActivateOperationDescriptor((ActivateOperationDescriptor) descriptor));
        } else if (descriptor instanceof SetAlertStateOperationDescriptor) {
            setAlertStateMapper.apply(operationMapper.mapSetAlertStateOperationDescriptor((SetAlertStateOperationDescriptor) descriptor));
        } else if (descriptor instanceof SetComponentStateOperationDescriptor) {
            setComponentStateMapper.apply(operationMapper.mapSetComponentStateOperationDescriptor((SetComponentStateOperationDescriptor) descriptor));
        } else if (descriptor instanceof SetContextStateOperationDescriptor) {
            setContextStateMapper.apply(operationMapper.mapSetContextStateOperationDescriptor((SetContextStateOperationDescriptor) descriptor));
        } else if (descriptor instanceof SetMetricStateOperationDescriptor) {
            setMetricStateMapper.apply(operationMapper.mapSetMetricStateOperationDescriptor((SetMetricStateOperationDescriptor) descriptor));
        } else if (descriptor instanceof SetStringOperationDescriptor) {
            setStringMapper.apply(operationMapper.mapSetStringOperationDescriptor((SetStringOperationDescriptor) descriptor));
        } else if (descriptor instanceof SetValueOperationDescriptor) {
            setValueMapper.apply(operationMapper.mapSetValueOperationDescriptor((SetValueOperationDescriptor) descriptor));
        } else {
            instanceLogger.error("Class {} not supported", descriptor.getClass());
        }
    }

    public AbstractDescriptorOneOfMsg mapAbstractDescriptor(AbstractDescriptor descriptor) {
        var builder = AbstractDescriptorOneOfMsg.newBuilder();
        if (descriptor instanceof AbstractComplexDeviceComponentDescriptor) {
            buildAbstractComplexDeviceComponentDescriptor(
                    (AbstractComplexDeviceComponentDescriptor) descriptor,
                    builder::setMdsDescriptor,
                    builder::setVmdDescriptor
            );
        } else if (descriptor instanceof AbstractDeviceComponentDescriptor) {
            buildAbstractDeviceComponentDescriptor(
                    (AbstractDeviceComponentDescriptor) descriptor,
                    builder::setBatteryDescriptor,
                    builder::setChannelDescriptor,
                    builder::setClockDescriptor,
                    builder::setScoDescriptor,
                    builder::setSystemContextDescriptor
            );
        } else if (descriptor instanceof AbstractMetricDescriptor) {
            buildAbstractMetricDescriptor(
                    (AbstractMetricDescriptor) descriptor,
                    builder::setDistributionSampleArrayMetricDescriptor,
                    builder::setNumericMetricDescriptor,
                    builder::setRealTimeSampleArrayMetricDescriptor,
                    builder::setEnumStringMetricDescriptor,
                    builder::setStringMetricDescriptor
            );
        } else if (descriptor instanceof AbstractContextDescriptor) {
            buildAbstractContextDescriptor(
                    (AbstractContextDescriptor) descriptor,
                    builder::setEnsembleContextDescriptor,
                    builder::setLocationContextDescriptor,
                    builder::setMeansContextDescriptor,
                    builder::setOperatorContextDescriptor,
                    builder::setPatientContextDescriptor,
                    builder::setWorkflowContextDescriptor
            );
        } else if (descriptor instanceof AbstractAlertDescriptor) {
            buildAbstractAlertDescriptor(
                    (AbstractAlertDescriptor) descriptor,
                    builder::setAlertConditionDescriptor,
                    builder::setAlertSignalDescriptor,
                    builder::setAlertSystemDescriptor
            );
        } else if (descriptor instanceof AbstractOperationDescriptor) {
            buildAbstractOperationDescriptor(
                    (AbstractOperationDescriptor) descriptor,
                    builder::setActivateOperationDescriptor,
                    builder::setSetAlertStateOperationDescriptor,
                    builder::setSetComponentStateOperationDescriptor,
                    builder::setSetContextStateOperationDescriptor,
                    builder::setSetMetricStateOperationDescriptor,
                    builder::setSetStringOperationDescriptor,
                    builder::setSetValueOperationDescriptor
            );
        } else {
            instanceLogger.error("Class {} not supported", descriptor.getClass());
        }
        return builder.build();
    }

    public AbstractOperationDescriptorOneOfMsg mapAbstractOperationDescriptor(AbstractOperationDescriptor descriptor) {
        var builder = AbstractOperationDescriptorOneOfMsg.newBuilder();
        buildAbstractOperationDescriptor(
                descriptor,
                builder::setActivateOperationDescriptor,
                builder::setSetAlertStateOperationDescriptor,
                builder::setSetComponentStateOperationDescriptor,
                builder::setSetContextStateOperationDescriptor,
                builder::setSetMetricStateOperationDescriptor,
                builder::setSetStringOperationDescriptor,
                builder::setSetValueOperationDescriptor
        );
        return builder.build();
    }

    public AbstractContextDescriptorOneOfMsg mapAbstractContextDescriptor(AbstractContextDescriptor descriptor) {
        var builder = AbstractContextDescriptorOneOfMsg.newBuilder();
        buildAbstractContextDescriptor(
                descriptor,
                builder::setEnsembleContextDescriptor,
                builder::setLocationContextDescriptor,
                builder::setMeansContextDescriptor,
                builder::setOperatorContextDescriptor,
                builder::setPatientContextDescriptor,
                builder::setWorkflowContextDescriptor
        );
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
        buildAbstractAlertDescriptor(
                descriptor,
                builder::setAlertConditionDescriptor,
                builder::setAlertSignalDescriptor,
                builder::setAlertSystemDescriptor
        );
        return builder.build();
    }

    public AlertConditionDescriptorOneOfMsg mapAlertConditionDescriptor(AlertConditionDescriptor descriptor) {
        var builder = AlertConditionDescriptorOneOfMsg.newBuilder();
        if (descriptor instanceof LimitAlertConditionDescriptor) {
            builder.setLimitAlertConditionDescriptor(
                    alertMapper.mapLimitAlertConditionDescriptor((LimitAlertConditionDescriptor) descriptor)
            );
        } else {
            builder.setAlertConditionDescriptor(alertMapper.mapAlertConditionDescriptor(descriptor));
        }
        return builder.build();
    }

    public AbstractMetricDescriptorOneOfMsg mapAbstractMetricDescriptor(AbstractMetricDescriptor descriptor) {
        var builder = AbstractMetricDescriptorOneOfMsg.newBuilder();
        buildAbstractMetricDescriptor(
                descriptor,
                builder::setDistributionSampleArrayMetricDescriptor,
                builder::setNumericMetricDescriptor,
                builder::setRealTimeSampleArrayMetricDescriptor,
                builder::setEnumStringMetricDescriptor,
                builder::setStringMetricDescriptor
        );
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
        buildAbstractDeviceComponentDescriptor(
                descriptor,
                builder::setBatteryDescriptor,
                builder::setChannelDescriptor,
                builder::setClockDescriptor,
                builder::setScoDescriptor,
                builder::setSystemContextDescriptor
        );
        return builder.build();
    }

    private AbstractComplexDeviceComponentDescriptorOneOfMsg mapAbstractComplexDeviceComponentDescriptor(
            AbstractComplexDeviceComponentDescriptor descriptor
    ) {
        var builder = AbstractComplexDeviceComponentDescriptorOneOfMsg.newBuilder();
        buildAbstractComplexDeviceComponentDescriptor(
                descriptor,
                builder::setMdsDescriptor,
                builder::setVmdDescriptor
        );
        return builder.build();
    }

    public AbstractMetricStateOneOfMsg mapAbstractMetricStateOneOf(AbstractMetricState state) {
        var builder = AbstractMetricStateOneOfMsg.newBuilder();
        buildAbstractMetricState(
                state,
                builder::setDistributionSampleArrayMetricState,
                builder::setNumericMetricState,
                builder::setRealTimeSampleArrayMetricState,
                builder::setEnumStringMetricState,
                builder::setStringMetricState
        );
        return builder.build();
    }

    // TODO: huh?
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
        buildAbstractOperationState(
                state,
                builder::setActivateOperationState,
                builder::setSetAlertStateOperationState,
                builder::setSetComponentStateOperationState,
                builder::setSetContextStateOperationState,
                builder::setSetMetricStateOperationState,
                builder::setSetStringOperationState,
                builder::setSetValueOperationState
        );
        return builder.build();
    }


    public AbstractAlertStateOneOfMsg mapAbstractAlertStateOneOf(AbstractAlertState state) {
        var builder = AbstractAlertStateOneOfMsg.newBuilder();
        buildAbstractAlertState(
                state,
                builder::setLimitAlertConditionState,
                builder::setAlertConditionState,
                builder::setAlertSignalState,
                builder::setAlertSystemState
        );
        return builder.build();
    }

    public AlertConditionStateOneOfMsg mapAlertConditionStateOneOf(AlertConditionState state) {
        var builder = AlertConditionStateOneOfMsg.newBuilder();
        if (state instanceof LimitAlertConditionState) {
            builder.setLimitAlertConditionState(
                    alertMapper.mapLimitAlertConditionState((LimitAlertConditionState) state)
            );
        } else {
            builder.setAlertConditionState(alertMapper.mapAlertConditionState(state));
        }
        return builder.build();
    }

    public AbstractMultiStateOneOfMsg mapAbstractMultiStateOneOf(AbstractMultiState state) {
        var builder = AbstractMultiStateOneOfMsg.newBuilder();
        if (state instanceof AbstractContextState) {
            buildAbstractContextState(
                    (AbstractContextState) state,
                    builder::setEnsembleContextState,
                    builder::setLocationContextState,
                    builder::setMeansContextState,
                    builder::setOperatorContextState,
                    builder::setPatientContextState,
                    builder::setWorkflowContextState
            );
        } else {
            instanceLogger.error("Class {} not supported", state.getClass());
        }
        return builder.build();
    }

    public AbstractContextStateOneOfMsg mapAbstractContextStateOneOf(AbstractContextState state) {
        var builder = AbstractContextStateOneOfMsg.newBuilder();
        buildAbstractContextState(
                state,
                builder::setEnsembleContextState,
                builder::setLocationContextState,
                builder::setMeansContextState,
                builder::setOperatorContextState,
                builder::setPatientContextState,
                builder::setWorkflowContextState
        );
        return builder.build();
    }

    public AbstractDeviceComponentStateOneOfMsg mapAbstractDeviceComponentStateOneOf(
            AbstractDeviceComponentState state) {
        var builder = AbstractDeviceComponentStateOneOfMsg.newBuilder();
        buildAbstractDeviceComponentState(
                state,
                builder::setBatteryState,
                builder::setChannelState,
                builder::setClockState,
                builder::setScoState,
                builder::setSystemContextState
        );
        return builder.build();
    }

    public AbstractComplexDeviceComponentStateOneOfMsg mapAbstractComplexDeviceComponentStateOneOf(
            AbstractComplexDeviceComponentState state) {
        var builder = AbstractComplexDeviceComponentStateOneOfMsg.newBuilder();
        buildAbstractComplexDeviceComponentState(
                state,
                builder::setMdsState,
                builder::setVmdState
        );
        return builder.build();
    }

    public PersonReferenceOneOfMsg mapPersonReferenceOneOf(PersonReference personReference) {
        var builder = PersonReferenceOneOfMsg.newBuilder();
        if (personReference instanceof PersonParticipation) {
            builder.setPersonParticipation(contextMapper.mapPersonParticipation((PersonParticipation) personReference));
        } else {
            builder.setPersonReference(contextMapper.mapPersonReference(personReference));
        }
        return builder.build();
    }

    public InstanceIdentifierOneOfMsg mapInstanceIdentifier(InstanceIdentifier instanceIdentifier) {
        var builder = InstanceIdentifierOneOfMsg.newBuilder();

        if (instanceIdentifier instanceof OperatingJurisdiction) {
//            builder.setOperatingJurisdiction()
            instanceLogger.error("Class {} not supported", instanceIdentifier.getClass().getSimpleName());
        } else {
            builder.setInstanceIdentifier(baseMapper.mapInstanceIdentifier(instanceIdentifier));
        }
        return builder.build();
    }

    public BaseDemographicsOneOfMsg mapBaseDemographics(BaseDemographics baseDemographics) {
        var builder = BaseDemographicsOneOfMsg.newBuilder();

        if (baseDemographics instanceof NeonatalPatientDemographicsCoreData) {
            builder.setNeonatalPatientDemographicsCoreData(
                    contextMapper.mapNeonatalPatientDemographicsCoreData((NeonatalPatientDemographicsCoreData) baseDemographics)
            );
        } else if (baseDemographics instanceof PatientDemographicsCoreData) {
            builder.setPatientDemographicsCoreData(
                    contextMapper.mapPatientDemographicsCoreData((PatientDemographicsCoreData) baseDemographics)
            );
        } else {
            builder.setBaseDemographics(baseMapper.mapBaseDemographics(baseDemographics));
        }
        return builder.build();
    }
}
