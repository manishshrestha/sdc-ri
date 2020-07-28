package org.somda.sdc.proto.mapping;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;

public class PojoToProtoContextMapper {
    private static final Logger LOG = LogManager.getLogger(PojoToProtoContextMapper.class);
    private final Logger instanceLogger;
    private final PojoToProtoBaseMapper baseMapper;

    @Inject
    PojoToProtoContextMapper(@Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier,
                           PojoToProtoBaseMapper baseMapper) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.baseMapper = baseMapper;
    }
}
