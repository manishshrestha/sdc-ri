package org.somda.sdc.proto.mapping.participant;

import com.google.common.base.Splitter;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.BoolValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.participant.AbstractOperationDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractOperationState;
import org.somda.sdc.biceps.model.participant.AbstractSetStateOperationDescriptor;
import org.somda.sdc.biceps.model.participant.ActivateOperationDescriptor;
import org.somda.sdc.biceps.model.participant.ActivateOperationState;
import org.somda.sdc.biceps.model.participant.OperatingMode;
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

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class ProtoToPojoOperationMapper {
    private static final Logger LOG = LogManager.getLogger(ProtoToPojoOperationMapper.class);
    private final Logger instanceLogger;
    private final ProtoToPojoBaseMapper baseMapper;

    @Inject
    ProtoToPojoOperationMapper(@Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier,
                               ProtoToPojoBaseMapper baseMapper) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.baseMapper = baseMapper;
    }

    public ActivateOperationDescriptor map(ActivateOperationDescriptorMsg protoMsg) {
        var pojo = new ActivateOperationDescriptor();
        map(pojo, protoMsg.getAbstractSetStateOperationDescriptor());
        pojo.setArgument(protoMsg.getArgumentList().stream().map(this::map).collect(Collectors.toList()));
        return pojo;
    }

    public ActivateOperationState map(ActivateOperationStateMsg protoMsg) {
        var pojo = new ActivateOperationState();
        map(pojo, protoMsg.getAbstractOperationState());
        return pojo;
    }

    public SetStringOperationDescriptor map(SetStringOperationDescriptorMsg protoMsg) {
        var pojo = new SetStringOperationDescriptor();
        map(pojo, protoMsg.getAbstractOperationDescriptor());
        Util.doIfNotNull(Util.optionalBigIntOfLong(protoMsg, "AMaxLength"), pojo::setMaxLength);
        return pojo;
    }

    public SetValueOperationState map(SetValueOperationStateMsg protoMsg) {
        var pojo = new SetValueOperationState();
        map(pojo, protoMsg.getAbstractOperationState());
        return pojo;
    }

    public SetValueOperationDescriptor map(SetValueOperationDescriptorMsg protoMsg) {
        var pojo = new SetValueOperationDescriptor();
        map(pojo, protoMsg.getAbstractOperationDescriptor());
        return pojo;
    }

    public SetStringOperationState map(SetStringOperationStateMsg protoMsg) {
        var pojo = new SetStringOperationState();
        map(pojo, protoMsg.getAbstractOperationState());
        Util.doIfNotNull(
                Util.optional(protoMsg, "AllowedValues", SetStringOperationStateMsg.AllowedValuesMsg.class),
                it -> pojo.setAllowedValues(map(it))
        );
        return pojo;
    }

    public SetStringOperationState.AllowedValues map(SetStringOperationStateMsg.AllowedValuesMsg protoMsg) {
        var pojo = new SetStringOperationState.AllowedValues();
        pojo.setValue(new ArrayList<>(protoMsg.getValueList()));
        return pojo;
    }

    public SetMetricStateOperationDescriptor map(SetMetricStateOperationDescriptorMsg protoMsg) {
        var pojo = new SetMetricStateOperationDescriptor();
        map(pojo, protoMsg.getAbstractSetStateOperationDescriptor());
        return pojo;
    }

    public SetMetricStateOperationState map(SetMetricStateOperationStateMsg protoMsg) {
        var pojo = new SetMetricStateOperationState();
        map(pojo, protoMsg.getAbstractOperationState());
        return pojo;
    }

    public SetComponentStateOperationDescriptor map(SetComponentStateOperationDescriptorMsg protoMsg) {
        var pojo = new SetComponentStateOperationDescriptor();
        map(pojo, protoMsg.getAbstractSetStateOperationDescriptor());
        return pojo;
    }

    public SetComponentStateOperationState map(SetComponentStateOperationStateMsg protoMsg) {
        var pojo = new SetComponentStateOperationState();
        map(pojo, protoMsg.getAbstractOperationState());
        return pojo;
    }

    public SetContextStateOperationDescriptor map(SetContextStateOperationDescriptorMsg protoMsg) {
        var pojo = new SetContextStateOperationDescriptor();
        map(pojo, protoMsg.getAbstractSetStateOperationDescriptor());
        return pojo;
    }

    public SetContextStateOperationState map(SetContextStateOperationStateMsg protoMsg) {
        var pojo = new SetContextStateOperationState();
        map(pojo, protoMsg.getAbstractOperationState());
        return pojo;
    }

    public SetAlertStateOperationDescriptor map(SetAlertStateOperationDescriptorMsg protoMsg) {
        var pojo = new SetAlertStateOperationDescriptor();
        map(pojo, protoMsg.getAbstractSetStateOperationDescriptor());
        return pojo;
    }

    public SetAlertStateOperationState map(SetAlertStateOperationStateMsg protoMsg) {
        var pojo = new SetAlertStateOperationState();
        map(pojo, protoMsg.getAbstractOperationState());
        return pojo;
    }

    private void map(AbstractSetStateOperationDescriptor pojo, AbstractSetStateOperationDescriptorMsg protoMsg) {
        pojo.getModifiableData().addAll(protoMsg.getModifiableDataList());
        map(pojo, protoMsg.getAbstractOperationDescriptor());
    }

    private void map(AbstractOperationDescriptor pojo, AbstractOperationDescriptorMsg protoMsg) {
        pojo.setAccessLevel(Util.mapToPojoEnum(protoMsg, "AccessLevel",
                AbstractOperationDescriptor.AccessLevel.class));
        pojo.setOperationTarget(protoMsg.getAOperationTarget());
        if (protoMsg.hasAInvocationEffectiveTimeout()) {
            pojo.setInvocationEffectiveTimeout(Util.fromProtoDuration(protoMsg.getAInvocationEffectiveTimeout()));
        }
        if (protoMsg.hasAMaxTimeToFinish()) {
            pojo.setMaxTimeToFinish(Util.fromProtoDuration(protoMsg.getAMaxTimeToFinish()));
        }

        Util.doIfNotNull(Util.optional(protoMsg, "ARetriggerable", BoolValue.class), boolValue ->
                pojo.setRetriggerable(boolValue.getValue()));

        baseMapper.map(pojo, protoMsg.getAbstractDescriptor());
    }

    private void map(AbstractOperationState pojo, AbstractOperationStateMsg protoMsg) {
        pojo.setOperatingMode(Util.mapToPojoEnum(protoMsg, "AOperatingMode", OperatingMode.class));
        baseMapper.map(pojo, protoMsg.getAbstractState());
    }

    private ActivateOperationDescriptor.Argument map(ActivateOperationDescriptorMsg.ArgumentMsg protoMsg) {
        var arg = new ActivateOperationDescriptor.Argument();
        var argName = protoMsg.getArg();
        argName = argName.substring(1);
        var split = Splitter.on('}').splitToList(argName);
        if (split.size() != 2) {
            throw new ArrayIndexOutOfBoundsException(String.format("Split QName was malformed: %s", protoMsg.getArg()));
        }
        arg.setArg(new QName(split.get(0), split.get(1)));
        arg.setArgName(baseMapper.map(protoMsg.getArgName()));
        return arg;
    }
}
