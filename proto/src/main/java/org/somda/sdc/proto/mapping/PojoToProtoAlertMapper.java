package org.somda.sdc.proto.mapping;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.participant.AbstractAlertDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractAlertState;
import org.somda.sdc.biceps.model.participant.AlertSystemDescriptor;
import org.somda.sdc.biceps.model.participant.AlertSystemState;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.common.util.TimestampAdapter;
import org.somda.sdc.proto.model.biceps.AbstractAlertDescriptorMsg;
import org.somda.sdc.proto.model.biceps.AbstractAlertStateMsg;
import org.somda.sdc.proto.model.biceps.AlertActivationMsg;
import org.somda.sdc.proto.model.biceps.AlertConditionReferenceMsg;
import org.somda.sdc.proto.model.biceps.AlertSystemDescriptorMsg;
import org.somda.sdc.proto.model.biceps.AlertSystemStateMsg;

import java.math.BigInteger;
import java.util.List;

public class PojoToProtoAlertMapper {
    private static final Logger LOG = LogManager.getLogger(PojoToProtoAlertMapper.class);
    private final Logger instanceLogger;
    private final PojoToProtoBaseMapper baseMapper;
    private final TimestampAdapter timestampAdapter;

    @Inject
    PojoToProtoAlertMapper(@Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier,
                           PojoToProtoBaseMapper baseMapper,
                           TimestampAdapter timestampAdapter) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.baseMapper = baseMapper;
        this.timestampAdapter = timestampAdapter;
    }

    public AlertSystemDescriptorMsg mapAlertSystemDescriptor(AlertSystemDescriptor descriptor) {
        var builder = AlertSystemDescriptorMsg.newBuilder()
                .setAbstractAlertDescriptor(mapAbstractAlertDescriptor(descriptor));

        Util.doIfNotNull(descriptor.getMaxPhysiologicalParallelAlarms(), limit ->
                builder.setAMaxPhysiologicalParallelAlarms(Util.toUInt32(limit)));
        Util.doIfNotNull(descriptor.getMaxTechnicalParallelAlarms(), limit ->
                builder.setAMaxTechnicalParallelAlarms(Util.toUInt32(limit)));
        Util.doIfNotNull(descriptor.getSelfCheckPeriod(), period ->
                builder.setASelfCheckPeriod(Util.fromJavaDuration(period)));

        // these are handled by the tree mapper?
//        builder.setAlertCondition();
//        builder.setAlertSignal();
        return builder.build();
    }

    public AlertSystemStateMsg mapAlertSystemState(AlertSystemState state) {
        var builder = AlertSystemStateMsg.newBuilder()
                .setAbstractAlertState(mapAbstractAlertState(state));

        Util.doIfNotNull(state.getLastSelfCheck(), timestamp ->
                builder.setALastSelfCheck(Util.toUInt64(timestampAdapter.marshal(timestamp))));
        Util.doIfNotNull(state.getSelfCheckCount(), count ->
                builder.setASelfCheckCount(Util.toInt64(count)));
        builder.setAPresentPhysiologicalAlarmConditions(mapAlertConditionReference(state.getPresentPhysiologicalAlarmConditions()));
        builder.setAPresentTechnicalAlarmConditions(mapAlertConditionReference(state.getPresentTechnicalAlarmConditions()));

        return builder.build();
    }

    public AbstractAlertDescriptorMsg mapAbstractAlertDescriptor(AbstractAlertDescriptor descriptor) {
        var builder = AbstractAlertDescriptorMsg.newBuilder()
                .setAbstractDescriptor(baseMapper.mapAbstractDescriptor(descriptor));
        return builder.build();
    }

    private AbstractAlertStateMsg mapAbstractAlertState(AbstractAlertState state) {
        var builder = AbstractAlertStateMsg.newBuilder()
                .setAbstractState(baseMapper.mapAbstractState(state));
        builder.setAActivationState(Util.mapToProtoEnum(state.getActivationState(), AlertActivationMsg.class));
        return builder.build();
    }

    private AlertConditionReferenceMsg mapAlertConditionReference(List<String> references) {
        var builder = AlertConditionReferenceMsg.newBuilder();
        references.forEach(builder::addAlertConditionReference);
        return builder.build();
    }
}
