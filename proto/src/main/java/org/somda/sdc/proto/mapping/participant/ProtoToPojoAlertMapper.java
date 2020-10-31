package org.somda.sdc.proto.mapping.participant;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.BoolValue;
import com.google.protobuf.Duration;
import com.google.protobuf.Int64Value;
import com.google.protobuf.StringValue;
import com.google.protobuf.UInt32Value;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.participant.AbstractAlertDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractAlertState;
import org.somda.sdc.biceps.model.participant.AlertActivation;
import org.somda.sdc.biceps.model.participant.AlertConditionDescriptor;
import org.somda.sdc.biceps.model.participant.AlertConditionKind;
import org.somda.sdc.biceps.model.participant.AlertConditionMonitoredLimits;
import org.somda.sdc.biceps.model.participant.AlertConditionPriority;
import org.somda.sdc.biceps.model.participant.AlertConditionState;
import org.somda.sdc.biceps.model.participant.AlertSignalDescriptor;
import org.somda.sdc.biceps.model.participant.AlertSignalManifestation;
import org.somda.sdc.biceps.model.participant.AlertSignalPresence;
import org.somda.sdc.biceps.model.participant.AlertSignalPrimaryLocation;
import org.somda.sdc.biceps.model.participant.AlertSignalState;
import org.somda.sdc.biceps.model.participant.AlertSystemDescriptor;
import org.somda.sdc.biceps.model.participant.AlertSystemState;
import org.somda.sdc.biceps.model.participant.CauseInfo;
import org.somda.sdc.biceps.model.participant.LimitAlertConditionDescriptor;
import org.somda.sdc.biceps.model.participant.LimitAlertConditionState;
import org.somda.sdc.biceps.model.participant.RemedyInfo;
import org.somda.sdc.biceps.model.participant.SystemSignalActivation;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.common.util.TimestampAdapter;
import org.somda.sdc.proto.mapping.Util;
import org.somda.sdc.proto.model.biceps.AbstractAlertDescriptorMsg;
import org.somda.sdc.proto.model.biceps.AbstractAlertStateMsg;
import org.somda.sdc.proto.model.biceps.AlertConditionDescriptorMsg;
import org.somda.sdc.proto.model.biceps.AlertConditionStateMsg;
import org.somda.sdc.proto.model.biceps.AlertSignalDescriptorMsg;
import org.somda.sdc.proto.model.biceps.AlertSignalStateMsg;
import org.somda.sdc.proto.model.biceps.AlertSystemDescriptorMsg;
import org.somda.sdc.proto.model.biceps.AlertSystemStateMsg;
import org.somda.sdc.proto.model.biceps.CauseInfoMsg;
import org.somda.sdc.proto.model.biceps.LimitAlertConditionDescriptorMsg;
import org.somda.sdc.proto.model.biceps.LimitAlertConditionStateMsg;
import org.somda.sdc.proto.model.biceps.RemedyInfoMsg;
import org.somda.sdc.proto.model.biceps.SystemSignalActivationMsg;

public class ProtoToPojoAlertMapper {
    private static final Logger LOG = LogManager.getLogger(ProtoToPojoAlertMapper.class);
    private final Logger instanceLogger;
    private final ProtoToPojoBaseMapper baseMapper;
    private final TimestampAdapter timestampAdapter;

