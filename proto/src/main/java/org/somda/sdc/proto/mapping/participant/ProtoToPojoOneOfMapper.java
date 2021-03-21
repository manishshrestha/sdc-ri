package org.somda.sdc.proto.mapping.participant;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.MessageOrBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.common.util.TimestampAdapter;
import org.somda.sdc.proto.mapping.Util;
import org.somda.protosdc.proto.model.biceps.*;

public class ProtoToPojoOneOfMapper {
    private static final Logger LOG = LogManager.getLogger(ProtoToPojoOneOfMapper.class);

    private final ProtoToPojoBaseMapper baseMapper;
    private final ProtoToPojoAlertMapper alertMapper;
    private final ProtoToPojoComponentMapper componentMapper;
    private final ProtoToPojoContextMapper contextMapper;
    private final ProtoToPojoMetricMapper metricMapper;
    private final ProtoToPojoOperationMapper operationMapper;
    private final TimestampAdapter timestampAdapter;
    private final Logger instanceLogger;

    @Inject
    ProtoToPojoOneOfMapper(@Named(org.somda.sdc.common.CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier,
                           ProtoToPojoBaseMapper baseMapper,
                           ProtoToPojoAlertMapper alertMapper,
                           ProtoToPojoComponentMapper componentMapper,
                           ProtoToPojoContextMapper contextMapper,
                           ProtoToPojoMetricMapper metricMapper,
                           ProtoToPojoOperationMapper operationMapper,
                           TimestampAdapter timestampAdapter) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.baseMapper = baseMapper;
        this.alertMapper = alertMapper;
        this.componentMapper = componentMapper;
        this.contextMapper = contextMapper;
        this.metricMapper = metricMapper;
        this.operationMapper = operationMapper;
        this.timestampAdapter = timestampAdapter;
    }

    public AbstractDescriptor mapDescriptor(MessageOrBuilder protoMsg) {
        if (protoMsg instanceof MdsDescriptorMsg) {
            return componentMapper.map((MdsDescriptorMsg) protoMsg);
        } else if (protoMsg instanceof VmdDescriptorMsg) {
            return componentMapper.map((VmdDescriptorMsg) protoMsg);
        } else if (protoMsg instanceof ChannelDescriptorMsg) {
            return componentMapper.map((ChannelDescriptorMsg) protoMsg);
        } else if (protoMsg instanceof EnumStringMetricDescriptorMsg) {
            return metricMapper.map((EnumStringMetricDescriptorMsg) protoMsg);
        } else if (protoMsg instanceof StringMetricDescriptorMsg) {
            return metricMapper.map((StringMetricDescriptorMsg) protoMsg);
        } else if (protoMsg instanceof NumericMetricDescriptorMsg) {
            return metricMapper.map((NumericMetricDescriptorMsg) protoMsg);
        } else if (protoMsg instanceof RealTimeSampleArrayMetricDescriptorMsg) {
            return metricMapper.map((RealTimeSampleArrayMetricDescriptorMsg) protoMsg);
        } else if (protoMsg instanceof SystemContextDescriptorMsg) {
            return componentMapper.map((SystemContextDescriptorMsg) protoMsg);
        } else if (protoMsg instanceof EnsembleContextDescriptorMsg) {
            return contextMapper.map((EnsembleContextDescriptorMsg) protoMsg);
        } else if (protoMsg instanceof LocationContextDescriptorMsg) {
            return contextMapper.map((LocationContextDescriptorMsg) protoMsg);
        } else if (protoMsg instanceof PatientContextDescriptorMsg) {
            return contextMapper.map((PatientContextDescriptorMsg) protoMsg);
        } else if (protoMsg instanceof AlertSystemDescriptorMsg) {
            return alertMapper.map((AlertSystemDescriptorMsg) protoMsg);
        } else if (protoMsg instanceof ScoDescriptorMsg) {
            return componentMapper.map((ScoDescriptorMsg) protoMsg);
        } else if (protoMsg instanceof ActivateOperationDescriptorMsg) {
            return operationMapper.map((ActivateOperationDescriptorMsg) protoMsg);
        } else if (protoMsg instanceof SetStringOperationDescriptorMsg) {
            return operationMapper.map((SetStringOperationDescriptorMsg) protoMsg);
        } else if (protoMsg instanceof SetValueOperationDescriptorMsg) {
            return operationMapper.map((SetValueOperationDescriptorMsg) protoMsg);
        } else if (protoMsg instanceof SetMetricStateOperationDescriptorMsg) {
            return operationMapper.map((SetMetricStateOperationDescriptorMsg) protoMsg);
        } else if (protoMsg instanceof SetComponentStateOperationDescriptorMsg) {
            return operationMapper.map((SetComponentStateOperationDescriptorMsg) protoMsg);
        } else if (protoMsg instanceof SetContextStateOperationDescriptorMsg) {
            return operationMapper.map((SetContextStateOperationDescriptorMsg) protoMsg);
        } else if (protoMsg instanceof SetAlertStateOperationDescriptorMsg) {
            return operationMapper.map((SetAlertStateOperationDescriptorMsg) protoMsg);
        } else if (protoMsg instanceof AlertConditionDescriptorMsg) {
            return alertMapper.map((AlertConditionDescriptorMsg) protoMsg);
        } else if (protoMsg instanceof AlertSignalDescriptorMsg) {
            return alertMapper.map((AlertSignalDescriptorMsg) protoMsg);
        } else if (protoMsg instanceof LimitAlertConditionDescriptorMsg) {
            return alertMapper.map((LimitAlertConditionDescriptorMsg) protoMsg);
        } else {
            instanceLogger.error("Descriptor mapping not implemented: {}", protoMsg);
            return Util.invalidDescriptor();
        }
    }

