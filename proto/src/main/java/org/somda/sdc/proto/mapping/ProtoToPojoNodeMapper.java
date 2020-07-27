package org.somda.sdc.proto.mapping;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.MessageOrBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.proto.model.biceps.AbstractComplexDeviceComponentDescriptorMsg;
import org.somda.sdc.proto.model.biceps.AbstractComplexDeviceComponentDescriptorOneOfMsg;
import org.somda.sdc.proto.model.biceps.AbstractComplexDeviceComponentStateMsg;
import org.somda.sdc.proto.model.biceps.AbstractComplexDeviceComponentStateOneOfMsg;
import org.somda.sdc.proto.model.biceps.AbstractDescriptorMsg;
import org.somda.sdc.proto.model.biceps.AbstractDescriptorOneOfMsg;
import org.somda.sdc.proto.model.biceps.AbstractDeviceComponentDescriptorMsg;
import org.somda.sdc.proto.model.biceps.AbstractDeviceComponentDescriptorOneOfMsg;
import org.somda.sdc.proto.model.biceps.AbstractDeviceComponentStateMsg;
import org.somda.sdc.proto.model.biceps.AbstractDeviceComponentStateOneOfMsg;
import org.somda.sdc.proto.model.biceps.AbstractStateMsg;
import org.somda.sdc.proto.model.biceps.AbstractStateOneOfMsg;
import org.somda.sdc.proto.model.biceps.ChannelDescriptorMsg;
import org.somda.sdc.proto.model.biceps.ChannelStateMsg;
import org.somda.sdc.proto.model.biceps.MdsDescriptorMsg;
import org.somda.sdc.proto.model.biceps.MdsStateMsg;
import org.somda.sdc.proto.model.biceps.OperatingJurisdictionMsg;
import org.somda.sdc.proto.model.biceps.VmdDescriptorMsg;
import org.somda.sdc.proto.model.biceps.VmdStateMsg;

import java.math.BigInteger;
import java.util.stream.Collectors;

public class ProtoToPojoNodeMapper {
    private static final Logger LOG = LogManager.getLogger(ProtoToPojoNodeMapper.class);

    private final ProtoToPojoBaseMapper baseMapper;
    private final Logger instanceLogger;

