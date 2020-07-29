package org.somda.sdc.proto.mapping;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.participant.AbstractMetricDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractMetricState;
import org.somda.sdc.biceps.model.participant.ComponentActivation;
import org.somda.sdc.biceps.model.participant.EnumStringMetricDescriptor;
import org.somda.sdc.biceps.model.participant.EnumStringMetricState;
import org.somda.sdc.biceps.model.participant.MetricCategory;
import org.somda.sdc.biceps.model.participant.StringMetricDescriptor;
import org.somda.sdc.biceps.model.participant.StringMetricState;
import org.somda.sdc.biceps.model.participant.StringMetricValue;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.proto.model.biceps.AbstractMetricDescriptorMsg;
import org.somda.sdc.proto.model.biceps.AbstractMetricStateMsg;
import org.somda.sdc.proto.model.biceps.EnumStringMetricDescriptorMsg;
import org.somda.sdc.proto.model.biceps.EnumStringMetricStateMsg;
import org.somda.sdc.proto.model.biceps.StringMetricDescriptorMsg;
import org.somda.sdc.proto.model.biceps.StringMetricStateMsg;
import org.somda.sdc.proto.model.biceps.StringMetricValueMsg;

import java.util.stream.Collectors;

public class ProtoToPojoMetricMapper {
    private static final Logger LOG = LogManager.getLogger(ProtoToPojoMetricMapper.class);
    private final Logger instanceLogger;
    private final ProtoToPojoBaseMapper baseMapper;

    @Inject
    ProtoToPojoMetricMapper(@Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier,
                            ProtoToPojoBaseMapper baseMapper) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.baseMapper = baseMapper;
    }

    public EnumStringMetricDescriptor map(EnumStringMetricDescriptorMsg protoMsg) {
        var pojo = new EnumStringMetricDescriptor();
        map(pojo, protoMsg.getStringMetricDescriptor());
        pojo.setAllowedValue(protoMsg.getAllowedValueList().stream().map(this::map).collect(Collectors.toList()));
        return pojo;
    }

    public EnumStringMetricState map(EnumStringMetricStateMsg protoMsg) {
        var pojo = new EnumStringMetricState();
        map(pojo, protoMsg.getStringMetricState());
        return pojo;
    }

    public EnumStringMetricDescriptor.AllowedValue map(EnumStringMetricDescriptorMsg.AllowedValueMsg protoMsg) {
        var pojo = new EnumStringMetricDescriptor.AllowedValue();
        pojo.setValue(protoMsg.getValue());
//        pojo.setCharacteristic();
//        pojo.setIdentification();
//        pojo.setType();
        return pojo;
    }

    public StringMetricDescriptor map(StringMetricDescriptorMsg protoMsg) {
        var pojo = new StringMetricDescriptor();
        map(pojo, protoMsg);
        return pojo;
    }

    public StringMetricState map(StringMetricStateMsg protoMsg) {
        var pojoState = new StringMetricState();
        map(pojoState, protoMsg);
        return pojoState;
    }

    public StringMetricValue map(StringMetricValueMsg protoMsg) {
        var pojoValue = new StringMetricValue();
        pojoValue.setValue(Util.optionalStr(protoMsg, "AValue"));
        return pojoValue;
    }

    private void map(StringMetricDescriptor pojo, StringMetricDescriptorMsg protoMsg) {
        map(pojo, protoMsg.getAbstractMetricDescriptor());
    }

    private void map(StringMetricState state, StringMetricStateMsg protoMsg) {
        state.setMetricValue(map(protoMsg.getMetricValue()));
        map(state, protoMsg.getAbstractMetricState());
    }

    private void map(AbstractMetricDescriptor pojo, AbstractMetricDescriptorMsg protoMsg) {
        baseMapper.map(pojo, protoMsg.getAbstractDescriptor());
        Util.doIfNotNull(
                protoMsg.getAMetricCategory(),
                category -> pojo.setMetricCategory(Util.mapToPojoEnum(protoMsg, "AMetricCategory", MetricCategory.class))
        );
    }

    private void map(AbstractMetricState state, AbstractMetricStateMsg protoMsg) {
        Util.doIfNotNull(
                protoMsg.getAActivationState(), aState ->
                        state.setActivationState(Util.mapToPojoEnum(protoMsg, "AActivationState", ComponentActivation.class))
        );
        Util.doIfNotNull(
                protoMsg.getAActiveDeterminationPeriod(),
                period -> state.setActiveDeterminationPeriod(Util.fromProtoDuration(period))
        );
        Util.doIfNotNull(
                protoMsg.getALifeTimePeriod(),
                period -> state.setLifeTimePeriod(Util.fromProtoDuration(period))
        );
        baseMapper.map(state, protoMsg.getAbstractState());
    }
}