    public AbstractState mapState(MessageOrBuilder protoMsg) {
        if (protoMsg instanceof MdsStateMsg) {
            return componentMapper.map((MdsStateMsg) protoMsg);
        } else if (protoMsg instanceof VmdStateMsg) {
            return componentMapper.map((VmdStateMsg) protoMsg);
        } else if (protoMsg instanceof ChannelStateMsg) {
            return componentMapper.map((ChannelStateMsg) protoMsg);
        } else if (protoMsg instanceof SystemContextStateMsg) {
            return componentMapper.map((SystemContextStateMsg) protoMsg);
        } else {
            instanceLogger.error("Descriptor mapping not implemented: {}", protoMsg);
            return Util.invalidState();
        }
    }

    public AbstractDescriptor map(AbstractDescriptorOneOfMsg protoMsg) {
        var type = protoMsg.getAbstractDescriptorOneOfCase();
        switch (type) {
            case MDS_DESCRIPTOR:
                return componentMapper.map(protoMsg.getMdsDescriptor());
            case VMD_DESCRIPTOR:
                return componentMapper.map(protoMsg.getVmdDescriptor());
            case CHANNEL_DESCRIPTOR:
                return componentMapper.map(protoMsg.getChannelDescriptor());
            case SCO_DESCRIPTOR:
                return componentMapper.map(protoMsg.getScoDescriptor());
            case SYSTEM_CONTEXT_DESCRIPTOR:
                return componentMapper.map(protoMsg.getSystemContextDescriptor());
            case NUMERIC_METRIC_DESCRIPTOR:
                return metricMapper.map(protoMsg.getNumericMetricDescriptor());
            case REAL_TIME_SAMPLE_ARRAY_METRIC_DESCRIPTOR:
                return metricMapper.map(protoMsg.getRealTimeSampleArrayMetricDescriptor());
            case ENUM_STRING_METRIC_DESCRIPTOR:
                return metricMapper.map(protoMsg.getEnumStringMetricDescriptor());
            case STRING_METRIC_DESCRIPTOR:
                return metricMapper.map(protoMsg.getStringMetricDescriptor());
            case ENSEMBLE_CONTEXT_DESCRIPTOR:
                return contextMapper.map(protoMsg.getEnsembleContextDescriptor());
            case LOCATION_CONTEXT_DESCRIPTOR:
                return contextMapper.map(protoMsg.getLocationContextDescriptor());
            case PATIENT_CONTEXT_DESCRIPTOR:
                return contextMapper.map(protoMsg.getPatientContextDescriptor());
            case LIMIT_ALERT_CONDITION_DESCRIPTOR:
                return alertMapper.map(protoMsg.getLimitAlertConditionDescriptor());
            case ALERT_CONDITION_DESCRIPTOR:
                return alertMapper.map(protoMsg.getAlertConditionDescriptor());
            case ALERT_SIGNAL_DESCRIPTOR:
                return alertMapper.map(protoMsg.getAlertSignalDescriptor());
            case ALERT_SYSTEM_DESCRIPTOR:
                return alertMapper.map(protoMsg.getAlertSystemDescriptor());
            case ACTIVATE_OPERATION_DESCRIPTOR:
                return operationMapper.map(protoMsg.getActivateOperationDescriptor());
            case SET_ALERT_STATE_OPERATION_DESCRIPTOR:
                return operationMapper.map(protoMsg.getSetAlertStateOperationDescriptor());
            case SET_COMPONENT_STATE_OPERATION_DESCRIPTOR:
                return operationMapper.map(protoMsg.getSetComponentStateOperationDescriptor());
            case SET_CONTEXT_STATE_OPERATION_DESCRIPTOR:
                return operationMapper.map(protoMsg.getSetContextStateOperationDescriptor());
            case SET_METRIC_STATE_OPERATION_DESCRIPTOR:
                return operationMapper.map(protoMsg.getSetMetricStateOperationDescriptor());
            case SET_STRING_OPERATION_DESCRIPTOR:
                return operationMapper.map(protoMsg.getSetStringOperationDescriptor());
            case SET_VALUE_OPERATION_DESCRIPTOR:
                return operationMapper.map(protoMsg.getSetValueOperationDescriptor());
            case BATTERY_DESCRIPTOR:
            case CLOCK_DESCRIPTOR:
            case DISTRIBUTION_SAMPLE_ARRAY_METRIC_DESCRIPTOR:
            case MEANS_CONTEXT_DESCRIPTOR:
            case OPERATOR_CONTEXT_DESCRIPTOR:
            case WORKFLOW_CONTEXT_DESCRIPTOR:
            case ABSTRACT_DESCRIPTOR:
            case ABSTRACT_COMPLEX_DEVICE_COMPONENT_DESCRIPTOR:
            case ABSTRACT_OPERATION_DESCRIPTOR:
            case ABSTRACT_DEVICE_COMPONENT_DESCRIPTOR:
            case ABSTRACT_SET_STATE_OPERATION_DESCRIPTOR:
            case ABSTRACT_ALERT_DESCRIPTOR:
            case ABSTRACT_CONTEXT_DESCRIPTOR:
            case ABSTRACT_METRIC_DESCRIPTOR:
            case ABSTRACTDESCRIPTORONEOF_NOT_SET:
            default:
                instanceLogger.error("Descriptor mapping not implemented: {}", type);
                break;
        }

        return Util.invalidDescriptor();
    }

