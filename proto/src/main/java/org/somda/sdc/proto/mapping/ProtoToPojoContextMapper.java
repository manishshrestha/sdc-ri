package org.somda.sdc.proto.mapping;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.participant.AbstractContextState;
import org.somda.sdc.biceps.model.participant.ContextAssociation;
import org.somda.sdc.biceps.model.participant.EnsembleContextState;
import org.somda.sdc.biceps.model.participant.*;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.common.util.TimestampAdapter;
import org.somda.sdc.proto.model.biceps.AbstractContextDescriptorMsg;
import org.somda.sdc.proto.model.biceps.AbstractContextStateMsg;
import org.somda.sdc.proto.model.biceps.EnsembleContextDescriptorMsg;
import org.somda.sdc.proto.model.biceps.EnsembleContextStateMsg;
import org.somda.sdc.proto.model.biceps.LocationContextDescriptorMsg;
import org.somda.sdc.proto.model.biceps.LocationContextStateMsg;

public class ProtoToPojoContextMapper {
    private static final Logger LOG = LogManager.getLogger(ProtoToPojoContextMapper.class);
    private final Logger instanceLogger;
    private final ProtoToPojoBaseMapper baseMapper;
    private final TimestampAdapter timestampAdapter;

    @Inject
    ProtoToPojoContextMapper(@Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier,
                             ProtoToPojoBaseMapper baseMapper,
                             TimestampAdapter timestampAdapter) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.baseMapper = baseMapper;
        this.timestampAdapter = timestampAdapter;
    }

    public EnsembleContextDescriptor map(EnsembleContextDescriptorMsg protoMsg) {
        var pojoDescriptor = new EnsembleContextDescriptor();
        map(pojoDescriptor, protoMsg.getAbstractContextDescriptor());
        return pojoDescriptor;
    }

    public EnsembleContextState map(EnsembleContextStateMsg protoMsg) {
        var pojoState = new EnsembleContextState();
        map(pojoState, protoMsg.getAbstractContextState());
        return pojoState;
    }

    public LocationContextDescriptor map(LocationContextDescriptorMsg protoMsg) {
        var pojoDescriptor = new LocationContextDescriptor();
        map(pojoDescriptor, protoMsg.getAbstractContextDescriptor());
        return pojoDescriptor;
    }

    public LocationContextState map(LocationContextStateMsg protoMsg) {
        var pojoState = new LocationContextState();
        if (protoMsg.hasLocationDetail()) {
            pojoState.setLocationDetail(baseMapper.map(protoMsg.getLocationDetail()));
        }
        map(pojoState, protoMsg.getAbstractContextState());
        return pojoState;
    }

    private void map(AbstractContextDescriptor pojo, AbstractContextDescriptorMsg protoMsg) {

        baseMapper.map(pojo, protoMsg.getAbstractDescriptor());
    }

    private void map(AbstractContextState pojo, AbstractContextStateMsg protoMsg) {
        pojo.setBindingEndTime(timestampAdapter.unmarshal(Util.optionalBigIntOfLong(protoMsg, "ABindingEndTime")));
        pojo.setBindingStartTime(timestampAdapter.unmarshal(Util.optionalBigIntOfLong(protoMsg, "ABindingStartTime")));
        pojo.setBindingMdibVersion(Util.optionalBigIntOfLong(protoMsg, "ABindingMdibVersion"));
        pojo.setUnbindingMdibVersion(Util.optionalBigIntOfLong(protoMsg, "AUnbindingMdibVersion"));
        pojo.setContextAssociation(Util.mapToPojoEnum(protoMsg, "AContextAssociation", ContextAssociation.class));
        pojo.setIdentification(baseMapper.mapInstanceIdentifiers(protoMsg.getIdentificationList()));
        pojo.setValidator(baseMapper.mapInstanceIdentifiers(protoMsg.getValidatorList()));
        baseMapper.map(pojo, protoMsg.getAbstractMultiState());
    }
}
