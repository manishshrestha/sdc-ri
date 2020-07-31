package org.somda.sdc.proto.mapping.participant;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.participant.AbstractAlertDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractAlertState;
import org.somda.sdc.biceps.model.participant.AlertConditionDescriptor;
import org.somda.sdc.biceps.model.participant.AlertConditionState;
import org.somda.sdc.biceps.model.participant.AlertSignalDescriptor;
import org.somda.sdc.biceps.model.participant.AlertSignalState;
import org.somda.sdc.biceps.model.participant.AlertSystemDescriptor;
import org.somda.sdc.biceps.model.participant.AlertSystemState;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.common.util.TimestampAdapter;
import org.somda.sdc.proto.mapping.Util;
import org.somda.sdc.proto.model.biceps.*;

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

    public AlertSystemDescriptorMsg.Builder mapAlertSystemDescriptor(AlertSystemDescriptor descriptor) {
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
        return builder;
    }

    public AlertConditionDescriptorMsg mapAlertConditionDescriptor(AlertConditionDescriptor descriptor) {
        var builder = AlertConditionDescriptorMsg.newBuilder()
                .setAbstractAlertDescriptor(mapAbstractAlertDescriptor(descriptor));

        builder.setAKind(Util.mapToProtoEnum(descriptor.getKind(), AlertConditionKindMsg.class));
        builder.setAPriority(Util.mapToProtoEnum(descriptor.getPriority(), AlertConditionPriorityMsg.class));
        Util.doIfNotNull(descriptor.getDefaultConditionGenerationDelay(), delay ->
                builder.setADefaultConditionGenerationDelay(Util.fromJavaDuration(delay)));
        Util.doIfNotNull(descriptor.getCanEscalate(), escalate ->
                builder.setCanEscalate(Util.mapToProtoEnum(escalate, AlertConditionDescriptorMsg.CanEscalateMsg.class)));
        Util.doIfNotNull(descriptor.getCanDeescalate(), escalate ->
                builder.setCanDeescalate(Util.mapToProtoEnum(escalate, AlertConditionDescriptorMsg.CanDeescalateMsg.class)));
        descriptor.getSource().forEach(builder::addSource);

        // TODO
//        builder.addCauseInfo()

        return builder.build();
    }

    public AlertSignalDescriptorMsg mapAlertSignalDescriptor(AlertSignalDescriptor descriptor) {
        var builder = AlertSignalDescriptorMsg.newBuilder()
                .setAbstractAlertDescriptor(mapAbstractAlertDescriptor(descriptor));

        Util.doIfNotNull(descriptor.getConditionSignaled(), condition ->
                builder.setAConditionSignaled(Util.toStringValue(condition)));
        builder.setAManifestation(Util.mapToProtoEnum(descriptor.getManifestation(), AlertSignalManifestationMsg.class));
        builder.setALatching(descriptor.isLatching());
        Util.doIfNotNull(descriptor.getDefaultSignalGenerationDelay(), duration ->
                builder.setADefaultSignalGenerationDelay(Util.fromJavaDuration(duration)));
        Util.doIfNotNull(descriptor.getMinSignalGenerationDelay(), duration ->
                builder.setAMinSignalGenerationDelay(Util.fromJavaDuration(duration)));
        Util.doIfNotNull(descriptor.getMaxSignalGenerationDelay(), duration ->
                builder.setAMaxSignalGenerationDelay(Util.fromJavaDuration(duration)));
        Util.doIfNotNull(descriptor.isSignalDelegationSupported(), delegation ->
                builder.setASignalDelegationSupported(Util.toBoolValue(delegation)));
        Util.doIfNotNull(descriptor.isAcknowledgementSupported(), ack ->
                builder.setAAcknowledgementSupported(Util.toBoolValue(ack)));
        Util.doIfNotNull(descriptor.getAcknowledgeTimeout(), duration ->
                builder.setAAcknowledgeTimeout(Util.fromJavaDuration(duration)));

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

    public AlertConditionStateMsg mapAlertConditionState(AlertConditionState state) {
        var builder = AlertConditionStateMsg.newBuilder()
                .setAbstractAlertState(mapAbstractAlertState(state));

        Util.doIfNotNull(state.getActualConditionGenerationDelay(), delay ->
                builder.setAActualConditionGenerationDelay(Util.fromJavaDuration(delay)));
        Util.doIfNotNull(state.getActualPriority(), priority ->
                builder.setAActualPriority(Util.mapToProtoEnum(priority, AlertConditionPriorityMsg.class)));
        Util.doIfNotNull(state.getRank(), rank ->
                builder.setARank(Util.toInt32(rank)));
        Util.doIfNotNull(state.isPresence(), presence ->
                builder.setAPresence(Util.toBoolValue(presence)));
        Util.doIfNotNull(state.getDeterminationTime(), timestamp ->
                builder.setADeterminationTime(Util.toUInt64(timestampAdapter.marshal(timestamp))));

        return builder.build();
    }

    public AlertSignalStateMsg mapAlertSignalState(AlertSignalState state) {
        var builder = AlertSignalStateMsg.newBuilder()
                .setAbstractAlertState(mapAbstractAlertState(state));

        Util.doIfNotNull(state.getActualSignalGenerationDelay(), duration ->
                builder.setAActualSignalGenerationDelay(Util.fromJavaDuration(duration)));
        Util.doIfNotNull(state.getPresence(), presence ->
                builder.setAPresence(Util.mapToProtoEnum(presence, AlertSignalPresenceMsg.class)));
        Util.doIfNotNull(state.getLocation(), location ->
                builder.setALocation(Util.mapToProtoEnum(location, AlertSignalPrimaryLocationMsg.class)));
        Util.doIfNotNull(state.getSlot(), slot ->
                builder.setASlot(Util.toUInt32(slot)));

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
