package org.somda.sdc.proto.mapping.message;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.message.AbstractReport;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.proto.mapping.Util;
import org.somda.sdc.proto.mapping.participant.PojoToProtoBaseMapper;
import org.somda.sdc.proto.model.biceps.AbstractReportMsg;
import org.somda.sdc.proto.model.biceps.MdibVersionGroupMsg;

public class ProtoToPojoMapper {
    private static final Logger LOG = LogManager.getLogger(ProtoToPojoMapper.class);
    private final Logger instanceLogger;
    private final PojoToProtoBaseMapper baseMapper;

    @Inject
    ProtoToPojoMapper(@Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier,
                      PojoToProtoBaseMapper baseMapper) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.baseMapper = baseMapper;
    }
    
    private void mapAbstractReport(AbstractReport pojo, AbstractReportMsg protoMsg) {
        var mdibVersion = protoMsg.getAMdibVersionGroup();
        pojo.setInstanceId(Util.optionalBigIntOfLong(mdibVersion, "AInstanceId"));
        pojo.setInstanceId(Util.optionalBigIntOfLong(mdibVersion, "AMdibVersion"));
        pojo.setSequenceId(mdibVersion.getASequenceId());
    }
}
