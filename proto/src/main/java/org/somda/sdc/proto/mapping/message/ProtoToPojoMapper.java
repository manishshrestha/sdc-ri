package org.somda.sdc.proto.mapping.message;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.message.*;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.proto.mapping.Util;
import org.somda.sdc.proto.mapping.participant.PojoToProtoBaseMapper;
import org.somda.sdc.proto.mapping.participant.ProtoToPojoBaseMapper;
import org.somda.sdc.proto.mapping.participant.ProtoToPojoMetricMapper;
import org.somda.sdc.proto.mapping.participant.ProtoToPojoOneOfMapper;
import org.somda.sdc.proto.model.biceps.*;

import java.util.stream.Collectors;

public class ProtoToPojoMapper {
    private static final Logger LOG = LogManager.getLogger(ProtoToPojoMapper.class);
    private final Logger instanceLogger;
    private final ProtoToPojoBaseMapper baseMapper;
    private final ProtoToPojoOneOfMapper oneOfMapper;
    private final ProtoToPojoMetricMapper metricMapper;

    @Inject
    ProtoToPojoMapper(@Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier,
                      ProtoToPojoBaseMapper baseMapper,
                      ProtoToPojoOneOfMapper oneOfMapper,
                      ProtoToPojoMetricMapper metricMapper) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.baseMapper = baseMapper;
        this.oneOfMapper = oneOfMapper;
        this.metricMapper = metricMapper;
    }

    public EpisodicMetricReport map(EpisodicMetricReportMsg protoMsg) {
        var pojo = new EpisodicMetricReport();
        map(pojo, protoMsg.getAbstractMetricReport());
        return pojo;
    }

    public EpisodicOperationalStateReport map(EpisodicOperationalStateReportMsg protoMsg) {
        var pojo = new EpisodicOperationalStateReport();
        map(pojo, protoMsg.getAbstractOperationalStateReport());
        return pojo;
    }

    public EpisodicContextReport map(EpisodicContextReportMsg protoMsg) {
        var pojo = new EpisodicContextReport();
        map(pojo, protoMsg.getAbstractContextReport());
        return pojo;
    }

    public EpisodicAlertReport map(EpisodicAlertReportMsg protoMsg) {
        var pojo = new EpisodicAlertReport();
        map(pojo, protoMsg.getAbstractAlertReport());
        return pojo;
    }

    public EpisodicComponentReport map(EpisodicComponentReportMsg protoMsg) {
        var pojo = new EpisodicComponentReport();
        map(pojo, protoMsg.getAbstractComponentReport());
        return pojo;
    }

    public WaveformStream map(WaveformStreamMsg protoMsg) {
        var pojo = new WaveformStream();
        map(pojo, protoMsg.getAbstractReport());
        protoMsg.getStateList().forEach(state -> pojo.getState().add(metricMapper.map(state)));
        return pojo;
    }

