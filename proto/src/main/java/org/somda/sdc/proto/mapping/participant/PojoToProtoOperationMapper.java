package org.somda.sdc.proto.mapping.participant;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.participant.AbstractOperationDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractOperationState;
import org.somda.sdc.biceps.model.participant.AbstractSetStateOperationDescriptor;
import org.somda.sdc.biceps.model.participant.ActivateOperationDescriptor;
import org.somda.sdc.biceps.model.participant.ActivateOperationState;
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
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.proto.mapping.Util;
import org.somda.sdc.proto.model.biceps.AbstractOperationDescriptorMsg;
import org.somda.sdc.proto.model.biceps.AbstractOperationStateMsg;
import org.somda.sdc.proto.model.biceps.AbstractSetStateOperationDescriptorMsg;
import org.somda.sdc.proto.model.biceps.ActivateOperationDescriptorMsg;
import org.somda.sdc.proto.model.biceps.ActivateOperationStateMsg;
import org.somda.sdc.proto.model.biceps.OperatingModeMsg;
import org.somda.sdc.proto.model.biceps.SetAlertStateOperationDescriptorMsg;
import org.somda.sdc.proto.model.biceps.SetAlertStateOperationStateMsg;
import org.somda.sdc.proto.model.biceps.SetComponentStateOperationDescriptorMsg;
import org.somda.sdc.proto.model.biceps.SetComponentStateOperationStateMsg;
import org.somda.sdc.proto.model.biceps.SetContextStateOperationDescriptorMsg;
import org.somda.sdc.proto.model.biceps.SetContextStateOperationStateMsg;
import org.somda.sdc.proto.model.biceps.SetMetricStateOperationDescriptorMsg;
import org.somda.sdc.proto.model.biceps.SetMetricStateOperationStateMsg;
import org.somda.sdc.proto.model.biceps.SetStringOperationDescriptorMsg;
import org.somda.sdc.proto.model.biceps.SetStringOperationStateMsg;
import org.somda.sdc.proto.model.biceps.SetValueOperationDescriptorMsg;
import org.somda.sdc.proto.model.biceps.SetValueOperationStateMsg;

import java.util.Optional;
import java.util.stream.Collectors;

public class PojoToProtoOperationMapper {
    private static final Logger LOG = LogManager.getLogger(PojoToProtoOperationMapper.class);
    private final Logger instanceLogger;
    private final PojoToProtoBaseMapper baseMapper;

