package org.somda.sdc.proto.mapping;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.participant.AbstractComplexDeviceComponentDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractComplexDeviceComponentState;
import org.somda.sdc.biceps.model.participant.AbstractDeviceComponentDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractDeviceComponentState;
import org.somda.sdc.biceps.model.participant.ChannelDescriptor;
import org.somda.sdc.biceps.model.participant.ChannelState;
import org.somda.sdc.biceps.model.participant.MdsDescriptor;
import org.somda.sdc.biceps.model.participant.MdsState;
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
import org.somda.sdc.proto.model.biceps.ChannelDescriptorMsg;
import org.somda.sdc.proto.model.biceps.ChannelStateMsg;
import org.somda.sdc.proto.model.biceps.ComponentActivationMsg;
import org.somda.sdc.proto.model.biceps.MdsDescriptorMsg;
import org.somda.sdc.proto.model.biceps.MdsOperatingModeMsg;
import org.somda.sdc.proto.model.biceps.MdsStateMsg;
import org.somda.sdc.proto.model.biceps.SystemContextDescriptorMsg;
import org.somda.sdc.proto.model.biceps.SystemContextStateMsg;
import org.somda.sdc.proto.model.biceps.VmdDescriptorMsg;
import org.somda.sdc.proto.model.biceps.VmdStateMsg;

public class PojoToProtoComponentMapper {
    private static final Logger LOG = LogManager.getLogger(PojoToProtoComponentMapper.class);
    private final Logger instanceLogger;
    private final PojoToProtoBaseMapper baseMapper;

    @Inject
    PojoToProtoComponentMapper(@Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier,
                               PojoToProtoBaseMapper baseMapper) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.baseMapper = baseMapper;
    }

    public SystemContextDescriptorMsg.Builder mapSystemContextDescriptor(SystemContextDescriptor systemContextDescriptor) {
        return SystemContextDescriptorMsg.newBuilder()
                .setAbstractDeviceComponentDescriptor(
                        mapAbstractDeviceComponentDescriptor(systemContextDescriptor));
    }

    public SystemContextStateMsg mapSystemContextState(SystemContextState systemContextState) {
        return SystemContextStateMsg.newBuilder()
                .setAbstractDeviceComponentState(mapAbstractDeviceComponentState(systemContextState)).build();
    }

    public MdsDescriptorMsg.Builder mapMdsDescriptor(MdsDescriptor mdsDescriptor) {
        return MdsDescriptorMsg.newBuilder()
                .setAbstractComplexDeviceComponentDescriptor(
                        mapAbstractComplexDeviceComponentDescriptor(mdsDescriptor));
    }

    public MdsStateMsg mapMdsState(MdsState mdsState) {
        var builder = MdsStateMsg.newBuilder();
        builder.setAbstractComplexDeviceComponentState(mapAbstractComplexDeviceComponentState(mdsState));
        builder.setALang(Util.toStringValue(mdsState.getLang()));
        Util.doIfNotNull(mdsState.getOperatingMode(), mdsOperatingMode ->
                builder.setAOperatingMode(Util.mapToProtoEnum(mdsOperatingMode, MdsOperatingModeMsg.class)));
        Util.doIfNotNull(mdsState.getOperatingJurisdiction(), operatingJurisdiction ->
                builder.setOperatingJurisdiction(baseMapper.mapOperatingJurisdiction(operatingJurisdiction)));
        return builder.build();
    }

    public VmdDescriptorMsg.Builder mapVmdDescriptor(VmdDescriptor vmdDescriptor) {
        return VmdDescriptorMsg.newBuilder()
                .setAbstractComplexDeviceComponentDescriptor(
                        mapAbstractComplexDeviceComponentDescriptor(vmdDescriptor));
    }

    public VmdStateMsg mapVmdState(VmdState vmdState) {
        var builder = VmdStateMsg.newBuilder();
        Util.doIfNotNull(vmdState.getOperatingJurisdiction(), operatingJurisdiction ->
                builder.setOperatingJurisdiction(baseMapper.mapOperatingJurisdiction(operatingJurisdiction)));
        return builder.setAbstractComplexDeviceComponentState(mapAbstractComplexDeviceComponentState(vmdState)).build();
    }

    public ChannelDescriptorMsg.Builder mapChannelDescriptor(ChannelDescriptor channelDescriptor) {
        return ChannelDescriptorMsg.newBuilder()
                .setAbstractDeviceComponentDescriptor(
                        mapAbstractDeviceComponentDescriptor(channelDescriptor));
    }

    public ChannelStateMsg mapChannelState(ChannelState channelState) {
        return ChannelStateMsg.newBuilder()
                .setAbstractDeviceComponentState(mapAbstractDeviceComponentState(channelState)).build();
    }

    public AbstractDeviceComponentDescriptorMsg mapAbstractDeviceComponentDescriptor(
            AbstractDeviceComponentDescriptor componentDescriptor) {
        var builder = AbstractDeviceComponentDescriptorMsg.newBuilder();
        builder.setAbstractDescriptor(baseMapper.mapAbstractDescriptor(componentDescriptor));
        componentDescriptor.getProductionSpecification().forEach(productionSpecification ->
                builder.addProductionSpecification(baseMapper.mapProductionSpecification(productionSpecification)));
        return builder.build();
    }

    private AbstractDeviceComponentStateMsg mapAbstractDeviceComponentState(AbstractDeviceComponentState state) {
        var builder = AbstractDeviceComponentStateMsg.newBuilder();
        builder.setAbstractState(baseMapper.mapAbstractState(state));
        Util.doIfNotNull(state.getActivationState(), componentActivation ->
                builder.setAActivationState(Util.mapToProtoEnum(componentActivation, ComponentActivationMsg.class)));
        Util.doIfNotNull(state.getOperatingHours(), it -> builder.setAOperatingHours(Util.toUInt32(it)));
        Util.doIfNotNull(state.getOperatingCycles(), it -> builder.setAOperatingCycles(Util.toInt32(it)));
        // todo map CalibrationInfo
        // todo map NextCalibration
        // todo map PhysicalConnector
        return builder.build();
    }

    private AbstractComplexDeviceComponentDescriptorMsg mapAbstractComplexDeviceComponentDescriptor(
            AbstractComplexDeviceComponentDescriptor complexComponentDescriptor) {
        var builder = AbstractComplexDeviceComponentDescriptorMsg.newBuilder();
        builder.setAbstractDeviceComponentDescriptor(mapAbstractDeviceComponentDescriptor(complexComponentDescriptor));
        return builder.build();
    }

    private AbstractComplexDeviceComponentStateMsg mapAbstractComplexDeviceComponentState(
            AbstractComplexDeviceComponentState state) {
        return AbstractComplexDeviceComponentStateMsg.newBuilder()
                .setAbstractDeviceComponentState(mapAbstractDeviceComponentState(state)).build();
    }
}