    @Inject
    ProtoToPojoNodeMapper(ProtoToPojoBaseMapper baseMapper,
                          @Named(org.somda.sdc.common.CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.baseMapper = baseMapper;
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
    }

    public AbstractDescriptor mapDescriptor(MessageOrBuilder protoMsg) {
        if (protoMsg instanceof MdsDescriptorMsg) {
            return map((MdsDescriptorMsg)protoMsg);
        } else if (protoMsg instanceof VmdDescriptorMsg) {
            return map((VmdDescriptorMsg)protoMsg);
        }else if (protoMsg instanceof ChannelDescriptorMsg) {
            return map((ChannelDescriptorMsg)protoMsg);
        } else {
            instanceLogger.error("Descriptor mapping not implemented: {}", protoMsg);
            return invalidDescriptor();
        }
    }

    public AbstractState mapState(MessageOrBuilder protoMsg) {
        if (protoMsg instanceof MdsStateMsg) {
            return map((MdsStateMsg)protoMsg);
        } else if (protoMsg instanceof VmdStateMsg) {
            return map((VmdStateMsg)protoMsg);
        }else if (protoMsg instanceof ChannelStateMsg) {
            return map((ChannelStateMsg)protoMsg);
        } else {
            instanceLogger.error("Descriptor mapping not implemented: {}", protoMsg);
            return invalidState();
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

        return invalidDescriptor();
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
                return map(protoMsg.getChannelDescriptor());
            case SCO_DESCRIPTOR:
                instanceLogger.error("Descriptor mapping not implemented: {}", type);
                break;
            case SYSTEM_CONTEXT_DESCRIPTOR:
                instanceLogger.error("Descriptor mapping not implemented: {}", type);
                break;
            case ABSTRACT_COMPLEX_DEVICE_COMPONENT_DESCRIPTOR_ONE_OF:
                break;
            case ABSTRACTDEVICECOMPONENTDESCRIPTORONEOF_NOT_SET:
            default:
                instanceLogger.error("Descriptor mapping not implemented: {}", type);

        }

        return invalidDescriptor();
    }

    private AbstractDescriptor map(AbstractComplexDeviceComponentDescriptorOneOfMsg protoMsg) {
        var type = protoMsg.getAbstractComplexDeviceComponentDescriptorOneOfCase();
        switch (type) {
            case ABSTRACT_COMPLEX_DEVICE_COMPONENT_DESCRIPTOR:
                instanceLogger.error("Descriptor mapping not implemented: {}", type);
                break;
            case VMD_DESCRIPTOR:
                return map(protoMsg.getVmdDescriptor());
            case MDS_DESCRIPTOR:
                return map(protoMsg.getMdsDescriptor());
            case ABSTRACTCOMPLEXDEVICECOMPONENTDESCRIPTORONEOF_NOT_SET:
            default:
                instanceLogger.error("Descriptor mapping not implemented: {}", type);
        }

        return invalidDescriptor();
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
                instanceLogger.error("State mapping not implemented: {}", type);
                break;
            case ABSTRACT_METRIC_STATE_ONE_OF:
                instanceLogger.error("State mapping not implemented: {}", type);
                break;
            case ABSTRACT_DEVICE_COMPONENT_STATE_ONE_OF:
                return map(protoMsg.getAbstractDeviceComponentStateOneOf());
            case ABSTRACTSTATEONEOF_NOT_SET:
            default:
                instanceLogger.error("State mapping not implemented: {}", type);
        }

        return invalidState();
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
                return map(protoMsg.getChannelState());
            case SYSTEM_CONTEXT_STATE:
                instanceLogger.error("State mapping not implemented: {}", type);
                break;
            case ABSTRACT_COMPLEX_DEVICE_COMPONENT_STATE_ONE_OF:
                return map(protoMsg.getAbstractComplexDeviceComponentStateOneOf());
            case BATTERY_STATE:
                instanceLogger.error("State mapping not implemented: {}", type);
                break;
            case SCO_STATE:
                instanceLogger.error("State mapping not implemented: {}", type);
                break;
            case ABSTRACTDEVICECOMPONENTSTATEONEOF_NOT_SET:
            default:
                instanceLogger.error("State mapping not implemented: {}", type);
                break;
        }