    public AbstractOperationDescriptor map(AbstractOperationDescriptorOneOfMsg protoMsg) {
        var type = protoMsg.getAbstractOperationDescriptorOneOfCase();
        switch (type) {
            case ACTIVATE_OPERATION_DESCRIPTOR:
                return operationMapper.map(protoMsg.getActivateOperationDescriptor());
            case SET_ALERT_STATE_OPERATION_DESCRIPTOR:
                return operationMapper.map(protoMsg.getSetAlertStateOperationDescriptor());
            case SET_COMPONENT_STATE_OPERATION_DESCRIPTOR:
                return operationMapper.map(protoMsg.getSetComponentStateOperationDescriptor());
            case SET_CONTEXT_STATE_OPERATION_DESCRIPTOR:
                return operationMapper.map(protoMsg.getSetContextStateOperationDescriptor());
            case SET_METRIC_STATE_OPERATION_DESCRIPTOR:
                return operationMapper.map(protoMsg.getSetMetricStateOperationDescriptor());
            case SET_STRING_OPERATION_DESCRIPTOR:
                return operationMapper.map(protoMsg.getSetStringOperationDescriptor());
            case SET_VALUE_OPERATION_DESCRIPTOR:
                return operationMapper.map(protoMsg.getSetValueOperationDescriptor());
            case ABSTRACT_OPERATION_DESCRIPTOR:
            case ABSTRACTOPERATIONDESCRIPTORONEOF_NOT_SET:
            default:
                instanceLogger.error("Descriptor mapping not implemented: {}", type);
        }

        return Util.invalidOperationDescriptor();
    }