    @Inject
    PojoToProtoOperationMapper(@Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier,
                               PojoToProtoBaseMapper baseMapper) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.baseMapper = baseMapper;
    }

    public ActivateOperationDescriptorMsg mapActivateOperationDescriptor(
            ActivateOperationDescriptor descriptor) {
        return ActivateOperationDescriptorMsg.newBuilder()
                .addAllArgument(descriptor.getArgument().stream().map(this::mapArgument).collect(Collectors.toList()))
                .setAbstractSetStateOperationDescriptor(mapAbstractSetStateOperationDescriptor(descriptor))
                .build();
    }

    public ActivateOperationStateMsg mapActivateOperationState(ActivateOperationState state) {
        return ActivateOperationStateMsg.newBuilder()
                .setAbstractOperationState(mapAbstractOperationState(state))
                .build();
    }

    public SetStringOperationDescriptorMsg mapSetStringOperationDescriptor(
            SetStringOperationDescriptor descriptor) {
        var builder = SetStringOperationDescriptorMsg.newBuilder();

        builder.setAbstractOperationDescriptor(mapAbstractOperationDescriptor(descriptor));
        Util.doIfNotNull(descriptor.getMaxLength(), it -> builder.setAMaxLength(Util.toUInt64(it)));

        return builder.build();
    }

    public SetStringOperationStateMsg mapSetStringOperationState(SetStringOperationState state) {
        var builder = SetStringOperationStateMsg.newBuilder();

        builder.setAbstractOperationState(mapAbstractOperationState(state));
        Util.doIfNotNull(state.getAllowedValues(), it -> builder.setAllowedValues(mapAllowedValues(it)));

        return builder.build();
    }

    public SetStringOperationStateMsg.AllowedValuesMsg mapAllowedValues(SetStringOperationState.AllowedValues values) {
        var builder = SetStringOperationStateMsg.AllowedValuesMsg.newBuilder();
        builder.addAllValue(values.getValue());
        return builder.build();
    }

    public SetValueOperationDescriptorMsg mapSetValueOperationDescriptor(
            SetValueOperationDescriptor descriptor) {
        return SetValueOperationDescriptorMsg.newBuilder()
                .setAbstractOperationDescriptor(mapAbstractOperationDescriptor(descriptor))
                .build();
    }

    public SetValueOperationStateMsg mapSetValueOperationState(SetValueOperationState state) {
        var builder = SetValueOperationStateMsg.newBuilder();

        builder.setAbstractOperationState(mapAbstractOperationState(state));
        state.getAllowedRange().forEach(it -> builder.addAllowedRange(baseMapper.mapRange(it)));

        return builder.build();
    }

    public SetMetricStateOperationDescriptorMsg mapSetMetricStateOperationDescriptor(
            SetMetricStateOperationDescriptor descriptor) {
        return SetMetricStateOperationDescriptorMsg.newBuilder()
                .setAbstractSetStateOperationDescriptor(mapAbstractSetStateOperationDescriptor(descriptor))
                .build();
    }

    public SetMetricStateOperationStateMsg mapSetMetricStateOperationState(SetMetricStateOperationState state) {
        return SetMetricStateOperationStateMsg.newBuilder()
                .setAbstractOperationState(mapAbstractOperationState(state))
                .build();
    }

    public SetComponentStateOperationDescriptorMsg mapSetComponentStateOperationDescriptor(
            SetComponentStateOperationDescriptor descriptor) {
        return SetComponentStateOperationDescriptorMsg.newBuilder()
                .setAbstractSetStateOperationDescriptor(mapAbstractSetStateOperationDescriptor(descriptor))
                .build();
    }

    public SetComponentStateOperationStateMsg mapSetComponentStateOperationState(SetComponentStateOperationState state) {
        return SetComponentStateOperationStateMsg.newBuilder()
                .setAbstractOperationState(mapAbstractOperationState(state))
                .build();
    }

    public SetAlertStateOperationDescriptorMsg mapSetAlertStateOperationDescriptor(
            SetAlertStateOperationDescriptor descriptor) {
        return SetAlertStateOperationDescriptorMsg.newBuilder()
                .setAbstractSetStateOperationDescriptor(mapAbstractSetStateOperationDescriptor(descriptor))
                .build();
    }

    public SetAlertStateOperationStateMsg mapSetAlertStateOperationState(SetAlertStateOperationState state) {
        return SetAlertStateOperationStateMsg.newBuilder()
                .setAbstractOperationState(mapAbstractOperationState(state))
                .build();
    }

    public SetContextStateOperationDescriptorMsg mapSetContextStateOperationDescriptor(
            SetContextStateOperationDescriptor descriptor) {
        return SetContextStateOperationDescriptorMsg.newBuilder()
                .setAbstractSetStateOperationDescriptor(mapAbstractSetStateOperationDescriptor(descriptor))
                .build();
    }

    public SetContextStateOperationStateMsg mapSetContextStateOperationState(SetContextStateOperationState state) {
        return SetContextStateOperationStateMsg.newBuilder()
                .setAbstractOperationState(mapAbstractOperationState(state))
                .build();
    }

    private AbstractSetStateOperationDescriptorMsg mapAbstractSetStateOperationDescriptor(
            AbstractSetStateOperationDescriptor descriptor) {
        var builder = AbstractSetStateOperationDescriptorMsg.newBuilder();
        builder.addAllModifiableData(descriptor.getModifiableData());
        builder.setAbstractOperationDescriptor(mapAbstractOperationDescriptor(descriptor));
        return builder.build();
    }

    private AbstractOperationDescriptorMsg mapAbstractOperationDescriptor(AbstractOperationDescriptor descriptor) {
        var builder = AbstractOperationDescriptorMsg.newBuilder();
        builder.setAbstractDescriptor(baseMapper.mapAbstractDescriptor(descriptor));
        Util.doIfNotNull(descriptor.getAccessLevel(), it ->
                builder.setAccessLevel(Util.mapToProtoEnum(it, AbstractOperationDescriptorMsg.AccessLevelMsg.class)));
        Util.doIfNotNull(descriptor.getInvocationEffectiveTimeout(), it ->
                builder.setAInvocationEffectiveTimeout(Util.fromJavaDuration(it)));
        Util.doIfNotNull(descriptor.getMaxTimeToFinish(), it ->
                builder.setAMaxTimeToFinish(Util.fromJavaDuration(it)));
        builder.setAOperationTarget(descriptor.getOperationTarget());
        Util.doIfNotNull(descriptor.isRetriggerable(), it ->
                builder.setARetriggerable(Util.toBoolValue(it)));
        return builder.build();
    }

    private AbstractOperationStateMsg mapAbstractOperationState(AbstractOperationState state) {
        var builder = AbstractOperationStateMsg.newBuilder();
        builder.setAbstractState(baseMapper.mapAbstractState(state));
        builder.setAOperatingMode(Util.mapToProtoEnum(state.getOperatingMode(), OperatingModeMsg.class));
        return builder.build();
    }

    private ActivateOperationDescriptorMsg.ArgumentMsg mapArgument(ActivateOperationDescriptor.Argument argument) {
        var builder = ActivateOperationDescriptorMsg.ArgumentMsg.newBuilder();
        builder.setArg(String.format("{%s}%s",
                Optional.ofNullable(argument.getArg().getNamespaceURI()).orElse(""),
                argument.getArg().getLocalPart()));
        builder.setArgName(baseMapper.mapCodedValue(argument.getArgName()));
        return builder.build();
    }
}
