package org.somda.sdc.proto.mapping;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.MessageOrBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.participant.AbstractDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractMetricState;
import org.somda.sdc.biceps.model.participant.AbstractState;
import org.somda.sdc.biceps.model.participant.NumericMetricState;
import org.somda.sdc.biceps.model.participant.RealTimeSampleArrayMetricDescriptor;
import org.somda.sdc.biceps.model.participant.ScoState;
import org.somda.sdc.biceps.model.participant.StringMetricState;
import org.somda.sdc.biceps.model.participant.SystemContextDescriptor;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.common.util.TimestampAdapter;
import org.somda.sdc.proto.model.biceps.*;

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
            case ABSTRACT_DESCRIPTOR:
                instanceLogger.error("Descriptor mapping not implemented: {}", type);
                break;
            case ABSTRACT_ALERT_DESCRIPTOR_ONE_OF:
                instanceLogger.error("Descriptor mapping not implemented: {}", type);
                break;
            case ABSTRACT_DEVICE_COMPONENT_DESCRIPTOR_ONE_OF:
                return map(protoMsg.getAbstractDeviceComponentDescriptorOneOf());
            case ABSTRACT_OPERATION_DESCRIPTOR_ONE_OF:
                instanceLogger.error("Descriptor mapping not implemented: {}", type);
                break;
            case ABSTRACT_METRIC_DESCRIPTOR_ONE_OF:
                instanceLogger.error("Descriptor mapping not implemented: {}", type);
                break;
            case ABSTRACT_CONTEXT_DESCRIPTOR_ONE_OF:
                instanceLogger.error("Descriptor mapping not implemented: {}", type);
                break;
            case ABSTRACTDESCRIPTORONEOF_NOT_SET:
            default:
                instanceLogger.error("Descriptor mapping not implemented: {}", type);
        }

        return Util.invalidDescriptor();
    }

    private AbstractDescriptor map(AbstractDeviceComponentDescriptorOneOfMsg protoMsg) {
        var type = protoMsg.getAbstractDeviceComponentDescriptorOneOfCase();
        switch (type) {
            case ABSTRACT_DEVICE_COMPONENT_DESCRIPTOR:
                break;
            case CLOCK_DESCRIPTOR:
                instanceLogger.error("Descriptor mapping not implemented: {}", type);
                break;
            case BATTERY_DESCRIPTOR:
                instanceLogger.error("Descriptor mapping not implemented: {}", type);
                break;
            case CHANNEL_DESCRIPTOR:
                return componentMapper.map(protoMsg.getChannelDescriptor());
            case SCO_DESCRIPTOR:
                return componentMapper.map(protoMsg.getScoDescriptor());
            case SYSTEM_CONTEXT_DESCRIPTOR:
                return componentMapper.map(protoMsg.getSystemContextDescriptor());
            case ABSTRACT_COMPLEX_DEVICE_COMPONENT_DESCRIPTOR_ONE_OF:
                break;
            case ABSTRACTDEVICECOMPONENTDESCRIPTORONEOF_NOT_SET:
            default:
                instanceLogger.error("Descriptor mapping not implemented: {}", type);

        }

        return Util.invalidDescriptor();
    }

    private AbstractDescriptor map(AbstractComplexDeviceComponentDescriptorOneOfMsg protoMsg) {
        var type = protoMsg.getAbstractComplexDeviceComponentDescriptorOneOfCase();
        switch (type) {
            case ABSTRACT_COMPLEX_DEVICE_COMPONENT_DESCRIPTOR:
                instanceLogger.error("Descriptor mapping not implemented: {}", type);
                break;
            case VMD_DESCRIPTOR:
                return componentMapper.map(protoMsg.getVmdDescriptor());
            case MDS_DESCRIPTOR:
                return componentMapper.map(protoMsg.getMdsDescriptor());
            case ABSTRACTCOMPLEXDEVICECOMPONENTDESCRIPTORONEOF_NOT_SET:
            default:
                instanceLogger.error("Descriptor mapping not implemented: {}", type);
        }

        return Util.invalidDescriptor();
    }

    public AbstractState map(AbstractStateOneOfMsg protoMsg) {
        var type = protoMsg.getAbstractStateOneOfCase();
        switch (type) {
            case ABSTRACT_STATE:
                instanceLogger.error("State mapping not implemented: {}", type);
                break;
            case ABSTRACT_OPERATION_STATE_ONE_OF:
                instanceLogger.error("State mapping not implemented: {}", type);
                break;
            case ABSTRACT_ALERT_STATE_ONE_OF:
                instanceLogger.error("State mapping not implemented: {}", type);
                break;
            case ABSTRACT_MULTI_STATE_ONE_OF:
                return map(protoMsg.getAbstractMultiStateOneOf());
            case ABSTRACT_METRIC_STATE_ONE_OF:
                return map(protoMsg.getAbstractMetricStateOneOf());
            case ABSTRACT_DEVICE_COMPONENT_STATE_ONE_OF:
                return map(protoMsg.getAbstractDeviceComponentStateOneOf());
            case ABSTRACTSTATEONEOF_NOT_SET:
            default:
                instanceLogger.error("State mapping not implemented: {}", type);
        }

        return Util.invalidState();
    }

    private AbstractMetricState map(AbstractMetricStateOneOfMsg protoMsg) {
        var type = protoMsg.getAbstractMetricStateOneOfCase();
        switch (type) {
            case STRING_METRIC_STATE_ONE_OF:
                return map(protoMsg.getStringMetricStateOneOf());
            case NUMERIC_METRIC_STATE:
                return metricMapper.map(protoMsg.getNumericMetricState());
            case REAL_TIME_SAMPLE_ARRAY_METRIC_STATE:
                return metricMapper.map(protoMsg.getRealTimeSampleArrayMetricState());
            case DISTRIBUTION_SAMPLE_ARRAY_METRIC_STATE:
                instanceLogger.error("State mapping not implemented: {}", type);
                break;
            case ABSTRACT_METRIC_STATE:
                instanceLogger.error("State mapping not implemented: {}", type);
                break;
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
            case ABSTRACT_MULTI_STATE:
                instanceLogger.error("State mapping not implemented: {}", type);
                break;
            case ABSTRACT_CONTEXT_STATE_ONE_OF:
                return map(protoMsg.getAbstractContextStateOneOf());
            case ABSTRACTMULTISTATEONEOF_NOT_SET:
            default:
                instanceLogger.error("State mapping not implemented: {}", type);
        }

        return Util.invalidState();
    }

    private AbstractState map(AbstractContextStateOneOfMsg protoMsg) {
        var type = protoMsg.getAbstractContextStateOneOfCase();
        switch (type) {
            case ABSTRACT_CONTEXT_STATE:
                instanceLogger.error("State mapping not implemented: {}", type);
                break;
            case OPERATOR_CONTEXT_STATE:
                instanceLogger.error("State mapping not implemented: {}", type);
                break;
            case ENSEMBLE_CONTEXT_STATE:
                return contextMapper.map(protoMsg.getEnsembleContextState());
            case WORKFLOW_CONTEXT_STATE:
                instanceLogger.error("State mapping not implemented: {}", type);
                break;
            case PATIENT_CONTEXT_STATE:
                instanceLogger.error("State mapping not implemented: {}", type);
                break;
            case LOCATION_CONTEXT_STATE:
                return contextMapper.map(protoMsg.getLocationContextState());
            case MEANS_CONTEXT_STATE:
                instanceLogger.error("State mapping not implemented: {}", type);
                break;
            case ABSTRACTCONTEXTSTATEONEOF_NOT_SET:
            default:
                instanceLogger.error("State mapping not implemented: {}", type);
        }

        return Util.invalidState();
    }

    private AbstractState map(AbstractDeviceComponentStateOneOfMsg protoMsg) {
        var type = protoMsg.getAbstractDeviceComponentStateOneOfCase();
        switch (type) {
            case ABSTRACT_DEVICE_COMPONENT_STATE:
                instanceLogger.error("State mapping not implemented: {}", type);
                break;
            case CLOCK_STATE:
                instanceLogger.error("State mapping not implemented: {}", type);
                break;
            case CHANNEL_STATE:
                return componentMapper.map(protoMsg.getChannelState());
            case SYSTEM_CONTEXT_STATE:
                return componentMapper.map(protoMsg.getSystemContextState());
            case ABSTRACT_COMPLEX_DEVICE_COMPONENT_STATE_ONE_OF:
                return map(protoMsg.getAbstractComplexDeviceComponentStateOneOf());
            case BATTERY_STATE:
                instanceLogger.error("State mapping not implemented: {}", type);
                break;
            case SCO_STATE:
                return map(protoMsg.getScoState());
            case ABSTRACTDEVICECOMPONENTSTATEONEOF_NOT_SET:
            default:
                instanceLogger.error("State mapping not implemented: {}", type);
                break;
        }

        return Util.invalidState();
    }

    private ScoState map(ScoStateMsg protoMsg) {
        var pojo = new ScoState();
        pojo.setInvocationRequested(protoMsg.getAInvocationRequested().getOperationRefList());
        pojo.setInvocationRequired(protoMsg.getAInvocationRequired().getOperationRefList());
        return pojo;
    }

    private AbstractState map(AbstractComplexDeviceComponentStateOneOfMsg protoMsg) {
        var type = protoMsg.getAbstractComplexDeviceComponentStateOneOfCase();
        switch (type) {
            case ABSTRACT_COMPLEX_DEVICE_COMPONENT_STATE:
                instanceLogger.error("State mapping not implemented: {}", type);
                break;
            case VMD_STATE:
                return componentMapper.map(protoMsg.getVmdState());
            case MDS_STATE:
                return componentMapper.map(protoMsg.getMdsState());
            case ABSTRACTCOMPLEXDEVICECOMPONENTSTATEONEOF_NOT_SET:
            default:
                instanceLogger.error("State mapping not implemented: {}", type);
                break;
        }

        return Util.invalidState();
    }
}