    @Inject
    ProtoToPojoAlertMapper(@Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier,
                           ProtoToPojoBaseMapper baseMapper,
                           TimestampAdapter timestampAdapter) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.baseMapper = baseMapper;
        this.timestampAdapter = timestampAdapter;
    }

    public AlertSystemDescriptor map(AlertSystemDescriptorMsg protoMsg) {
        var pojo = new AlertSystemDescriptor();
        map(pojo, protoMsg.getAbstractAlertDescriptor());
        Util.doIfNotNull(Util.optional(protoMsg, "AMaxPhysiologicalParallelAlarms", UInt32Value.class), value ->
                pojo.setMaxPhysiologicalParallelAlarms((long) value.getValue())
        );
        Util.doIfNotNull(Util.optional(protoMsg, "AMaxTechnicalParallelAlarms", UInt32Value.class), value ->
                pojo.setMaxTechnicalParallelAlarms((long) value.getValue())
        );
        Util.doIfNotNull(Util.optional(protoMsg, "ASelfCheckPeriod", Duration.class), duration ->
                pojo.setSelfCheckPeriod(Util.fromProtoDuration(duration)));
//        pojo.setAlertCondition();
//        pojo.setAlertSignal();
        return pojo;
    }

    public LimitAlertConditionDescriptor map(LimitAlertConditionDescriptorMsg protoMsg) {
        var pojo = new LimitAlertConditionDescriptor();
        map(pojo, protoMsg.getAlertConditionDescriptor());
        Util.doIfNotNull(Util.optional(protoMsg, "AAutoLimitSupported", BoolValue.class), supported ->
                pojo.setAutoLimitSupported(supported.getValue()));
        pojo.setMaxLimits(baseMapper.map(protoMsg.getMaxLimits()));
        return pojo;
    }

    public AlertConditionDescriptor map(AlertConditionDescriptorMsg protoMsg) {
        var pojo = new AlertConditionDescriptor();
        map(pojo, protoMsg);
        return pojo;
    }

    private void map(AlertConditionDescriptor pojo, AlertConditionDescriptorMsg protoMsg) {
        map(pojo, protoMsg.getAbstractAlertDescriptor());

        pojo.setKind(Util.mapToPojoEnum(protoMsg, "AKind", AlertConditionKind.class));
        pojo.setPriority(Util.mapToPojoEnum(protoMsg, "APriority", AlertConditionPriority.class));

        Util.doIfNotNull(Util.optional(protoMsg, "ADefaultConditionGenerationDelay", Duration.class), duration ->
                pojo.setDefaultConditionGenerationDelay(Util.fromProtoDuration(duration)));
        pojo.setCanEscalate(Util.mapToPojoEnum(protoMsg, "CanEscalate", AlertConditionPriority.class));
        pojo.setCanDeescalate(Util.mapToPojoEnum(protoMsg, "CanDeescalate", AlertConditionPriority.class));

        protoMsg.getSourceList().forEach(src -> pojo.getSource().add(src));
        protoMsg.getCauseInfoList().forEach(info -> pojo.getCauseInfo().add(map(info)));

        // TODO: Extension
//        pojo.setExtension();
    }

    public CauseInfo map(CauseInfoMsg protoMsg) {
        var pojo = new CauseInfo();
        Util.doIfNotNull(Util.optional(protoMsg, "RemedyInfo", RemedyInfoMsg.class), remedyInfoMsg -> {
            pojo.setRemedyInfo(map(remedyInfoMsg));
        });
        pojo.setDescription(baseMapper.mapLocalizedTexts(protoMsg.getDescriptionList()));

        return pojo;
    }

    public RemedyInfo map(RemedyInfoMsg protoMsg) {
        var pojo = new RemedyInfo();
        pojo.setDescription(baseMapper.mapLocalizedTexts(protoMsg.getDescriptionList()));
        return pojo;
    }

    public AlertSignalDescriptor map(AlertSignalDescriptorMsg protoMsg) {
        var pojo = new AlertSignalDescriptor();
        map(pojo, protoMsg.getAbstractAlertDescriptor());

        Util.doIfNotNull(Util.optional(protoMsg, "AConditionSignaled", StringValue.class), condition ->
                pojo.setConditionSignaled(condition.getValue()));
        pojo.setManifestation(Util.mapToPojoEnum(protoMsg, "AManifestation", AlertSignalManifestation.class));
        pojo.setLatching(protoMsg.getALatching());
        Util.doIfNotNull(Util.optional(protoMsg, "ADefaultSignalGenerationDelay", Duration.class), duration ->
                pojo.setDefaultSignalGenerationDelay(Util.fromProtoDuration(duration)));
        Util.doIfNotNull(Util.optional(protoMsg, "AMinSignalGenerationDelay", Duration.class), duration ->
                pojo.setMinSignalGenerationDelay(Util.fromProtoDuration(duration)));
        Util.doIfNotNull(Util.optional(protoMsg, "AMaxSignalGenerationDelay", Duration.class), duration ->
                pojo.setMaxSignalGenerationDelay(Util.fromProtoDuration(duration)));
        Util.doIfNotNull(Util.optional(protoMsg, "ASignalDelegationSupported", BoolValue.class), delegation ->
                pojo.setSignalDelegationSupported(delegation.getValue()));
        Util.doIfNotNull(Util.optional(protoMsg, "AAcknowledgementSupported", BoolValue.class), ack ->
                pojo.setAcknowledgementSupported(ack.getValue()));
        Util.doIfNotNull(Util.optional(protoMsg, "AAcknowledgeTimeout", Duration.class), duration ->
                pojo.setAcknowledgeTimeout(Util.fromProtoDuration(duration)));

        return pojo;
    }

    public void map(AbstractAlertDescriptor pojo, AbstractAlertDescriptorMsg protoMsg) {
        baseMapper.map(pojo, protoMsg.getAbstractDescriptor());
    }

    public AlertSystemState map(AlertSystemStateMsg protoMsg) {
        var pojo = new AlertSystemState();
        map(pojo, protoMsg.getAbstractAlertState());

        Util.doIfNotNull(Util.optionalBigIntOfLong(protoMsg, "ALastSelfCheck"), check ->
                pojo.setLastSelfCheck(timestampAdapter.unmarshal(check)));
        Util.doIfNotNull(Util.optional(protoMsg, "ASelfCheckCount", Int64Value.class), value ->
                pojo.setSelfCheckCount(value.getValue()));
        Util.doIfNotNull(protoMsg.getAPresentPhysiologicalAlarmConditions(), conditions ->
                pojo.setPresentPhysiologicalAlarmConditions(conditions.getAlertConditionReferenceList())
        );
        Util.doIfNotNull(protoMsg.getAPresentTechnicalAlarmConditions(), conditions ->
                pojo.setPresentTechnicalAlarmConditions(conditions.getAlertConditionReferenceList())
        );

        protoMsg.getSystemSignalActivationList().forEach(ssa -> pojo.getSystemSignalActivation().add(map(ssa)));

        return pojo;
    }

    public LimitAlertConditionState map(LimitAlertConditionStateMsg protoMsg) {
        var pojo = new LimitAlertConditionState();
        map(pojo, protoMsg.getAlertConditionState());
        pojo.setMonitoredAlertLimits(Util.mapToPojoEnum(protoMsg, "AMonitoredAlertLimits", AlertConditionMonitoredLimits.class));
        pojo.setAutoLimitActivationState(Util.mapToPojoEnum(protoMsg, "AAutoLimitActivationState", AlertActivation.class));
        pojo.setLimits(baseMapper.map(protoMsg.getLimits()));
        return pojo;
    }

    public AlertConditionState map(AlertConditionStateMsg protoMsg) {
        var pojo = new AlertConditionState();
        map(pojo, protoMsg);
        return pojo;
    }

    public AlertSignalState map(AlertSignalStateMsg protoMsg) {
        var pojo = new AlertSignalState();
        map(pojo, protoMsg.getAbstractAlertState());

        Util.doIfNotNull(Util.optional(protoMsg, "AActualSignalGenerationDelay", Duration.class), duration ->
                pojo.setActualSignalGenerationDelay(Util.fromProtoDuration(duration)));
        pojo.setPresence(Util.mapToPojoEnum(protoMsg, "APresence", AlertSignalPresence.class));
        pojo.setLocation(Util.mapToPojoEnum(protoMsg, "ALocation", AlertSignalPrimaryLocation.class));
        pojo.setSlot(Util.optionalLongOfInt(protoMsg, "ASlot"));

        return pojo;
    }

    private void map(AlertConditionState pojo, AlertConditionStateMsg protoMsg) {
        map(pojo, protoMsg.getAbstractAlertState());
        Util.doIfNotNull(Util.optional(protoMsg, "AActualConditionGenerationDelay", Duration.class), duration ->
                pojo.setActualConditionGenerationDelay(Util.fromProtoDuration(duration)));
        pojo.setActualPriority(Util.mapToPojoEnum(protoMsg, "AActualPriority", AlertConditionPriority.class));
        pojo.setRank(Util.optionalIntOfInt(protoMsg, "ARank"));
        Util.doIfNotNull(Util.optional(protoMsg, "APresence", BoolValue.class), presence ->
                pojo.setPresence(presence.getValue()));
        Util.doIfNotNull(Util.optionalBigIntOfLong(protoMsg, "ADeterminationTime"), check ->
                pojo.setDeterminationTime(timestampAdapter.unmarshal(check)));
    }

    private void map(AbstractAlertState pojo, AbstractAlertStateMsg protoMsg) {
        baseMapper.map(pojo, protoMsg.getAbstractState());
        pojo.setActivationState(Util.mapToPojoEnum(protoMsg, "AActivationState", AlertActivation.class));
    }

    private SystemSignalActivation map(SystemSignalActivationMsg protoMsg) {
        var pojo = new SystemSignalActivation();
        pojo.setManifestation(Util.mapToPojoEnum(protoMsg, "AManifestation", AlertSignalManifestation.class));
        pojo.setState(Util.mapToPojoEnum(protoMsg, "AState", AlertActivation.class));
        return pojo;
    }
}