    public AbstractSetStateOperationDescriptor map(AbstractSetStateOperationDescriptorOneOfMsg protoMsg) {
        var type = protoMsg.getAbstractSetStateOperationDescriptorOneOfCase();
        switch (type) {
            case ACTIVATE_OPERATION_DESCRIPTOR:
                return operationMapper.map(protoMsg.getActivateOperationDescriptor());
            case SET_ALERT_STATE_OPERATION_DESCRIPTOR:
                return operationMapper.map(protoMsg.getSetAlertStateOperationDescriptor());
            case SET_COMPONENT_STATE_OPERATION_DESCRIPTOR:
                return operationMapper.map(protoMsg.getSetComponentStateOperationDescriptor());
            case SET_CONTEXT_STATE_OPERATION_DESCRIPTOR:
                return operationMapper.map(protoMsg.getSetContextStateOperationDescriptor());
            case SET_METRIC_STATE_OPERATION_DESCRIPTOR:
                return operationMapper.map(protoMsg.getSetMetricStateOperationDescriptor());
            case ABSTRACT_SET_STATE_OPERATION_DESCRIPTOR:
            case ABSTRACTSETSTATEOPERATIONDESCRIPTORONEOF_NOT_SET:
            default:
                instanceLogger.error("Descriptor mapping not implemented: {}", type);
        }

        return Util.invalidSetStateOperationDescriptor();
    }

    private AbstractAlertDescriptor map(AbstractAlertDescriptorOneOfMsg protoMsg) {
        var type = protoMsg.getAbstractAlertDescriptorOneOfCase();
        switch (type) {
            case LIMIT_ALERT_CONDITION_DESCRIPTOR:
                return alertMapper.map(protoMsg.getLimitAlertConditionDescriptor());
            case ALERT_CONDITION_DESCRIPTOR:
                return alertMapper.map(protoMsg.getAlertConditionDescriptor());
            case ALERT_SIGNAL_DESCRIPTOR:
                return alertMapper.map(protoMsg.getAlertSignalDescriptor());
            case ALERT_SYSTEM_DESCRIPTOR:
                return alertMapper.map(protoMsg.getAlertSystemDescriptor());
            default:
                instanceLogger.error("Descriptor mapping not implemented: {}", type);
        }

        return Util.invalidAlertDescriptor();
    }