        return invalidState();
    }

    private AbstractState map(AbstractComplexDeviceComponentStateOneOfMsg protoMsg) {
        var type = protoMsg.getAbstractComplexDeviceComponentStateOneOfCase();
        switch (type) {
            case ABSTRACT_COMPLEX_DEVICE_COMPONENT_STATE:
                instanceLogger.error("State mapping not implemented: {}", type);
                break;
            case VMD_STATE:
                return map(protoMsg.getVmdState());
            case MDS_STATE:
                return map(protoMsg.getMdsState());
            case ABSTRACTCOMPLEXDEVICECOMPONENTSTATEONEOF_NOT_SET:
            default:
                instanceLogger.error("State mapping not implemented: {}", type);
                break;
        }

        return invalidState();
    }



    public MdsDescriptor map(MdsDescriptorMsg protoMsg) {
        var pojo = new MdsDescriptor();
        map(pojo, protoMsg.getAbstractComplexDeviceComponentDescriptor());
        return pojo;
    }

    public MdsState map(MdsStateMsg protoMsg) {
        var pojoState = new MdsState();
        pojoState.setLang(Util.optionalStr(protoMsg, "ALang"));
        pojoState.setOperatingMode(Util.mapToPojoEnum(protoMsg, "AOperatingMode", MdsOperatingMode.class));
        pojoState.setOperatingJurisdiction(baseMapper.map(
                Util.optional(protoMsg, "OperatingJurisdiction", OperatingJurisdictionMsg.class)));
        map(pojoState, protoMsg.getAbstractComplexDeviceComponentState());
        return pojoState;
    }

    public VmdDescriptor map(VmdDescriptorMsg protoMsg) {
        var pojoState = new VmdDescriptor();
        map(pojoState, protoMsg.getAbstractComplexDeviceComponentDescriptor());
        return pojoState;
    }

    public VmdState map(VmdStateMsg protoMsg) {
        var pojoState = new VmdState();
        pojoState.setOperatingJurisdiction(baseMapper.map(
                Util.optional(protoMsg, "OperatingJurisdiction", OperatingJurisdictionMsg.class)));
        map(pojoState, protoMsg.getAbstractComplexDeviceComponentState());
        return pojoState;
    }

    public ChannelDescriptor map(ChannelDescriptorMsg protoMsg) {
        var pojo = new ChannelDescriptor();
        map(pojo, protoMsg.getAbstractDeviceComponentDescriptor());
        return pojo;
    }

    public ChannelState map(ChannelStateMsg protoMsg) {
        var pojoState = new ChannelState();
        map(pojoState, protoMsg.getAbstractDeviceComponentState());
        return pojoState;
    }

    private void map(AbstractDeviceComponentDescriptor pojo, AbstractDeviceComponentDescriptorMsg protoMsg) {
        pojo.setProductionSpecification(protoMsg.getProductionSpecificationList()
                .stream().map(baseMapper::map).collect(Collectors.toList()));
        map(pojo, protoMsg.getAbstractDescriptor());
    }

    private void map(AbstractDeviceComponentState pojo, AbstractDeviceComponentStateMsg protoMsg) {
        pojo.setActivationState(Util.mapToPojoEnum(protoMsg, "AActivationState", ComponentActivation.class));
        pojo.setOperatingCycles(Util.optionalIntOfInt(protoMsg, "AOperatingCycles"));
        pojo.setOperatingHours(Util.optionalLongOfInt(protoMsg, "AOperatingHours"));
        instanceLogger.error("CalibrationInfo mapping is missing");
        instanceLogger.error("NextCalibration mapping is missing");
        instanceLogger.error("PhysicalConnectorInfo mapping is missing");
        map(pojo, protoMsg.getAbstractState());
    }

    private void map(AbstractComplexDeviceComponentDescriptor pojo,
                     AbstractComplexDeviceComponentDescriptorMsg protoMsg) {
        map(pojo, protoMsg.getAbstractDeviceComponentDescriptor());
    }

    private void map(AbstractComplexDeviceComponentState pojo, AbstractComplexDeviceComponentStateMsg protoMsg) {
        map(pojo, protoMsg.getAbstractDeviceComponentState());
    }

    private void map(AbstractDescriptor pojo, AbstractDescriptorMsg protoMsg) {
        pojo.setHandle(protoMsg.getAHandle());
        pojo.setDescriptorVersion(Util.optionalBigIntOfLong(protoMsg, "ADescriptorVersion"));
        pojo.setSafetyClassification(Util.mapToPojoEnum(protoMsg, "ASafetyClassification", SafetyClassification.class));
    }

    private void map(AbstractState pojo, AbstractStateMsg protoMsg) {
        pojo.setDescriptorHandle(protoMsg.getADescriptorHandle());
        pojo.setDescriptorVersion(Util.optionalBigIntOfLong(protoMsg, "ADescriptorVersion"));
        pojo.setStateVersion(Util.optionalBigIntOfLong(protoMsg, "AStateVersion"));
    }

    private AbstractState invalidState() {
        var state = new AbstractState();
        state.setDescriptorHandle("[mapping failed]");
        return state;
    }

    private AbstractDescriptor invalidDescriptor() {
        var descr = new AbstractDescriptor();
        descr.setHandle("[mapping failed]");
        return descr;
    }
}
