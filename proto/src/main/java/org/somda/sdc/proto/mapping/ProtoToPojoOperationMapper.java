package org.somda.sdc.proto.mapping;

import com.google.common.base.Splitter;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.participant.AbstractOperationDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractOperationState;
import org.somda.sdc.biceps.model.participant.AbstractSetStateOperationDescriptor;
import org.somda.sdc.biceps.model.participant.ActivateOperationDescriptor;
import org.somda.sdc.biceps.model.participant.ActivateOperationState;
import org.somda.sdc.biceps.model.participant.OperatingMode;
import org.somda.sdc.biceps.model.participant.SetMetricStateOperationDescriptor;
import org.somda.sdc.biceps.model.participant.SetMetricStateOperationState;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.proto.model.biceps.AbstractOperationDescriptorMsg;
import org.somda.sdc.proto.model.biceps.AbstractOperationStateMsg;
import org.somda.sdc.proto.model.biceps.AbstractSetStateOperationDescriptorMsg;
import org.somda.sdc.proto.model.biceps.ActivateOperationDescriptorMsg;
import org.somda.sdc.proto.model.biceps.ActivateOperationStateMsg;
import org.somda.sdc.proto.model.biceps.SetMetricStateOperationDescriptorMsg;
import org.somda.sdc.proto.model.biceps.SetMetricStateOperationStateMsg;

import javax.xml.namespace.QName;
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

        pojo.setRetriggerable(Util.optional(protoMsg, "ARetriggerable", Boolean.class));

        baseMapper.map(pojo, protoMsg.getAbstractDescriptor());
    }

    private void map(AbstractOperationState pojo, AbstractOperationStateMsg protoMsg) {
        pojo.setOperatingMode(Util.mapToPojoEnum(protoMsg, "AOperatingMode", OperatingMode.class));
        baseMapper.map(pojo, protoMsg.getAbstractState());
    }

    private ActivateOperationDescriptor.Argument map(ActivateOperationDescriptorMsg.ArgumentMsg protoMsg) {
        var arg = new ActivateOperationDescriptor.Argument();
        var argName = protoMsg.getArg();
        argName = argName.substring(1, argName.length() - 1);
        var split = Splitter.on(':').splitToList(argName);
        if (split.size() != 2) {
            throw new ArrayIndexOutOfBoundsException(String.format("Split QName was malformed: %s", protoMsg.getArg()));
        }
        arg.setArg(new QName(split.get(0), split.get(1)));
        arg.setArgName(baseMapper.map(protoMsg.getArgName()));
        return arg;
    }
}