    private AbstractDescriptor map(AbstractDeviceComponentDescriptorOneOfMsg protoMsg) {
        var type = protoMsg.getAbstractDeviceComponentDescriptorOneOfCase();
        switch (type) {
            case MDS_DESCRIPTOR:
                return componentMapper.map(protoMsg.getMdsDescriptor());
            case VMD_DESCRIPTOR:
                return componentMapper.map(protoMsg.getVmdDescriptor());
            case CHANNEL_DESCRIPTOR:
                return componentMapper.map(protoMsg.getChannelDescriptor());
            case SCO_DESCRIPTOR:
                return componentMapper.map(protoMsg.getScoDescriptor());
            case SYSTEM_CONTEXT_DESCRIPTOR:
                return componentMapper.map(protoMsg.getSystemContextDescriptor());
            case CLOCK_DESCRIPTOR:
            case BATTERY_DESCRIPTOR:
            case ABSTRACT_DEVICE_COMPONENT_DESCRIPTOR:
            case ABSTRACT_COMPLEX_DEVICE_COMPONENT_DESCRIPTOR:
            case ABSTRACTDEVICECOMPONENTDESCRIPTORONEOF_NOT_SET:
            default:
                instanceLogger.error("Descriptor mapping not implemented: {}", type);

        }

        return Util.invalidDescriptor();
    }

    private AbstractDescriptor map(AbstractComplexDeviceComponentDescriptorOneOfMsg protoMsg) {
        var type = protoMsg.getAbstractComplexDeviceComponentDescriptorOneOfCase();
        switch (type) {
            case MDS_DESCRIPTOR:
                return componentMapper.map(protoMsg.getMdsDescriptor());
            case VMD_DESCRIPTOR:
                return componentMapper.map(protoMsg.getVmdDescriptor());
            case ABSTRACT_COMPLEX_DEVICE_COMPONENT_DESCRIPTOR:
            case ABSTRACTCOMPLEXDEVICECOMPONENTDESCRIPTORONEOF_NOT_SET:
            default:
                instanceLogger.error("Descriptor mapping not implemented: {}", type);
        }

        return Util.invalidDescriptor();
    }

    public AbstractState map(AbstractStateOneOfMsg protoMsg) {
        var type = protoMsg.getAbstractStateOneOfCase();
        switch (type) {
            case MDS_STATE:
                return componentMapper.map(protoMsg.getMdsState());
            case VMD_STATE:
                return componentMapper.map(protoMsg.getVmdState());
            case CHANNEL_STATE:
                return componentMapper.map(protoMsg.getChannelState());
            case SCO_STATE:
                return componentMapper.map(protoMsg.getScoState());
            case SYSTEM_CONTEXT_STATE:
                return componentMapper.map(protoMsg.getSystemContextState());
            case NUMERIC_METRIC_STATE:
                return metricMapper.map(protoMsg.getNumericMetricState());
            case REAL_TIME_SAMPLE_ARRAY_METRIC_STATE:
                return metricMapper.map(protoMsg.getRealTimeSampleArrayMetricState());
            case ENUM_STRING_METRIC_STATE:
                return metricMapper.map(protoMsg.getEnumStringMetricState());
            case STRING_METRIC_STATE:
                return metricMapper.map(protoMsg.getStringMetricState());
            case ENSEMBLE_CONTEXT_STATE:
                return contextMapper.map(protoMsg.getEnsembleContextState());
            case LOCATION_CONTEXT_STATE:
                return contextMapper.map(protoMsg.getLocationContextState());
            case PATIENT_CONTEXT_STATE:
                return contextMapper.map(protoMsg.getPatientContextState());
            case LIMIT_ALERT_CONDITION_STATE:
                return alertMapper.map(protoMsg.getLimitAlertConditionState());
            case ALERT_CONDITION_STATE:
                return alertMapper.map(protoMsg.getAlertConditionState());
            case ALERT_SIGNAL_STATE:
                return alertMapper.map(protoMsg.getAlertSignalState());
            case ALERT_SYSTEM_STATE:
                return alertMapper.map(protoMsg.getAlertSystemState());
            case ACTIVATE_OPERATION_STATE:
                return operationMapper.map(protoMsg.getActivateOperationState());
            case SET_ALERT_STATE_OPERATION_STATE:
                return operationMapper.map(protoMsg.getSetAlertStateOperationState());
            case SET_COMPONENT_STATE_OPERATION_STATE:
                return operationMapper.map(protoMsg.getSetComponentStateOperationState());
            case SET_CONTEXT_STATE_OPERATION_STATE:
                return operationMapper.map(protoMsg.getSetContextStateOperationState());
            case SET_METRIC_STATE_OPERATION_STATE:
                return operationMapper.map(protoMsg.getSetMetricStateOperationState());
            case SET_STRING_OPERATION_STATE:
                return operationMapper.map(protoMsg.getSetStringOperationState());
            case SET_VALUE_OPERATION_STATE:
                return operationMapper.map(protoMsg.getSetValueOperationState());
            case BATTERY_STATE:
            case CLOCK_STATE:
            case DISTRIBUTION_SAMPLE_ARRAY_METRIC_STATE:
            case MEANS_CONTEXT_STATE:
            case OPERATOR_CONTEXT_STATE:
            case WORKFLOW_CONTEXT_STATE:
            case ABSTRACT_STATE:
            case ABSTRACT_COMPLEX_DEVICE_COMPONENT_STATE:
            case ABSTRACT_OPERATION_STATE:
            case ABSTRACT_DEVICE_COMPONENT_STATE:
            case ABSTRACT_MULTI_STATE:
            case ABSTRACT_ALERT_STATE:
            case ABSTRACT_CONTEXT_STATE:
            case ABSTRACT_METRIC_STATE:
            case ABSTRACTSTATEONEOF_NOT_SET:
            default:
                instanceLogger.error("State mapping not implemented: {}", type);
        }

        return Util.invalidState();
    }

