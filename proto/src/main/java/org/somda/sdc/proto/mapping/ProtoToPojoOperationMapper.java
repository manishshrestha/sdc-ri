package org.somda.sdc.proto.mapping;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.participant.AbstractMetricState;
import org.somda.sdc.biceps.model.participant.ComponentActivation;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.proto.model.biceps.AbstractMetricStateMsg;

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


}
