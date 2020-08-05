package org.somda.sdc.proto.mapping.message;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.message.AbstractAlertReport;
import org.somda.sdc.biceps.model.message.AbstractComponentReport;
import org.somda.sdc.biceps.model.message.AbstractContextReport;
import org.somda.sdc.biceps.model.message.AbstractMetricReport;
import org.somda.sdc.biceps.model.message.AbstractOperationalStateReport;
import org.somda.sdc.biceps.model.message.AbstractReport;
import org.somda.sdc.biceps.model.message.AbstractSet;
import org.somda.sdc.biceps.model.message.AbstractSetResponse;
import org.somda.sdc.biceps.model.message.Activate;
import org.somda.sdc.biceps.model.message.ActivateResponse;
import org.somda.sdc.biceps.model.message.EpisodicAlertReport;
import org.somda.sdc.biceps.model.message.EpisodicComponentReport;
import org.somda.sdc.biceps.model.message.EpisodicContextReport;
import org.somda.sdc.biceps.model.message.EpisodicMetricReport;
import org.somda.sdc.biceps.model.message.EpisodicOperationalStateReport;
import org.somda.sdc.biceps.model.message.InvocationInfo;
import org.somda.sdc.biceps.model.message.OperationInvokedReport;
import org.somda.sdc.biceps.model.message.SetString;
import org.somda.sdc.biceps.model.message.SetStringResponse;
import org.somda.sdc.biceps.model.message.WaveformStream;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.proto.mapping.Util;
import org.somda.sdc.proto.mapping.participant.PojoToProtoAlertMapper;
import org.somda.sdc.proto.mapping.participant.PojoToProtoBaseMapper;
import org.somda.sdc.proto.mapping.participant.PojoToProtoMetricMapper;
import org.somda.sdc.proto.mapping.participant.PojoToProtoOneOfMapper;
import org.somda.sdc.proto.model.biceps.AbstractAlertReportMsg;
import org.somda.sdc.proto.model.biceps.AbstractComponentReportMsg;
import org.somda.sdc.proto.model.biceps.AbstractContextReportMsg;
import org.somda.sdc.proto.model.biceps.AbstractMetricReportMsg;
import org.somda.sdc.proto.model.biceps.AbstractOperationalStateReportMsg;
import org.somda.sdc.proto.model.biceps.AbstractReportMsg;
import org.somda.sdc.proto.model.biceps.AbstractSetMsg;
import org.somda.sdc.proto.model.biceps.AbstractSetResponseMsg;
import org.somda.sdc.proto.model.biceps.ActivateMsg;
import org.somda.sdc.proto.model.biceps.ActivateResponseMsg;
import org.somda.sdc.proto.model.biceps.EpisodicAlertReportMsg;
import org.somda.sdc.proto.model.biceps.EpisodicComponentReportMsg;
import org.somda.sdc.proto.model.biceps.EpisodicContextReportMsg;
import org.somda.sdc.proto.model.biceps.EpisodicMetricReportMsg;
import org.somda.sdc.proto.model.biceps.EpisodicOperationalStateReportMsg;
import org.somda.sdc.proto.model.biceps.InvocationErrorMsg;
import org.somda.sdc.proto.model.biceps.InvocationInfoMsg;
import org.somda.sdc.proto.model.biceps.InvocationStateMsg;
import org.somda.sdc.proto.model.biceps.MdibVersionGroupMsg;
import org.somda.sdc.proto.model.biceps.OperationInvokedReportMsg;
import org.somda.sdc.proto.model.biceps.SetStringMsg;
import org.somda.sdc.proto.model.biceps.SetStringResponseMsg;
import org.somda.sdc.proto.model.biceps.WaveformStreamMsg;

import java.util.stream.Collectors;

public class PojoToProtoMapper {
    private static final Logger LOG = LogManager.getLogger(PojoToProtoMapper.class);
    private final Logger instanceLogger;
    private final PojoToProtoBaseMapper baseMapper;
    private final PojoToProtoMetricMapper metricMapper;
    private final PojoToProtoOneOfMapper oneOfMapper;
    private final PojoToProtoAlertMapper alertMapper;