    public AbstractOperationState map(final AbstractOperationStateOneOfMsg protoMsg) {
        var type = protoMsg.getAbstractOperationStateOneOfCase();
        switch (type) {
            case SET_CONTEXT_STATE_OPERATION_STATE:
                return operationMapper.map(protoMsg.getSetContextStateOperationState());
            case SET_VALUE_OPERATION_STATE:
                return operationMapper.map(protoMsg.getSetValueOperationState());
            case SET_COMPONENT_STATE_OPERATION_STATE:
                return operationMapper.map(protoMsg.getSetComponentStateOperationState());
            case SET_METRIC_STATE_OPERATION_STATE:
                return operationMapper.map(protoMsg.getSetMetricStateOperationState());
            case SET_ALERT_STATE_OPERATION_STATE:
                return operationMapper.map(protoMsg.getSetAlertStateOperationState());
            case SET_STRING_OPERATION_STATE:
                return operationMapper.map(protoMsg.getSetStringOperationState());
            case ACTIVATE_OPERATION_STATE:
                return operationMapper.map(protoMsg.getActivateOperationState());
            case ABSTRACT_OPERATION_STATE:
            case ABSTRACTOPERATIONSTATEONEOF_NOT_SET:
            default:
                instanceLogger.error("State mapping not implemented: {}", type);
        }

        return Util.invalidOperationState();
    }

    public AbstractAlertState map(final AbstractAlertStateOneOfMsg protoMsg) {
        var type = protoMsg.getAbstractAlertStateOneOfCase();
        switch (type) {
            case LIMIT_ALERT_CONDITION_STATE:
                return alertMapper.map(protoMsg.getLimitAlertConditionState());
            case ALERT_CONDITION_STATE:
                return alertMapper.map(protoMsg.getAlertConditionState());
            case ALERT_SIGNAL_STATE:
                return alertMapper.map(protoMsg.getAlertSignalState());
            case ALERT_SYSTEM_STATE:
                return alertMapper.map(protoMsg.getAlertSystemState());
            default:
                instanceLogger.error("State mapping not implemented: {}", type);
                break;
        }

        return Util.invalidAlertState();
    }

    private AbstractAlertState map(final AlertConditionStateOneOfMsg protoMsg) {
        var type = protoMsg.getAlertConditionStateOneOfCase();
        switch (type) {
            case ALERT_CONDITION_STATE:
                return alertMapper.map(protoMsg.getAlertConditionState());
            case LIMIT_ALERT_CONDITION_STATE:
                return alertMapper.map(protoMsg.getLimitAlertConditionState());
            case ALERTCONDITIONSTATEONEOF_NOT_SET:
            default:
                instanceLogger.error("State mapping not implemented: {}", type);
                break;
        }


        return Util.invalidAlertState();
    }

