package org.somda.sdc.proto.mapping;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.participant.AbstractComplexDeviceComponentDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractComplexDeviceComponentState;
import org.somda.sdc.biceps.model.participant.AbstractDeviceComponentDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractDeviceComponentState;
import org.somda.sdc.biceps.model.participant.ApprovedJurisdictions;
import org.somda.sdc.biceps.model.participant.ChannelDescriptor;
import org.somda.sdc.biceps.model.participant.ChannelState;
import org.somda.sdc.biceps.model.participant.ComponentActivation;
import org.somda.sdc.biceps.model.participant.MdsDescriptor;
import org.somda.sdc.biceps.model.participant.MdsOperatingMode;
import org.somda.sdc.biceps.model.participant.MdsState;
import org.somda.sdc.biceps.model.participant.ScoDescriptor;
import org.somda.sdc.biceps.model.participant.ScoState;
import org.somda.sdc.biceps.model.participant.SystemContextDescriptor;
import org.somda.sdc.biceps.model.participant.SystemContextState;
import org.somda.sdc.biceps.model.participant.VmdDescriptor;
import org.somda.sdc.biceps.model.participant.VmdState;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.proto.model.biceps.AbstractComplexDeviceComponentDescriptorMsg;
import org.somda.sdc.proto.model.biceps.AbstractComplexDeviceComponentStateMsg;
import org.somda.sdc.proto.model.biceps.AbstractDeviceComponentDescriptorMsg;
import org.somda.sdc.proto.model.biceps.AbstractDeviceComponentStateMsg;
import org.somda.sdc.proto.model.biceps.ApprovedJurisdictionsMsg;
import org.somda.sdc.proto.model.biceps.ChannelDescriptorMsg;
import org.somda.sdc.proto.model.biceps.ChannelStateMsg;
import org.somda.sdc.proto.model.biceps.MdsDescriptorMsg;
import org.somda.sdc.proto.model.biceps.MdsStateMsg;
import org.somda.sdc.proto.model.biceps.OperatingJurisdictionMsg;
import org.somda.sdc.proto.model.biceps.ScoDescriptorMsg;
import org.somda.sdc.proto.model.biceps.ScoStateMsg;
import org.somda.sdc.proto.model.biceps.SystemContextDescriptorMsg;
import org.somda.sdc.proto.model.biceps.SystemContextStateMsg;
import org.somda.sdc.proto.model.biceps.VmdDescriptorMsg;
import org.somda.sdc.proto.model.biceps.VmdStateMsg;

import java.util.stream.Collectors;

public class ProtoToPojoComponentMapper {
    private static final Logger LOG = LogManager.getLogger(ProtoToPojoComponentMapper.class);
    private final Logger instanceLogger;
    private final ProtoToPojoBaseMapper baseMapper;

