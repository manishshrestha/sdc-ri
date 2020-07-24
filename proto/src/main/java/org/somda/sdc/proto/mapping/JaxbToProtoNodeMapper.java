package org.somda.sdc.proto.mapping;

import com.google.inject.Inject;
import org.somda.sdc.biceps.model.participant.AbstractComplexDeviceComponentState;
import org.somda.sdc.biceps.model.participant.AbstractDeviceComponentState;
import org.somda.sdc.biceps.model.participant.AbstractState;
import org.somda.sdc.biceps.model.participant.ChannelState;
import org.somda.sdc.biceps.model.participant.MdsState;
import org.somda.sdc.biceps.model.participant.VmdState;
import org.somda.sdc.proto.model.biceps.AbstractComplexDeviceComponentStateOneOfType;
import org.somda.sdc.proto.model.biceps.AbstractComplexDeviceComponentStateType;
import org.somda.sdc.proto.model.biceps.AbstractDeviceComponentStateOneOfType;
import org.somda.sdc.proto.model.biceps.AbstractDeviceComponentStateType;
import org.somda.sdc.proto.model.biceps.AbstractStateOneOfType;
import org.somda.sdc.proto.model.biceps.AbstractStateType;
import org.somda.sdc.proto.model.biceps.ChannelStateType;
import org.somda.sdc.proto.model.biceps.MdsStateType;
import org.somda.sdc.proto.model.biceps.VmdStateType;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class JaxbToProtoNodeMapper {

    private final JaxbToProtoBaseMapper baseMapper;

    @Inject
    JaxbToProtoNodeMapper(JaxbToProtoBaseMapper baseMapper) {

        this.baseMapper = baseMapper;
    }

    public AbstractStateOneOfType.AbstractStateOneOf mapAbstractStateOneOf(AbstractState state) {
        var builder = AbstractStateOneOfType.AbstractStateOneOf.newBuilder();
        if (state instanceof AbstractDeviceComponentState) {
            builder.setAbstractDeviceComponentStateOneOf(
                    mapAbstractDeviceComponentStateOneOf((AbstractDeviceComponentState) state));
        } else {
            throw new IllegalArgumentException(String.format("Class %s not supported", state.getClass()));
        }

        return builder.build();
    }

    public AbstractDeviceComponentStateOneOfType.AbstractDeviceComponentStateOneOf
    mapAbstractDeviceComponentStateOneOf(AbstractDeviceComponentState state) {
        var builder = AbstractDeviceComponentStateOneOfType.AbstractDeviceComponentStateOneOf.newBuilder();
        if (state instanceof ChannelState) {
            builder.setChannelState(mapChannelState((ChannelState) state));
        } else if (state instanceof AbstractComplexDeviceComponentState) {
            builder.setAbstractComplexDeviceComponentStateOneOf(
                    mapAbstractComplexDeviceComponentStateOneOf((AbstractComplexDeviceComponentState) state));
        } else {
            throw new IllegalArgumentException(String.format("Class %s not supported", state.getClass()));
        }

        return builder.build();
    }

    public AbstractComplexDeviceComponentStateOneOfType.AbstractComplexDeviceComponentStateOneOf
    mapAbstractComplexDeviceComponentStateOneOf(AbstractComplexDeviceComponentState state) {
        var builder = AbstractComplexDeviceComponentStateOneOfType.AbstractComplexDeviceComponentStateOneOf.newBuilder();
        if (state instanceof MdsState) {
            builder.setMdsState(mapMdsState((MdsState) state));
        } else if (state instanceof VmdState) {
            builder.setVmdState(mapVmdState((VmdState) state));
        } else {
            throw new IllegalArgumentException(String.format("Class %s not supported", state.getClass()));
        }

        return builder.build();
    }

    public MdsStateType.MdsState mapMdsState(MdsState mdsState) {
        var builder = MdsStateType.MdsState.newBuilder();
        builder.setAbstractComplexDeviceComponentState(mapAbstractComplexDeviceComponentState(mdsState));
        setOptional(mdsState.getLang(), builder::setALang);
        setOptional(mdsState.getOperatingMode(), mdsOperatingMode ->
                builder.setAOperatingMode(baseMapper.mapMdsOperatingMode(mdsOperatingMode)));
        setOptional(mdsState.getLang(), builder::setALang);
        setOptional(mdsState.getOperatingJurisdiction(), operatingJurisdiction ->
                builder.setOperatingJurisdiction(baseMapper.mapOperatingJurisdiction(operatingJurisdiction)));
        return builder.build();
    }

    public VmdStateType.VmdState mapVmdState(VmdState vmdState) {
        return VmdStateType.VmdState.newBuilder()
                .setAbstractComplexDeviceComponentState(mapAbstractComplexDeviceComponentState(vmdState)).build();
    }

    public ChannelStateType.ChannelState mapChannelState(ChannelState channelState) {
        return ChannelStateType.ChannelState.newBuilder()
                .setAbstractDeviceComponentState(mapAbstractDeviceComponentState(channelState)).build();
    }

    public AbstractComplexDeviceComponentStateType.AbstractComplexDeviceComponentState
    mapAbstractComplexDeviceComponentState(AbstractComplexDeviceComponentState state) {
        return AbstractComplexDeviceComponentStateType.AbstractComplexDeviceComponentState.newBuilder()
                .setAbstractDeviceComponentState(mapAbstractDeviceComponentState(state)).build();
    }

    public AbstractDeviceComponentStateType.AbstractDeviceComponentState
    mapAbstractDeviceComponentState(AbstractDeviceComponentState state) {
        var builder = AbstractDeviceComponentStateType.AbstractDeviceComponentState.newBuilder();
        builder.setAbstractState(mapAbstractState(state));
        setOptional(state.getActivationState(), componentActivation ->
                builder.setAActivationState(baseMapper.mapComponentActivation(componentActivation)));
        setOptional(state.getOperatingCycles(), builder::setAOperatingHours);
        // todo map CalibrationInfo
        // todo map NextCalibration
        // todo map PhysicalConnector
        return builder.build();
    }

    public AbstractStateType.AbstractState mapAbstractState(AbstractState abstractState) {
        var builder = AbstractStateType.AbstractState.newBuilder();
        setOptional(abstractState.getDescriptorHandle(), builder::setADescriptorHandle);
        setOptional(abstractState.getDescriptorVersion().longValue(), builder::setADescriptorVersion);
        setOptional(abstractState.getStateVersion().longValue(), builder::setAStateVersion);
        return builder.build();
    }

    private <T> void setOptional(@Nullable T value, Consumer<T> consumer) {
        if (value != null) {
            consumer.accept(value);
        }
    }
}
