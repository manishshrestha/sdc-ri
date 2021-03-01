package org.somda.sdc.proto.mapping.message;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.StringValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.message.AbstractAlertReport;
import org.somda.sdc.biceps.model.message.AbstractComponentReport;
import org.somda.sdc.biceps.model.message.AbstractContextReport;
import org.somda.sdc.biceps.model.message.AbstractMetricReport;
import org.somda.sdc.biceps.model.message.AbstractOperationalStateReport;
import org.somda.sdc.biceps.model.message.AbstractReport;
import org.somda.sdc.biceps.model.message.AbstractReportPart;
import org.somda.sdc.biceps.model.message.AbstractSetResponse;
import org.somda.sdc.biceps.model.message.Activate;
import org.somda.sdc.biceps.model.message.ActivateResponse;
import org.somda.sdc.biceps.model.message.DescriptionModificationReport;
import org.somda.sdc.biceps.model.message.DescriptionModificationType;
import org.somda.sdc.biceps.model.message.EpisodicAlertReport;
import org.somda.sdc.biceps.model.message.EpisodicComponentReport;
import org.somda.sdc.biceps.model.message.EpisodicContextReport;
import org.somda.sdc.biceps.model.message.EpisodicMetricReport;
import org.somda.sdc.biceps.model.message.EpisodicOperationalStateReport;
import org.somda.sdc.biceps.model.message.InvocationError;
import org.somda.sdc.biceps.model.message.InvocationInfo;
import org.somda.sdc.biceps.model.message.InvocationState;
import org.somda.sdc.biceps.model.message.OperationInvokedReport;
import org.somda.sdc.biceps.model.message.SetString;
import org.somda.sdc.biceps.model.message.SetStringResponse;
import org.somda.sdc.biceps.model.message.SetValue;
import org.somda.sdc.biceps.model.message.SetValueResponse;
import org.somda.sdc.biceps.model.message.WaveformStream;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.proto.mapping.Util;
import org.somda.sdc.proto.mapping.participant.ProtoToPojoBaseMapper;
import org.somda.sdc.proto.mapping.participant.ProtoToPojoMetricMapper;
import org.somda.sdc.proto.mapping.participant.ProtoToPojoOneOfMapper;
import org.somda.sdc.proto.model.biceps.*;

import java.math.BigDecimal;
import java.math.BigInteger;
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

    public DescriptionModificationReport map(DescriptionModificationReportMsg protoMsg) {
        var pojo = new DescriptionModificationReport();
        map(pojo, protoMsg.getAbstractReport());
        protoMsg.getReportPartList().forEach(part -> pojo.getReportPart().add(map(part)));
        return pojo;
    }

    public Activate map(ActivateMsg protoMsg) {
        var pojo = new Activate();
        pojo.setArgument(protoMsg.getArgumentList().stream()
                .map(argumentMsg -> {
                    var arg = new Activate.Argument();
                    arg.setArgValue(null);
                    if (argumentMsg.getArgValue().getTypeUrl().endsWith(StringValue.getDescriptor().getFullName())) {
                        try {
                            var strVal = argumentMsg.getArgValue().unpack(StringValue.class);
                            arg.setArgValue(strVal.getValue());
                        } catch (InvalidProtocolBufferException e) {
                            // ignore
                        }
                    }
                    return arg;
                })
                .filter(arg -> arg.getArgValue() != null)
                .collect(Collectors.toList()));
        return pojo;
    }

    public ActivateResponse map(ActivateResponseMsg protoMsg) {
        var pojo = new ActivateResponse();
        map(pojo, protoMsg.getAbstractSetResponse());
        return pojo;
    }

    public SetValue map(SetValueMsg protoMsg) {
        var pojo = new SetValue();
        pojo.setRequestedNumericValue(new BigDecimal(protoMsg.getRequestedNumericValue()));
        return pojo;
    }

    public SetValueResponse map(SetValueResponseMsg protoMsg) {
        var pojo = new SetValueResponse();
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
            reportPart.setOperationHandleRef(baseMapper.map(reportPartMsg.getAOperationHandleRef()));
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
        pojo.setTransactionId(baseMapper.map(protoMsg.getTransactionId()));
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
        Util.doIfNotNull(
                Util.optional(mdibVersion, "AMdibVersion", VersionCounterMsg.class),
                versionCounterMsg -> pojo.setMdibVersion(BigInteger.valueOf(versionCounterMsg.getUnsignedLong()))
        );
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