//    public DescriptionModificationReport map(DescriptionModificationReportMsg protoMsg) {
//        var pojo = new DescriptionModificationReport();
//        map(pojo, protoMsg.getAbstractReport());
//        protoMsg.getReportPartList().forEach();
//        return pojo;
//    }

    public Activate map(ActivateMsg protoMsg) {
        var pojo = new Activate();
        // todo map params - it's an any; should be a one of simple types
        return pojo;
    }

    public ActivateResponse map(ActivateResponseMsg protoMsg) {
        var pojo = new ActivateResponse();
        map(pojo, protoMsg.getAbstractSetResponse());
        return pojo;
    }

    public SetString map(SetStringMsg protoMsg) {
        var pojo = new SetString();
        pojo.setRequestedStringValue(protoMsg.getRequestedStringValue());
        return pojo;
    }

    public SetStringResponse map(SetStringResponseMsg protoMsg) {
        var pojo = new SetStringResponse();
        map(pojo, protoMsg.getAbstractSetResponse());
        return pojo;
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

    public AbstractMetricReport.ReportPart map(AbstractMetricReportMsg.ReportPartMsg protoMsg) {
        var pojo = new AbstractMetricReport.ReportPart();
        protoMsg.getMetricStateList().forEach(state -> pojo.getMetricState().add(oneOfMapper.map(state)));
        return pojo;
    }

    public AbstractOperationalStateReport.ReportPart map(AbstractOperationalStateReportMsg.ReportPartMsg protoMsg) {
        var pojo = new AbstractOperationalStateReport.ReportPart();
        protoMsg.getOperationStateList().forEach(state -> pojo.getOperationState().add(oneOfMapper.map(state)));
        return pojo;
    }

    public AbstractContextReport.ReportPart map(AbstractContextReportMsg.ReportPartMsg protoMsg) {
        var pojo = new AbstractContextReport.ReportPart();
        protoMsg.getContextStateList().forEach(state -> pojo.getContextState().add(oneOfMapper.map(state)));
        return pojo;
    }

    public AbstractAlertReport.ReportPart map(AbstractAlertReportMsg.ReportPartMsg protoMsg) {
        var pojo = new AbstractAlertReport.ReportPart();
        protoMsg.getAlertStateList().forEach(state -> pojo.getAlertState().add(oneOfMapper.map(state)));
        return pojo;
    }

    public AbstractComponentReport.ReportPart map(AbstractComponentReportMsg.ReportPartMsg protoMsg) {
        var pojo = new AbstractComponentReport.ReportPart();
        protoMsg.getComponentStateList().forEach(state -> pojo.getComponentState().add(oneOfMapper.map(state)));
        return pojo;
    }

    public DescriptionModificationReport.ReportPart map(DescriptionModificationReportMsg.ReportPartMsg protoMsg) {
        var pojo = new DescriptionModificationReport.ReportPart();
        Util.doIfNotNull(Util.optional(protoMsg, "AModificationType", DescriptionModificationType.class),
                pojo::setModificationType
        );
        Util.doIfNotNull(Util.optionalStr(protoMsg, "AParentDescriptor"), pojo::setParentDescriptor);
        map(pojo, protoMsg.getAbstractReportPart());
        return pojo;
    }

    private void map(AbstractMetricReport pojo, AbstractMetricReportMsg protoMsg) {
        map(pojo, protoMsg.getAbstractReport());
        protoMsg.getReportPartList().forEach(part -> pojo.getReportPart().add(map(part)));
    }

    private void map(AbstractOperationalStateReport pojo, AbstractOperationalStateReportMsg protoMsg) {
        map(pojo, protoMsg.getAbstractReport());
        protoMsg.getReportPartList().forEach(part -> pojo.getReportPart().add(map(part)));
    }

    private void map(AbstractContextReport pojo, AbstractContextReportMsg protoMsg) {
        map(pojo, protoMsg.getAbstractReport());
        protoMsg.getReportPartList().forEach(part -> pojo.getReportPart().add(map(part)));
    }

    private void map(AbstractAlertReport pojo, AbstractAlertReportMsg protoMsg) {
        map(pojo, protoMsg.getAbstractReport());
        protoMsg.getReportPartList().forEach(part -> pojo.getReportPart().add(map(part)));
    }

    private void map(AbstractComponentReport pojo, AbstractComponentReportMsg protoMsg) {
        map(pojo, protoMsg.getAbstractReport());
        protoMsg.getReportPartList().forEach(part -> pojo.getReportPart().add(map(part)));
    }

    private void map(AbstractReport pojo, AbstractReportMsg protoMsg) {
        var mdibVersion = protoMsg.getAMdibVersionGroup();
        pojo.setInstanceId(Util.optionalBigIntOfLong(mdibVersion, "AInstanceId"));
        pojo.setMdibVersion(Util.optionalBigIntOfLong(mdibVersion, "AMdibVersion"));
        pojo.setSequenceId(mdibVersion.getASequenceId());
    }

    private void map(AbstractReportPart pojo, AbstractReportPartMsg protoMsg) {
        Util.doIfNotNull(Util.optionalStr(protoMsg, "SourceMds"), pojo::setSourceMds);
    }

    private void map(AbstractSetResponse pojo, AbstractSetResponseMsg protoMsg) {
        var mdibVersion = protoMsg.getAMdibVersionGroup();
        pojo.setInstanceId(Util.optionalBigIntOfLong(mdibVersion, "AInstanceId"));
        pojo.setMdibVersion(Util.optionalBigIntOfLong(mdibVersion, "AMdibVersion"));
        pojo.setSequenceId(mdibVersion.getASequenceId());
        pojo.setInvocationInfo(map(protoMsg.getInvocationInfo()));
    }
}