    @Inject
    PojoToProtoMapper(@Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier,
                      PojoToProtoBaseMapper baseMapper,
                      PojoToProtoAlertMapper alertMapper,
                      PojoToProtoMetricMapper metricMapper,
                      PojoToProtoOneOfMapper oneOfMapper) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.baseMapper = baseMapper;
        this.metricMapper = metricMapper;
        this.alertMapper = alertMapper;
        this.oneOfMapper = oneOfMapper;
    }

    public EpisodicMetricReportMsg mapEpisodicMetricReport(EpisodicMetricReport report) {
        var builder = EpisodicMetricReportMsg.newBuilder()
                .setAbstractMetricReport(mapAbstractMetricReport(report));
        return builder.build();
    }

    public EpisodicAlertReportMsg mapEpisodicAlertReport(EpisodicAlertReport report) {
        var builder = EpisodicAlertReportMsg.newBuilder()
                .setAbstractAlertReport(mapAbstractAlertReport(report));
        return builder.build();
    }

    public EpisodicComponentReportMsg mapEpisodicComponentReport(EpisodicComponentReport report) {
        var builder = EpisodicComponentReportMsg.newBuilder()
                .setAbstractComponentReport(mapAbstractComponentReport(report));
        return builder.build();
    }

    public EpisodicContextReportMsg mapEpisodicContextReport(EpisodicContextReport report) {
        var builder = EpisodicContextReportMsg.newBuilder()
                .setAbstractContextReport(mapAbstractContextReport(report));
        return builder.build();
    }

    public EpisodicOperationalStateReportMsg mapEpisodicOperationalStateReport(EpisodicOperationalStateReport report) {
        var builder = EpisodicOperationalStateReportMsg.newBuilder()
                .setAbstractOperationalStateReport(mapAbstractOperationalStateReport(report));
        return builder.build();
    }

    public WaveformStreamMsg mapWaveformStream(WaveformStream report) {
        var builder = WaveformStreamMsg.newBuilder()
                .setAbstractReport(mapAbstractReport(report));
        report.getState().forEach(state -> builder.addState(metricMapper.mapRealTimeSampleArrayMetricState(state)));
        return builder.build();
    }

    public AbstractMetricReportMsg mapAbstractMetricReport(AbstractMetricReport report) {
        var builder = AbstractMetricReportMsg.newBuilder()
                .setAbstractReport(mapAbstractReport(report));
        report.getReportPart().forEach(part -> builder.addReportPart(mapAbstractMetricReportReportPart(part)));
        return builder.build();
    }

    public AbstractMetricReportMsg.ReportPartMsg mapAbstractMetricReportReportPart(AbstractMetricReport.ReportPart reportPart) {
        var builder = AbstractMetricReportMsg.ReportPartMsg.newBuilder();
        reportPart.getMetricState().forEach(state -> builder.addMetricState(oneOfMapper.mapAbstractMetricStateOneOf(state)));
        Util.doIfNotNull(reportPart.getSourceMds(), reportPart::setSourceMds);
        return builder.build();
    }

    public AbstractAlertReportMsg mapAbstractAlertReport(AbstractAlertReport report) {
        var builder = AbstractAlertReportMsg.newBuilder()
                .setAbstractReport(mapAbstractReport(report));
        report.getReportPart().forEach(part -> builder.addReportPart(mapAbstractAlertReportMsgReportPart(part)));
        return builder.build();
    }

    public AbstractAlertReportMsg.ReportPartMsg mapAbstractAlertReportMsgReportPart(AbstractAlertReport.ReportPart reportPart) {
        var builder = AbstractAlertReportMsg.ReportPartMsg.newBuilder();
        reportPart.getAlertState().forEach(state -> builder.addAlertState(oneOfMapper.mapAbstractAlertStateOneOf(state)));
        Util.doIfNotNull(reportPart.getSourceMds(), reportPart::setSourceMds);
        return builder.build();
    }

    public AbstractComponentReportMsg mapAbstractComponentReport(AbstractComponentReport report) {
        var builder = AbstractComponentReportMsg.newBuilder()
                .setAbstractReport(mapAbstractReport(report));
        report.getReportPart().forEach(part -> builder.addReportPart(mapAbstractComponentReportReportPart(part)));
        return builder.build();
    }

    public AbstractComponentReportMsg.ReportPartMsg mapAbstractComponentReportReportPart(AbstractComponentReport.ReportPart reportPart) {
        var builder = AbstractComponentReportMsg.ReportPartMsg.newBuilder();
        reportPart.getComponentState().forEach(state -> builder.addComponentState(oneOfMapper.mapAbstractDeviceComponentStateOneOf(state)));
        Util.doIfNotNull(reportPart.getSourceMds(), reportPart::setSourceMds);
        return builder.build();
    }

    public AbstractContextReportMsg mapAbstractContextReport(AbstractContextReport report) {
        var builder = AbstractContextReportMsg.newBuilder()
                .setAbstractReport(mapAbstractReport(report));
        report.getReportPart().forEach(part -> builder.addReportPart(mapAbstractContextReportReportPart(part)));
        return builder.build();
    }

    public AbstractContextReportMsg.ReportPartMsg mapAbstractContextReportReportPart(AbstractContextReport.ReportPart reportPart) {
        var builder = AbstractContextReportMsg.ReportPartMsg.newBuilder();
        reportPart.getContextState().forEach(state -> builder.addContextState(oneOfMapper.mapAbstractContextStateOneOf(state)));
        Util.doIfNotNull(reportPart.getSourceMds(), reportPart::setSourceMds);
        return builder.build();
    }

    public AbstractOperationalStateReportMsg mapAbstractOperationalStateReport(AbstractOperationalStateReport report) {
        var builder = AbstractOperationalStateReportMsg.newBuilder()
                .setAbstractReport(mapAbstractReport(report));
        report.getReportPart().forEach(part -> builder.addReportPart(mapAbstractOperationalStateReportReportPart(part)));
        return builder.build();
    }

    public AbstractOperationalStateReportMsg.ReportPartMsg mapAbstractOperationalStateReportReportPart(AbstractOperationalStateReport.ReportPart reportPart) {
        var builder = AbstractOperationalStateReportMsg.ReportPartMsg.newBuilder();
        reportPart.getOperationState().forEach(state -> builder.addOperationState(oneOfMapper.mapAbstractOperationStateOneOf(state)));
        Util.doIfNotNull(reportPart.getSourceMds(), reportPart::setSourceMds);
        return builder.build();
    }

    public AbstractReportMsg mapAbstractReport(AbstractReport report) {
        var builder = AbstractReportMsg.newBuilder();

        var mdibVersionGroup = MdibVersionGroupMsg.newBuilder();
        Util.doIfNotNull(report.getInstanceId(), instanceId ->
                mdibVersionGroup.setAInstanceId(Util.toUInt64(instanceId))
        );
        mdibVersionGroup.setASequenceId(report.getSequenceId());
        Util.doIfNotNull(report.getMdibVersion(), version ->
                mdibVersionGroup.setAMdibVersion(Util.toUInt64(version))
        );

        builder.setAMdibVersionGroup(mdibVersionGroup);
        return builder.build();
    }

    public ActivateResponseMsg mapActivateResponse(ActivateResponse pojo) {
        return ActivateResponseMsg.newBuilder().setAbstractSetResponse(mapAbstractSetResponse(pojo)).build();
    }

    public SetStringResponseMsg mapSetStringResponse(SetStringResponse pojo) {
        return SetStringResponseMsg.newBuilder().setAbstractSetResponse(mapAbstractSetResponse(pojo)).build();
    }

    public OperationInvokedReportMsg mapOperationInvokedReport(OperationInvokedReport pojo) {
        var builder = OperationInvokedReportMsg.newBuilder();

        var reportParts = pojo.getReportPart();
        builder.addAllReportPart(reportParts.stream().map(reportPart -> {
            var reportPartBuilder = OperationInvokedReportMsg.ReportPartMsg.newBuilder();

            // todo map AbstractReportPartMsg
            //reportPartBuilder.setAbstractReportPart()
            reportPartBuilder.setAOperationHandleRef(reportPart.getOperationHandleRef());
            Util.doIfNotNull(reportPart.getOperationTarget(), it ->
                    reportPartBuilder.setAOperationTarget(Util.toStringValue(it)));
            reportPartBuilder.setInvocationInfo(mapInvocationInfo(reportPart.getInvocationInfo()));
            reportPartBuilder.setInvocationSource(
                    baseMapper.mapInstanceIdentifierOneOf(reportPart.getInvocationSource()));
            return reportPartBuilder.build();
        }).collect(Collectors.toList()));
        builder.setAbstractReport(mapAbstractReport(pojo));
        return builder.build();
    }

    public AbstractSetResponseMsg mapAbstractSetResponse(AbstractSetResponse pojo) {
        var builder = AbstractSetResponseMsg.newBuilder();
        var mdibVersionGroup = MdibVersionGroupMsg.newBuilder();
        Util.doIfNotNull(pojo.getInstanceId(), instanceId ->
                mdibVersionGroup.setAInstanceId(Util.toUInt64(instanceId))
        );
        mdibVersionGroup.setASequenceId(pojo.getSequenceId());
        Util.doIfNotNull(pojo.getMdibVersion(), version ->
                mdibVersionGroup.setAMdibVersion(Util.toUInt64(version))
        );

        builder.setAMdibVersionGroup(mdibVersionGroup);
        builder.setInvocationInfo(mapInvocationInfo(pojo.getInvocationInfo()));
        return builder.build();
    }


    public AbstractSetMsg mapAbstractSet(AbstractSet pojo) {
        var builder = AbstractSetMsg.newBuilder();
        builder.setOperationHandleRef(pojo.getOperationHandleRef());
        return builder.build();
    }

    public InvocationInfoMsg mapInvocationInfo(InvocationInfo invocationInfo) {
        var builder = InvocationInfoMsg.newBuilder();
        Util.doIfNotNull(invocationInfo.getInvocationError(), it ->
                builder.setInvocationError(Util.mapToProtoEnum(it, InvocationErrorMsg.class)));
        Util.doIfNotNull(invocationInfo.getInvocationState(), it ->
                builder.setInvocationState(Util.mapToProtoEnum(it, InvocationStateMsg.class)));
        builder.addAllInvocationErrorMessage(baseMapper.mapLocalizedTexts(invocationInfo.getInvocationErrorMessage()));
        return builder.build();
    }

    public ActivateMsg mapActivate(Activate pojo) {
        var builder = ActivateMsg.newBuilder();
        builder.setAbstractSet(mapAbstractSet(pojo));
        // todo map arguments
        //pojo.getArgument()
        return builder.build();
    }

    public SetStringMsg mapSetString(SetString pojo) {
        var builder = SetStringMsg.newBuilder();
        builder.setAbstractSet(mapAbstractSet(pojo));
        builder.setRequestedStringValue(pojo.getRequestedStringValue());
        return builder.build();
    }
}