    @Inject
    ProtoToPojoComponentMapper(@Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier,
                               ProtoToPojoBaseMapper baseMapper) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.baseMapper = baseMapper;
    }

    public MdsDescriptor map(MdsDescriptorMsg protoMsg) {
        var pojo = new MdsDescriptor();
        if (protoMsg.hasApprovedJurisdictions()) {
            pojo.setApprovedJurisdictions(map(protoMsg.getApprovedJurisdictions()));
        }
        map(pojo, protoMsg.getAbstractComplexDeviceComponentDescriptor());
        return pojo;
    }

    public MdsState map(MdsStateMsg protoMsg) {
        var pojo = new MdsState();
        pojo.setLang(Util.optionalStr(protoMsg, "ALang"));
        pojo.setOperatingMode(Util.mapToPojoEnum(protoMsg, "AOperatingMode", MdsOperatingMode.class));
        pojo.setOperatingJurisdiction(baseMapper.map(
                Util.optional(protoMsg, "OperatingJurisdiction", OperatingJurisdictionMsg.class)));
        map(pojo, protoMsg.getAbstractComplexDeviceComponentState());
        return pojo;
    }

    public VmdDescriptor map(VmdDescriptorMsg protoMsg) {
        var pojo = new VmdDescriptor();
        if (protoMsg.hasApprovedJurisdictions()) {
            pojo.setApprovedJurisdictions(map(protoMsg.getApprovedJurisdictions()));
        }
        map(pojo, protoMsg.getAbstractComplexDeviceComponentDescriptor());
        return pojo;
    }

    public VmdState map(VmdStateMsg protoMsg) {
        var pojo = new VmdState();
        pojo.setOperatingJurisdiction(baseMapper.map(
                Util.optional(protoMsg, "OperatingJurisdiction", OperatingJurisdictionMsg.class)));
        map(pojo, protoMsg.getAbstractComplexDeviceComponentState());
        return pojo;
    }

    private ApprovedJurisdictions map(ApprovedJurisdictionsMsg protoMsg) {
        var apj = new ApprovedJurisdictions();
        apj.setApprovedJurisdiction(baseMapper.mapInstanceIdentifiers(protoMsg.getApprovedJurisdictionList()));
        return apj;
    }

    public ChannelDescriptor map(ChannelDescriptorMsg protoMsg) {
        var pojo = new ChannelDescriptor();
        map(pojo, protoMsg.getAbstractDeviceComponentDescriptor());
        return pojo;
    }

    public ChannelState map(ChannelStateMsg protoMsg) {
        var pojo = new ChannelState();
        map(pojo, protoMsg.getAbstractDeviceComponentState());
        return pojo;
    }

    ScoDescriptor map(ScoDescriptorMsg protoMsg) {
        var pojo = new ScoDescriptor();
        map(pojo, protoMsg.getAbstractDeviceComponentDescriptor());
        return pojo;
    }

    ScoState map(ScoStateMsg protoMsg) {
        var pojo = new ScoState();
        pojo.setInvocationRequested(protoMsg.getAInvocationRequested().getOperationRefList());
        pojo.setInvocationRequired(protoMsg.getAInvocationRequired().getOperationRefList());
        map(pojo, protoMsg.getAbstractDeviceComponentState());
        return pojo;
    }

    SystemContextDescriptor map(SystemContextDescriptorMsg protoMsg) {
        var pojo = new SystemContextDescriptor();
        map(pojo, protoMsg.getAbstractDeviceComponentDescriptor());
        return pojo;
    }

    SystemContextState map(SystemContextStateMsg protoMsg) {
        var pojo = new SystemContextState();
        map(pojo, protoMsg.getAbstractDeviceComponentState());
        return pojo;
    }

    private void map(AbstractComplexDeviceComponentDescriptor pojo,
                     AbstractComplexDeviceComponentDescriptorMsg protoMsg) {
        map(pojo, protoMsg.getAbstractDeviceComponentDescriptor());
    }

    private void map(AbstractComplexDeviceComponentState pojo, AbstractComplexDeviceComponentStateMsg protoMsg) {
        map(pojo, protoMsg.getAbstractDeviceComponentState());
    }

    private void map(AbstractDeviceComponentDescriptor pojo, AbstractDeviceComponentDescriptorMsg protoMsg) {
        pojo.setProductionSpecification(protoMsg.getProductionSpecificationList()
                .stream().map(baseMapper::map).collect(Collectors.toList()));
        baseMapper.map(pojo, protoMsg.getAbstractDescriptor());
    }

    private void map(AbstractDeviceComponentState pojo, AbstractDeviceComponentStateMsg protoMsg) {
        pojo.setActivationState(Util.mapToPojoEnum(protoMsg, "AActivationState", ComponentActivation.class));
        pojo.setOperatingCycles(Util.optionalIntOfInt(protoMsg, "AOperatingCycles"));
        pojo.setOperatingHours(Util.optionalLongOfInt(protoMsg, "AOperatingHours"));
        instanceLogger.error("CalibrationInfo mapping is missing");
        instanceLogger.error("NextCalibration mapping is missing");
        instanceLogger.error("PhysicalConnectorInfo mapping is missing");
        baseMapper.map(pojo, protoMsg.getAbstractState());
    }
}
