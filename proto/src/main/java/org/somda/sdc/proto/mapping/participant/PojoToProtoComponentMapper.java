package org.somda.sdc.proto.mapping.participant;

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
import org.somda.sdc.biceps.model.participant.MdsDescriptor;
import org.somda.sdc.biceps.model.participant.MdsState;
import org.somda.sdc.biceps.model.participant.ScoDescriptor;
import org.somda.sdc.biceps.model.participant.ScoState;
import org.somda.sdc.biceps.model.participant.SystemContextDescriptor;
import org.somda.sdc.biceps.model.participant.SystemContextState;
import org.somda.sdc.biceps.model.participant.VmdDescriptor;
import org.somda.sdc.biceps.model.participant.VmdState;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.proto.mapping.Util;
import org.somda.sdc.proto.model.biceps.AbstractComplexDeviceComponentDescriptorMsg;
import org.somda.sdc.proto.model.biceps.AbstractComplexDeviceComponentStateMsg;
import org.somda.sdc.proto.model.biceps.AbstractDeviceComponentDescriptorMsg;
import org.somda.sdc.proto.model.biceps.AbstractDeviceComponentStateMsg;
import org.somda.sdc.proto.model.biceps.ApprovedJurisdictionsMsg;
import org.somda.sdc.proto.model.biceps.ChannelDescriptorMsg;
import org.somda.sdc.proto.model.biceps.ChannelStateMsg;
import org.somda.sdc.proto.model.biceps.ComponentActivationMsg;
import org.somda.sdc.proto.model.biceps.MdsDescriptorMsg;
import org.somda.sdc.proto.model.biceps.MdsOperatingModeMsg;
import org.somda.sdc.proto.model.biceps.MdsStateMsg;
import org.somda.sdc.proto.model.biceps.OperationRefMsg;
import org.somda.sdc.proto.model.biceps.ScoDescriptorMsg;
import org.somda.sdc.proto.model.biceps.ScoStateMsg;
import org.somda.sdc.proto.model.biceps.SystemContextDescriptorMsg;
import org.somda.sdc.proto.model.biceps.SystemContextStateMsg;
import org.somda.sdc.proto.model.biceps.VmdDescriptorMsg;
import org.somda.sdc.proto.model.biceps.VmdStateMsg;

import java.util.List;

public class PojoToProtoComponentMapper {
    private static final Logger LOG = LogManager.getLogger(PojoToProtoComponentMapper.class);
    private final Logger instanceLogger;
    private final PojoToProtoBaseMapper baseMapper;
    private final PojoToProtoAlertMapper alertMapper;

    @Inject
    PojoToProtoComponentMapper(@Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier,
                               PojoToProtoBaseMapper baseMapper,
                               PojoToProtoAlertMapper alertMapper) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.baseMapper = baseMapper;
        this.alertMapper = alertMapper;
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

    public ScoDescriptorMsg.Builder mapScoDescriptor(ScoDescriptor scoDescriptor) {
        return ScoDescriptorMsg.newBuilder()
                .setAbstractDeviceComponentDescriptor(mapAbstractDeviceComponentDescriptor(scoDescriptor));
    }

    public ScoStateMsg mapScoState(ScoState scoState) {
        var builder = ScoStateMsg.newBuilder();
        if (!scoState.getInvocationRequested().isEmpty()) {
            builder.setAInvocationRequested(mapOperationRefs(scoState.getInvocationRequested()));
        }
        if (!scoState.getInvocationRequired().isEmpty()) {
            builder.setAInvocationRequired(mapOperationRefs(scoState.getInvocationRequired()));
        }
        return builder.setAbstractDeviceComponentState(mapAbstractDeviceComponentState(scoState)).build();
    }

    public OperationRefMsg mapOperationRefs(List<String> opRefs) {
        return OperationRefMsg.newBuilder().addAllOperationRef(opRefs).build();
    }

    public MdsDescriptorMsg.Builder mapMdsDescriptor(MdsDescriptor mdsDescriptor) {
        var builder = MdsDescriptorMsg.newBuilder();
        Util.doIfNotNull(mdsDescriptor.getApprovedJurisdictions(), it ->
                builder.setApprovedJurisdictions(mapApprovedJurisdictions(it)));
        return builder.setAbstractComplexDeviceComponentDescriptor(
                mapAbstractComplexDeviceComponentDescriptor(mdsDescriptor));
    }

    public MdsStateMsg mapMdsState(MdsState mdsState) {
        var builder = MdsStateMsg.newBuilder();
        builder.setAbstractComplexDeviceComponentState(mapAbstractComplexDeviceComponentState(mdsState));
        Util.doIfNotNull(mdsState.getLang(), it ->
                builder.setALang(Util.toStringValue(mdsState.getLang())));
        Util.doIfNotNull(mdsState.getOperatingMode(), mdsOperatingMode ->
                builder.setAOperatingMode(Util.mapToProtoEnum(mdsOperatingMode, MdsOperatingModeMsg.class)));
        Util.doIfNotNull(mdsState.getOperatingJurisdiction(), operatingJurisdiction ->
                builder.setOperatingJurisdiction(baseMapper.mapOperatingJurisdiction(operatingJurisdiction)));
        return builder.build();
    }

    public VmdDescriptorMsg.Builder mapVmdDescriptor(VmdDescriptor vmdDescriptor) {
        var builder = VmdDescriptorMsg.newBuilder();
        Util.doIfNotNull(vmdDescriptor.getApprovedJurisdictions(), it ->
                builder.setApprovedJurisdictions(mapApprovedJurisdictions(it)));
        return builder.setAbstractComplexDeviceComponentDescriptor(
                mapAbstractComplexDeviceComponentDescriptor(vmdDescriptor));
    }

    public VmdStateMsg mapVmdState(VmdState vmdState) {
        var builder = VmdStateMsg.newBuilder();
        Util.doIfNotNull(vmdState.getOperatingJurisdiction(), operatingJurisdiction ->
                builder.setOperatingJurisdiction(baseMapper.mapOperatingJurisdiction(operatingJurisdiction)));
        return builder.setAbstractComplexDeviceComponentState(mapAbstractComplexDeviceComponentState(vmdState)).build();
    }

    private ApprovedJurisdictionsMsg mapApprovedJurisdictions(ApprovedJurisdictions approvedJurisdictions) {
        return ApprovedJurisdictionsMsg.newBuilder()
                .addAllApprovedJurisdiction(baseMapper
                        .mapInstanceIdentifiers(approvedJurisdictions.getApprovedJurisdiction())).build();

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

    private AbstractComplexDeviceComponentDescriptorMsg.Builder mapAbstractComplexDeviceComponentDescriptor(
            AbstractComplexDeviceComponentDescriptor complexComponentDescriptor) {
        var builder = AbstractComplexDeviceComponentDescriptorMsg.newBuilder();
        builder.setAbstractDeviceComponentDescriptor(mapAbstractDeviceComponentDescriptor(complexComponentDescriptor));
        return builder;
    }

    private AbstractComplexDeviceComponentStateMsg mapAbstractComplexDeviceComponentState(
            AbstractComplexDeviceComponentState state) {
        return AbstractComplexDeviceComponentStateMsg.newBuilder()
                .setAbstractDeviceComponentState(mapAbstractDeviceComponentState(state)).build();
    }
}
