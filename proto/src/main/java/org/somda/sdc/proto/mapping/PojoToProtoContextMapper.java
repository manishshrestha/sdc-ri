package org.somda.sdc.proto.mapping;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.participant.AbstractContextDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractContextState;
import org.somda.sdc.biceps.model.participant.EnsembleContextDescriptor;
import org.somda.sdc.biceps.model.participant.EnsembleContextState;
import org.somda.sdc.biceps.model.participant.LocationContextDescriptor;
import org.somda.sdc.biceps.model.participant.LocationContextState;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.common.util.TimestampAdapter;
import org.somda.sdc.proto.model.biceps.AbstractContextDescriptorMsg;
import org.somda.sdc.proto.model.biceps.AbstractContextStateMsg;
import org.somda.sdc.proto.model.biceps.ContextAssociationMsg;
import org.somda.sdc.proto.model.biceps.EnsembleContextDescriptorMsg;
import org.somda.sdc.proto.model.biceps.EnsembleContextStateMsg;
import org.somda.sdc.proto.model.biceps.LocationContextDescriptorMsg;
import org.somda.sdc.proto.model.biceps.LocationContextStateMsg;

import java.math.BigInteger;

public class PojoToProtoContextMapper {
    private static final Logger LOG = LogManager.getLogger(PojoToProtoContextMapper.class);
    private final Logger instanceLogger;
    private final TimestampAdapter timestampAdapter;
    private final PojoToProtoBaseMapper baseMapper;

    @Inject
    PojoToProtoContextMapper(@Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier,
                             TimestampAdapter timestampAdapter,
                             PojoToProtoBaseMapper baseMapper) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.timestampAdapter = timestampAdapter;
        this.baseMapper = baseMapper;
    }


    public EnsembleContextDescriptorMsg.Builder mapEnsembleContextDescriptor(
            EnsembleContextDescriptor ensembleContextDescriptor) {
        return EnsembleContextDescriptorMsg.newBuilder()
                .setAbstractContextDescriptor(mapAbstractContextDescriptor(ensembleContextDescriptor));
    }

    public EnsembleContextStateMsg.Builder mapEnsembleContextState(
            EnsembleContextState ensembleContextState) {
        return EnsembleContextStateMsg.newBuilder()
                .setAbstractContextState(mapAbstractContextState(ensembleContextState));
    }

    public LocationContextDescriptorMsg.Builder mapLocationContextDescriptor(
            LocationContextDescriptor locationContextDescriptor) {
        return LocationContextDescriptorMsg.newBuilder()
                .setAbstractContextDescriptor(mapAbstractContextDescriptor(locationContextDescriptor));
    }

    public LocationContextStateMsg.Builder mapLocationContextState(
            LocationContextState locationContextState) {
        return LocationContextStateMsg.newBuilder()
                .setAbstractContextState(mapAbstractContextState(locationContextState));
    }

    public AbstractContextDescriptorMsg mapAbstractContextDescriptor(
            AbstractContextDescriptor contextDescriptor) {
        return AbstractContextDescriptorMsg.newBuilder()
                .setAbstractDescriptor(baseMapper.mapAbstractDescriptor(contextDescriptor)).build();
    }

    public AbstractContextStateMsg mapAbstractContextState(AbstractContextState contextState) {
        var builder = AbstractContextStateMsg.newBuilder();
        Util.doIfNotNull(contextState.getBindingStartTime(), it ->
                builder.setABindingStartTime(Util.toUInt64(timestampAdapter.marshal(it))));
        Util.doIfNotNull(contextState.getBindingEndTime(), it ->
                builder.setABindingEndTime(Util.toUInt64(timestampAdapter.marshal(it))));
        Util.doIfNotNull(contextState.getBindingMdibVersion(), it ->
                builder.setABindingMdibVersion(Util.toUInt64(it)));
        Util.doIfNotNull(contextState.getUnbindingMdibVersion(), it ->
                builder.setAUnbindingMdibVersion(Util.toUInt64(it)));
        builder.setAContextAssociation(
                Util.mapToProtoEnum(contextState.getContextAssociation(), ContextAssociationMsg.class));
        Util.doIfNotNull(contextState.getIdentification(), it ->
                builder.addAllIdentification(baseMapper.mapInstanceIdentifiers(it)));
        Util.doIfNotNull(contextState.getValidator(), it ->
                builder.addAllValidator(baseMapper.mapInstanceIdentifiers(it)));
        builder.setAbstractMultiState(baseMapper.mapAbstractMultiState(contextState));
        return builder.build();
    }
}
