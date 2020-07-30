package org.somda.sdc.proto.mapping;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.Duration;
import com.google.protobuf.Int64Value;
import com.google.protobuf.UInt32Value;
import com.google.protobuf.UInt64Value;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.participant.AbstractAlertDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractAlertState;
import org.somda.sdc.biceps.model.participant.AlertActivation;
import org.somda.sdc.biceps.model.participant.AlertSystemDescriptor;
import org.somda.sdc.biceps.model.participant.AlertSystemState;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.common.util.TimestampAdapter;
import org.somda.sdc.proto.model.biceps.AbstractAlertDescriptorMsg;
import org.somda.sdc.proto.model.biceps.AbstractAlertStateMsg;
import org.somda.sdc.proto.model.biceps.AlertSystemDescriptorMsg;
import org.somda.sdc.proto.model.biceps.AlertSystemStateMsg;

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

        return pojo;
    }

    private void map(AbstractAlertState pojo, AbstractAlertStateMsg protoMsg) {
        baseMapper.map(pojo, protoMsg.getAbstractState());
        pojo.setActivationState(Util.mapToPojoEnum(protoMsg, "AActivationState", AlertActivation.class));
    }
}