    public AbstractMetricState map(AbstractMetricStateOneOfMsg protoMsg) {
        var type = protoMsg.getAbstractMetricStateOneOfCase();
        switch (type) {
            case NUMERIC_METRIC_STATE:
                return metricMapper.map(protoMsg.getNumericMetricState());
            case REAL_TIME_SAMPLE_ARRAY_METRIC_STATE:
                return metricMapper.map(protoMsg.getRealTimeSampleArrayMetricState());
            case ENUM_STRING_METRIC_STATE:
                return metricMapper.map(protoMsg.getEnumStringMetricState());
            case STRING_METRIC_STATE:
                return metricMapper.map(protoMsg.getStringMetricState());
            case DISTRIBUTION_SAMPLE_ARRAY_METRIC_STATE:
            case ABSTRACT_METRIC_STATE:
            case ABSTRACTMETRICSTATEONEOF_NOT_SET:
            default:
                instanceLogger.error("State mapping not implemented: {}", type);
                break;
        }
        return Util.invalidMetricState();
    }

    private StringMetricState map(StringMetricStateOneOfMsg protoMsg) {
        var type = protoMsg.getStringMetricStateOneOfCase();
        switch (type) {
            case ENUM_STRING_METRIC_STATE:
                return metricMapper.map(protoMsg.getEnumStringMetricState());
            case STRING_METRIC_STATE:
                return metricMapper.map(protoMsg.getStringMetricState());
        }

        return Util.invalidStringMetricState();
    }

    private AbstractState map(AbstractMultiStateOneOfMsg protoMsg) {
        var type = protoMsg.getAbstractMultiStateOneOfCase();
        switch (type) {
            case ENSEMBLE_CONTEXT_STATE:
                return contextMapper.map(protoMsg.getEnsembleContextState());
            case PATIENT_CONTEXT_STATE:
                return contextMapper.map(protoMsg.getPatientContextState());
            case LOCATION_CONTEXT_STATE:
                return contextMapper.map(protoMsg.getLocationContextState());
            case ABSTRACT_CONTEXT_STATE:
            case MEANS_CONTEXT_STATE:
            case OPERATOR_CONTEXT_STATE:
            case WORKFLOW_CONTEXT_STATE:
            case ABSTRACT_MULTI_STATE:
            case ABSTRACTMULTISTATEONEOF_NOT_SET:
            default:
                instanceLogger.error("State mapping not implemented: {}", type);
        }

        return Util.invalidState();
    }

    public AbstractContextState map(AbstractContextStateOneOfMsg protoMsg) {
        var type = protoMsg.getAbstractContextStateOneOfCase();
        switch (type) {
            case ENSEMBLE_CONTEXT_STATE:
                return contextMapper.map(protoMsg.getEnsembleContextState());
            case PATIENT_CONTEXT_STATE:
                return contextMapper.map(protoMsg.getPatientContextState());
            case LOCATION_CONTEXT_STATE:
                return contextMapper.map(protoMsg.getLocationContextState());
            case ABSTRACT_CONTEXT_STATE:
            case MEANS_CONTEXT_STATE:
            case OPERATOR_CONTEXT_STATE:
            case WORKFLOW_CONTEXT_STATE:
            case ABSTRACTCONTEXTSTATEONEOF_NOT_SET:
            default:
                instanceLogger.error("State mapping not implemented: {}", type);
        }

        return Util.invalidContextState();
    }

