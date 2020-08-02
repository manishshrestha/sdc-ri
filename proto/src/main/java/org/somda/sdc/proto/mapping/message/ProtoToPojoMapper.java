package org.somda.sdc.proto.mapping.message;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.message.AbstractReport;
import org.somda.sdc.biceps.model.message.InvocationError;
import org.somda.sdc.biceps.model.message.InvocationInfo;
import org.somda.sdc.biceps.model.message.InvocationState;
import org.somda.sdc.biceps.model.message.OperationInvokedReport;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.proto.mapping.Util;
import org.somda.sdc.proto.mapping.participant.PojoToProtoBaseMapper;
import org.somda.sdc.proto.mapping.participant.ProtoToPojoBaseMapper;
import org.somda.sdc.proto.model.biceps.AbstractReportMsg;
import org.somda.sdc.proto.model.biceps.InvocationInfoMsg;
import org.somda.sdc.proto.model.biceps.MdibVersionGroupMsg;
import org.somda.sdc.proto.model.biceps.OperationInvokedReportMsg;

import java.util.stream.Collectors;

public class ProtoToPojoMapper {
    private static final Logger LOG = LogManager.getLogger(ProtoToPojoMapper.class);
    private final Logger instanceLogger;
    private final ProtoToPojoBaseMapper baseMapper;

    @Inject
    ProtoToPojoMapper(@Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier,
                      ProtoToPojoBaseMapper baseMapper) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.baseMapper = baseMapper;
    }

    public OperationInvokedReport map(OperationInvokedReportMsg protoMsg) {
        var pojo = new OperationInvokedReport();
        pojo.setReportPart(protoMsg.getReportPartList().stream().map(reportPartMsg -> {
            var reportPart = new OperationInvokedReport.ReportPart();
            reportPart.setInvocationSource(baseMapper.map(reportPartMsg.getInvocationSource().getInstanceIdentifier()));
            reportPart.setOperationHandleRef(reportPartMsg.getAOperationHandleRef());
            reportPart.setOperationTarget(Util.optionalStr(reportPartMsg, "AOperationTarget"));
            // todo map abstract report part
            // reportPartMsg.getAbstractReportPart().
            reportPart.setInvocationInfo(map(reportPartMsg.getInvocationInfo()));
            return reportPart;
        }).collect(Collectors.toList()));
        map(pojo, protoMsg.getAbstractReport());
        return pojo;
    }

    private InvocationInfo map(InvocationInfoMsg protoMsg) {
        var pojo = new InvocationInfo();
        pojo.setInvocationError(Util.mapToPojoEnum(protoMsg, "InvocationError", InvocationError.class));
        pojo.setInvocationState(Util.mapToPojoEnum(protoMsg, "InvocationState", InvocationState.class));
        pojo.setInvocationErrorMessage(baseMapper.mapLocalizedTexts(protoMsg.getInvocationErrorMessageList()));
        pojo.setTransactionId(protoMsg.getTransactionId());
        return pojo;
    }

    private void map(AbstractReport pojo, AbstractReportMsg protoMsg) {
        var mdibVersion = protoMsg.getAMdibVersionGroup();
        pojo.setInstanceId(Util.optionalBigIntOfLong(mdibVersion, "AInstanceId"));
        pojo.setInstanceId(Util.optionalBigIntOfLong(mdibVersion, "AMdibVersion"));
        pojo.setSequenceId(mdibVersion.getASequenceId());
    }
}
