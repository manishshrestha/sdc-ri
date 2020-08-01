package org.somda.sdc.proto.mapping.message;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.message.AbstractAlertReport;
import org.somda.sdc.biceps.model.message.AbstractMetricReport;
import org.somda.sdc.biceps.model.message.AbstractReport;
import org.somda.sdc.biceps.model.message.EpisodicAlertReport;
import org.somda.sdc.biceps.model.message.EpisodicMetricReport;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.proto.mapping.Util;
import org.somda.sdc.proto.mapping.participant.PojoToProtoAlertMapper;
import org.somda.sdc.proto.mapping.participant.PojoToProtoBaseMapper;
import org.somda.sdc.proto.mapping.participant.PojoToProtoMetricMapper;
import org.somda.sdc.proto.mapping.participant.PojoToProtoOneOfMapper;
import org.somda.sdc.proto.model.biceps.AbstractAlertReportMsg;
import org.somda.sdc.proto.model.biceps.AbstractMetricReportMsg;
import org.somda.sdc.proto.model.biceps.AbstractReportMsg;
import org.somda.sdc.proto.model.biceps.EpisodicAlertReportMsg;
import org.somda.sdc.proto.model.biceps.EpisodicMetricReportMsg;
import org.somda.sdc.proto.model.biceps.MdibVersionGroupMsg;

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

    public AbstractMetricReportMsg mapAbstractMetricReport(AbstractMetricReport report) {
        var builder = AbstractMetricReportMsg.newBuilder()
                .setAbstractReport(mapAbstractReport(report));
        report.getReportPart().forEach(part -> builder.addReportPart(mapAbstractMetricReportReportPart(part)));
        return builder.build();
    }

    public AbstractMetricReportMsg.ReportPartMsg mapAbstractMetricReportReportPart(AbstractMetricReport.ReportPart reportPart) {
        var builder = AbstractMetricReportMsg.ReportPartMsg.newBuilder();
        reportPart.getMetricState().forEach(state -> builder.addMetricState(oneOfMapper.mapAbstractMetricStateOneOf(state)));
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
}