    public AbstractDeviceComponentState map(AbstractDeviceComponentStateOneOfMsg protoMsg) {
        var type = protoMsg.getAbstractDeviceComponentStateOneOfCase();
        switch (type) {
            case MDS_STATE:
                return componentMapper.map(protoMsg.getMdsState());
            case VMD_STATE:
                return componentMapper.map(protoMsg.getVmdState());
            case CHANNEL_STATE:
                return componentMapper.map(protoMsg.getChannelState());
            case SCO_STATE:
                return componentMapper.map(protoMsg.getScoState());
            case SYSTEM_CONTEXT_STATE:
                return componentMapper.map(protoMsg.getSystemContextState());
            case BATTERY_STATE:
            case CLOCK_STATE:
            case ABSTRACT_DEVICE_COMPONENT_STATE:
            case ABSTRACT_COMPLEX_DEVICE_COMPONENT_STATE:
            case ABSTRACTDEVICECOMPONENTSTATEONEOF_NOT_SET:
            default:
                instanceLogger.error("State mapping not implemented: {}", type);
                break;
        }

        return Util.invalidDeviceComponentState();
    }

    private AbstractDeviceComponentState map(AbstractComplexDeviceComponentStateOneOfMsg protoMsg) {
        var type = protoMsg.getAbstractComplexDeviceComponentStateOneOfCase();
        switch (type) {
            case MDS_STATE:
                return componentMapper.map(protoMsg.getMdsState());
            case VMD_STATE:
                return componentMapper.map(protoMsg.getVmdState());
            case ABSTRACT_COMPLEX_DEVICE_COMPONENT_STATE:
            case ABSTRACTCOMPLEXDEVICECOMPONENTSTATEONEOF_NOT_SET:
            default:
                instanceLogger.error("State mapping not implemented: {}", type);
                break;
        }

        return Util.invalidDeviceComponentState();
    }

    public PatientDemographicsCoreData map(PatientDemographicsCoreDataOneOfMsg protoMsg) {
        var type = protoMsg.getPatientDemographicsCoreDataOneOfCase();
        switch (type) {
            case NEONATAL_PATIENT_DEMOGRAPHICS_CORE_DATA:
                return contextMapper.map(protoMsg.getNeonatalPatientDemographicsCoreData());
            case PATIENT_DEMOGRAPHICS_CORE_DATA:
                return contextMapper.map(protoMsg.getPatientDemographicsCoreData());
            default:
                instanceLogger.error("Mapping not implemented: {}", type);
                break;
        }
        return Util.invalidPatientDemographicsCoreData();
    }

    public PersonReference map(PersonReferenceOneOfMsg protoMsg) {
        var type = protoMsg.getPersonReferenceOneOfCase();
        switch (type) {
            case PERSON_REFERENCE:
                return contextMapper.map(protoMsg.getPersonReference());
            case PERSON_PARTICIPATION:
                return contextMapper.map(protoMsg.getPersonParticipation());
        }
        return Util.invalidPersonReference();
    }

    public InstanceIdentifier map(InstanceIdentifierOneOfMsg protoMsg) {
        var type = protoMsg.getInstanceIdentifierOneOfCase();
        switch (type) {
            case INSTANCE_IDENTIFIER:
                return baseMapper.map(protoMsg.getInstanceIdentifier());
            case OPERATING_JURISDICTION:
            default:
                instanceLogger.error("Mapping not implemented: {}", type);
                break;
        }
        return Util.invalidInstanceIdentifier();
    }

    public BaseDemographics map(BaseDemographicsOneOfMsg protoMsg) {
        var type = protoMsg.getBaseDemographicsOneOfCase();
        switch (type) {
            case NEONATAL_PATIENT_DEMOGRAPHICS_CORE_DATA:
                return contextMapper.map(protoMsg.getNeonatalPatientDemographicsCoreData());
            case PATIENT_DEMOGRAPHICS_CORE_DATA:
                return contextMapper.map(protoMsg.getPatientDemographicsCoreData());
            case BASE_DEMOGRAPHICS:
                return baseMapper.map(protoMsg.getBaseDemographics());
            default:
                instanceLogger.error("Mapping not implemented: {}", type);
                break;
        }

        return Util.invalidBaseDemographics();
    }
}
