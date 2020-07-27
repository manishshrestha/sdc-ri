package org.somda.sdc.proto.mapping;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.proto.model.biceps.*;
import org.somda.sdc.biceps.model.participant.*;

public class PojoToProtoNodeMapper {
    private static final Logger LOG = LogManager.getLogger(PojoToProtoNodeMapper.class);
    private final PojoToProtoBaseMapper baseMapper;

    @Inject
    PojoToProtoNodeMapper(PojoToProtoBaseMapper baseMapper) {

        this.baseMapper = baseMapper;
    }

    public AbstractStateOneOfMsg mapAbstractStateOneOf(AbstractState state) {
        var builder = AbstractStateOneOfMsg.newBuilder();
        if (state instanceof org.somda.sdc.biceps.model.participant.AbstractDeviceComponentState) {
            builder.setAbstractDeviceComponentStateOneOf(
                    mapAbstractDeviceComponentStateOneOf((AbstractDeviceComponentState) state));
        } else {
            LOG.error("Class {} not supported", state.getClass());
            //throw new IllegalArgumentException(String.format("Class %s not supported", state.getClass()));
        }

        return builder.build();
    }

    public AbstractDeviceComponentStateOneOfMsg mapAbstractDeviceComponentStateOneOf(
            AbstractDeviceComponentState state) {
        var builder = AbstractDeviceComponentStateOneOfMsg.newBuilder();
        if (state instanceof ChannelState) {
            builder.setChannelState(mapChannelState((ChannelState) state));
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
            builder.setMdsState(mapMdsState((MdsState) state));
        } else if (state instanceof VmdState) {
            builder.setVmdState(mapVmdState((VmdState) state));
        } else {
            LOG.error("Class {} not supported", state.getClass());
            // throw new IllegalArgumentException(String.format("Class %s not supported", state.getClass()));
        }

        return builder.build();
    }

    public MdsStateMsg mapMdsState(MdsState mdsState) {
        var builder = MdsStateMsg.newBuilder();
        builder.setAbstractComplexDeviceComponentState(mapAbstractComplexDeviceComponentState(mdsState));
        builder.setALang(Util.toStringValue(mdsState.getLang()));
        Util.doIfNotNull(mdsState.getOperatingMode(), mdsOperatingMode ->
                builder.setAOperatingMode(Util.mapToProtoEnum(mdsOperatingMode, MdsOperatingModeMsg.class)));

//                baseMapper.mapMdsOperatingMode(mdsOperatingMode)));

        Util.doIfNotNull(mdsState.getOperatingJurisdiction(), operatingJurisdiction ->
                builder.setOperatingJurisdiction(baseMapper.mapOperatingJurisdiction(operatingJurisdiction)));
        return builder.build();
    }

    public VmdStateMsg mapVmdState(VmdState vmdState) {
        var builder = VmdStateMsg.newBuilder();
        Util.doIfNotNull(vmdState.getOperatingJurisdiction(), operatingJurisdiction ->
                builder.setOperatingJurisdiction(baseMapper.mapOperatingJurisdiction(operatingJurisdiction)));
        return builder.setAbstractComplexDeviceComponentState(mapAbstractComplexDeviceComponentState(vmdState)).build();
    }

    public ChannelStateMsg mapChannelState(ChannelState channelState) {
        return ChannelStateMsg.newBuilder()
                .setAbstractDeviceComponentState(mapAbstractDeviceComponentState(channelState)).build();
    }

    public AbstractComplexDeviceComponentStateMsg mapAbstractComplexDeviceComponentState(
            AbstractComplexDeviceComponentState state) {
        return AbstractComplexDeviceComponentStateMsg.newBuilder()
                .setAbstractDeviceComponentState(mapAbstractDeviceComponentState(state)).build();
    }

    public AbstractDeviceComponentStateMsg mapAbstractDeviceComponentState(AbstractDeviceComponentState state) {
        var builder = AbstractDeviceComponentStateMsg.newBuilder();
        builder.setAbstractState(mapAbstractState(state));
        Util.doIfNotNull(state.getActivationState(), componentActivation ->
                builder.setAActivationState(Util.mapToProtoEnum(componentActivation, ComponentActivationMsg.class)));
        builder.setAOperatingHours(Util.toUInt32(state.getOperatingHours()));
        builder.setAOperatingCycles(Util.toInt32(state.getOperatingCycles()));
        // todo map CalibrationInfo
        // todo map NextCalibration
        // todo map PhysicalConnector
        return builder.build();
    }

    public AbstractStateMsg mapAbstractState(AbstractState abstractState) {
        var builder = AbstractStateMsg.newBuilder();
        Util.doIfNotNull(abstractState.getDescriptorHandle(), builder::setADescriptorHandle);
        builder.setADescriptorVersion(Util.toUInt64(abstractState.getDescriptorVersion()));
        builder.setAStateVersion(Util.toUInt64(abstractState.getStateVersion()));
        return builder.build();
    }

    public MdsDescriptorMsg.Builder mapMdsDesctriptor(MdsDescriptor mdsDescriptor) {
        return MdsDescriptorMsg.newBuilder()
                .setAbstractComplexDeviceComponentDescriptor(
                        mapAbstractComplexDeviceComponentDescriptor(mdsDescriptor));
    }

    public VmdDescriptorMsg.Builder mapVmdDescriptor(VmdDescriptor vmdDescriptor) {
        return VmdDescriptorMsg.newBuilder()
                .setAbstractComplexDeviceComponentDescriptor(
                        mapAbstractComplexDeviceComponentDescriptor(vmdDescriptor));
    }

    public ChannelDescriptorMsg.Builder mapChannelDescriptor(ChannelDescriptor channelDescriptor) {
        return ChannelDescriptorMsg.newBuilder()
                .setAbstractDeviceComponentDescriptor(
                        mapAbstractDeviceComponentDescriptor(channelDescriptor));
    }

    public AbstractComplexDeviceComponentDescriptorMsg mapAbstractComplexDeviceComponentDescriptor(
            AbstractComplexDeviceComponentDescriptor complexComponentDescriptor) {
        var builder = AbstractComplexDeviceComponentDescriptorMsg.newBuilder();
        builder.setAbstractDeviceComponentDescriptor(mapAbstractDeviceComponentDescriptor(complexComponentDescriptor));
        return builder.build();
    }

    public AbstractDeviceComponentDescriptorMsg mapAbstractDeviceComponentDescriptor(
            AbstractDeviceComponentDescriptor componentDescriptor) {
        var builder = AbstractDeviceComponentDescriptorMsg.newBuilder();
        builder.setAbstractDescriptor(mapAbstractDescriptor(componentDescriptor));
        componentDescriptor.getProductionSpecification().forEach(productionSpecification ->
                builder.addProductionSpecification(baseMapper.mapProductionSpecification(productionSpecification)));
        return builder.build();
    }

    public AbstractDescriptorMsg mapAbstractDescriptor(AbstractDescriptor abstractDescriptor) {
        var builder = AbstractDescriptorMsg.newBuilder();
        builder.setADescriptorVersion(Util.toUInt64(abstractDescriptor.getDescriptorVersion()));
        Util.doIfNotNull(abstractDescriptor.getHandle(), builder::setAHandle);
        Util.doIfNotNull(abstractDescriptor.getSafetyClassification(), safetyClassification ->
                builder.setASafetyClassification(Util.mapToProtoEnum(safetyClassification, SafetyClassificationMsg.class)));
        Util.doIfNotNull(abstractDescriptor.getType(), codedValue ->
                builder.setType(baseMapper.mapCodedValue(codedValue)));
        return builder.build();
    }
}
