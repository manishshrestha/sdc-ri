package org.somda.sdc.glue.provider.services.helper;

import com.google.common.collect.Lists;
import com.google.common.eventbus.Subscribe;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.common.MdibEntity;
import org.somda.sdc.biceps.common.access.MdibAccessObserver;
import org.somda.sdc.biceps.common.event.AlertStateModificationMessage;
import org.somda.sdc.biceps.common.event.ComponentStateModificationMessage;
import org.somda.sdc.biceps.common.event.ContextStateModificationMessage;
import org.somda.sdc.biceps.common.event.DescriptionModificationMessage;
import org.somda.sdc.biceps.common.event.MetricStateModificationMessage;
import org.somda.sdc.biceps.common.event.OperationStateModificationMessage;
import org.somda.sdc.biceps.model.history.ChangeSequenceReportType;
import org.somda.sdc.biceps.model.history.ChangeSequenceType;
import org.somda.sdc.biceps.model.history.HistoricMdibType;
import org.somda.sdc.biceps.model.history.HistoricReportType;
import org.somda.sdc.biceps.model.history.HistoryQueryType;
import org.somda.sdc.biceps.model.history.ObjectFactory;
import org.somda.sdc.biceps.model.history.VersionRangeType;
import org.somda.sdc.biceps.model.message.AbstractReport;
import org.somda.sdc.biceps.model.message.DescriptionModificationReport;
import org.somda.sdc.biceps.model.message.DescriptionModificationType;
import org.somda.sdc.biceps.model.message.EpisodicAlertReport;
import org.somda.sdc.biceps.model.message.EpisodicComponentReport;
import org.somda.sdc.biceps.model.message.EpisodicContextReport;
import org.somda.sdc.biceps.model.message.EpisodicMetricReport;
import org.somda.sdc.biceps.model.message.EpisodicOperationalStateReport;
import org.somda.sdc.biceps.model.participant.MdibVersion;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.device.EventSourceAccess;
import org.somda.sdc.glue.common.MdibMapper;
import org.somda.sdc.glue.common.MdibVersionUtil;
import org.somda.sdc.glue.common.factory.MdibMapperFactory;
import org.somda.sdc.glue.provider.ProviderConfig;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.somda.sdc.biceps.model.message.DescriptionModificationType.CRT;
import static org.somda.sdc.biceps.model.message.DescriptionModificationType.DEL;
import static org.somda.sdc.biceps.model.message.DescriptionModificationType.UPT;

/**
 * Observer tracks all MDIB description and state modifications and provides interface to query historical data.
 */
public class MdibRevisionObserver implements MdibAccessObserver {
    private static final Logger LOG = LogManager.getLogger(MdibRevisionObserver.class);
    private static final String REFLECTION_ERROR = "Reflection error caught. Report not persisted.";

    private final EventSourceAccess eventSourceAccess;
    private final MdibMapper mdibMapper;
    private final ObjectFactory objectFactory;
    private final org.somda.sdc.biceps.model.message.ObjectFactory messageObjectFactory;
    private final MdibVersionUtil mdibVersionUtil;
    private final Logger instanceLogger;
    private final Integer historicalReportsLimit;
    private final ChangeSequenceReportType historicalMdibStorage;

    @AssistedInject
    MdibRevisionObserver(@Assisted EventSourceAccess eventSourceAccess,
                         @Assisted LocalMdibAccess mdibAccess,
                         ObjectFactory objectFactory,
                         org.somda.sdc.biceps.model.message.ObjectFactory messageObjectFactory,
                         MdibMapperFactory mdibMapperFactory,
                         MdibVersionUtil mdibVersionUtil,
                         @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier,
                         @Named(ProviderConfig.MAX_HISTORICAL_REPORTS_PER_NOTIFICATION) Integer historicalReportsLimit) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.eventSourceAccess = eventSourceAccess;
        this.mdibMapper = mdibMapperFactory.createMdibMapper(mdibAccess);
        this.objectFactory = objectFactory;
        this.messageObjectFactory = messageObjectFactory;
        this.mdibVersionUtil = mdibVersionUtil;
        this.historicalReportsLimit = historicalReportsLimit;
        historicalMdibStorage = objectFactory.createChangeSequenceReportType();
    }

    /**
     * Creates initial historical MDIB.
     *
     * @param mdibVersion the initial version of MDIB used to get sequence and instance identifiers.
     */
    public void createInitialReport(MdibVersion mdibVersion) {
        var changeSequence = objectFactory.createChangeSequenceType();
        changeSequence.setSequenceId(mdibVersion.getSequenceId());
        changeSequence.setInstanceId(mdibVersion.getInstanceId());
        changeSequence.setHistoricMdib(createHistoricMdib()); // TODO #142 Option to opt-in initial MDIB to the reports.
        changeSequence.setHistoricReport(new ArrayList<>());
        changeSequence.setHistoricLocalizedText(new ArrayList<>()); //TODO #142 implement historical LocalizedTexts

        historicalMdibStorage.setChangeSequence(new ArrayList<>(List.of(changeSequence)));
    }

    /**
     * Gets historical MDIB data and reports based on provided {@linkplain HistoryQueryType} filter.
     *
     * @param query a filter used to get historical MDIB and reports.
     * @return a list of @{@linkplain ChangeSequenceReportType} with historical data.
     */
    public List<ChangeSequenceReportType> getChangeSequenceReport(HistoryQueryType query) {
        if (query.getVersionRange() != null) {
            return filterByVersion(query);
        }

        if (query.getTimeRange() != null) {
            //TODO #142: implement filtering by time
        }

        throw new RuntimeException("No 'VersionRange' nor 'TimeRange' filter given, but required.");
    }

    private List<ChangeSequenceReportType> filterByVersion(HistoryQueryType query) {
        var seqId = query.getVersionRange().getSequenceId();
        var instanceId = query.getVersionRange().getInstanceId();
        var changeSequence = findChangeSequence(seqId, instanceId);
        var filteredReports = changeSequence.getHistoricReport().stream()
                .filter(historicReportType -> isInVersionRange(query.getVersionRange(), historicReportType))
                .collect(Collectors.toList());
        return generateReportPartitions(filteredReports, changeSequence.getSequenceId(),
                changeSequence.getInstanceId(), changeSequence.getHistoricMdib());
    }

    private boolean isInVersionRange(VersionRangeType versionRange, HistoricReportType report) {
        var reportVersion = getReportVersion(report);
        return versionRange.getEndVersion().compareTo(reportVersion) >= 0
                && versionRange.getStartVersion().compareTo(reportVersion) <= 0;
    }

    private BigInteger getReportVersion(HistoricReportType historicReportType) {
        // not a nice approach but faster than reflection
        if (historicReportType.getDescriptionModificationReport() != null) {
            return historicReportType.getDescriptionModificationReport().getMdibVersion();
        }
        if (historicReportType.getEpisodicAlertReport() != null) {
            return historicReportType.getEpisodicAlertReport().getMdibVersion();
        }
        if (historicReportType.getEpisodicComponentReport() != null) {
            return historicReportType.getEpisodicComponentReport().getMdibVersion();
        }
        if (historicReportType.getEpisodicContextReport() != null) {
            return historicReportType.getEpisodicContextReport().getMdibVersion();
        }
        if (historicReportType.getEpisodicMetricReport() != null) {
            return historicReportType.getEpisodicMetricReport().getMdibVersion();
        }
        if (historicReportType.getEpisodicOperationalStateReport() != null) {
            return historicReportType.getEpisodicOperationalStateReport().getMdibVersion();
        }

        return null;
    }

    private List<ChangeSequenceReportType> generateReportPartitions(List<HistoricReportType> reports,
                                                                    String sequenceId,
                                                                    BigInteger instanceId,
                                                                    @Nullable HistoricMdibType historicMdib) {
        List<ChangeSequenceReportType> changeSequenceReports = new ArrayList<>();

        var reportPartitions = Lists.partition(reports, historicalReportsLimit);
        for (int i = 0; i < reportPartitions.size(); i++) {
            var changeSequenceCopy = objectFactory.createChangeSequenceType();
            changeSequenceCopy.setSequenceId(sequenceId);
            changeSequenceCopy.setInstanceId(instanceId);
            changeSequenceCopy.setHistoricReport(reportPartitions.get(i));
            if (i == 0 && historicMdib != null) { // only first report needs historic MDIB
                changeSequenceCopy.setHistoricMdib(historicMdib);
            }
            var report = objectFactory.createChangeSequenceReportType();
            report.setChangeSequence(Collections.singletonList(changeSequenceCopy));
            changeSequenceReports.add(report);
        }

        return changeSequenceReports;
    }

    @Subscribe
    void onDescriptionChange(DescriptionModificationMessage modificationMessage) {
        var report = messageObjectFactory.createDescriptionModificationReport();
        appendReport(report, DEL, modificationMessage.getDeletedEntities());
        appendReport(report, CRT, modificationMessage.getInsertedEntities());
        appendReport(report, UPT, modificationMessage.getUpdatedEntities());

        appendHistoricReport(modificationMessage.getMdibAccess().getMdibVersion(), report);
    }

    @Subscribe
    void onAlertChange(AlertStateModificationMessage modificationMessage) {
        persistStateChange(
                modificationMessage.getMdibAccess().getMdibVersion(),
                modificationMessage.getStates(),
                EpisodicAlertReport.class);
    }

    @Subscribe
    void onComponentChange(ComponentStateModificationMessage modificationMessage) {
        persistStateChange(
                modificationMessage.getMdibAccess().getMdibVersion(),
                modificationMessage.getStates(),
                EpisodicComponentReport.class);
    }

    @Subscribe
    void onContextChange(ContextStateModificationMessage modificationMessage) {
        persistStateChange(
                modificationMessage.getMdibAccess().getMdibVersion(),
                modificationMessage.getStates(),
                EpisodicContextReport.class);
    }

    @Subscribe
    void onMetricChange(MetricStateModificationMessage modificationMessage) {
        persistStateChange(
                modificationMessage.getMdibAccess().getMdibVersion(),
                modificationMessage.getStates(),
                EpisodicMetricReport.class);
    }

    @Subscribe
    void onMetricChange(OperationStateModificationMessage modificationMessage) {
        persistStateChange(
                modificationMessage.getMdibAccess().getMdibVersion(),
                modificationMessage.getStates(),
                EpisodicOperationalStateReport.class);
    }

    private HistoricMdibType createHistoricMdib() {
        /*
        //TODO #142 do we need to lock mdib / open new transaction while mapping??
        try (ReadTransaction transaction = mdibAccess.startTransaction()) {
            final MdibMapper mdibMapper = mdibMapperFactory.createMdibMapper(transaction);
            var mdib = mdibMapper.mapMdib();
        }
        */

        var historicMdib = objectFactory.createHistoricMdibType();
        historicMdib.setTime(Instant.now()); //TODO #142 really instant now?
        mdibMapper.mapMdib().copyTo(historicMdib);

        return historicMdib;
    }

    private void appendReport(DescriptionModificationReport report,
                              DescriptionModificationType modType,
                              List<MdibEntity> entities) {
        for (MdibEntity entity : entities) {
            var reportPart =
                    messageObjectFactory.createDescriptionModificationReportReportPart();
            reportPart.getDescriptor().add(entity.getDescriptor());
            reportPart.getState().addAll(entity.getStates());
            reportPart.setParentDescriptor(entity.getParent().orElse(null));
            reportPart.setModificationType(modType);
            report.getReportPart().add(reportPart);
        }
    }

    private <T, V extends AbstractReport> void persistStateChange(MdibVersion mdibVersion,
                                                                  Collection<T> states,
                                                                  Class<V> reportClass) {
        if (states.isEmpty()) {
            return;
        }

        try {
            var report = createNewReportWithStates(reportClass, states);
            appendHistoricReport(mdibVersion, report);
        } catch (ReflectiveOperationException e) {
            instanceLogger.warn(REFLECTION_ERROR, e);
        }
    }

    private <T extends AbstractReport> void appendHistoricReport(MdibVersion mdibVersion, T report) {
        try {
            mdibVersionUtil.setMdibVersion(mdibVersion, report);

            var historicReport = objectFactory.createHistoricReportType();
            findSetReportMethod(report.getClass()).invoke(historicReport, report);
            historicReport.setTime(Instant.now()); //TODO #142 which time to set?
            appendReportToChangeSequence(mdibVersion, historicReport);

        } catch (ReflectiveOperationException e) {
            instanceLogger.warn(REFLECTION_ERROR, e);
        }
    }

    private void appendReportToChangeSequence(MdibVersion mdibVersion, HistoricReportType historicReport) {
        ChangeSequenceType changeSequence = findChangeSequence(mdibVersion.getSequenceId(),
                mdibVersion.getInstanceId());
        changeSequence.getHistoricReport().add(historicReport);

    }

    private ChangeSequenceType findChangeSequence(String sequenceId, BigInteger instanceId) {
        return historicalMdibStorage.getChangeSequence().stream()
                .filter(cs -> cs.getSequenceId().equals(sequenceId) &&
                        cs.getInstanceId().equals(instanceId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        String.format("Cannot find change sequence for seqId: %s instanceId: %s", sequenceId,
                                instanceId)));
    }

    private <T, V extends AbstractReport> V createNewReportWithStates(Class<V> reportClass,
                                                                      Collection<T> states) throws ReflectiveOperationException {
        var report = reportClass.getConstructor().newInstance();
        var reportPartClass = findReportPartClass(reportClass);
        var reportPart = reportPartClass.getConstructor().newInstance();
        var reportParts = findGetReportPartMethod(reportClass).invoke(report);

        if (!List.class.isAssignableFrom(reportParts.getClass())) {
            throw new NoSuchMethodException(String.format("Returned report parts was not a list, it was of type %s",
                    reportParts.getClass()));
        }
        ((List) reportParts).add(reportPart);

        findSetStateMethod(reportPartClass).invoke(reportPart, states);

        return report;

    }

    private Class<?> findReportPartClass(Class<?> reportClass) throws NoSuchFieldException {
        final Class<?>[] classes = reportClass.getClasses();
        if (classes.length == 0) {
            throw new NoSuchFieldException(String.format("ReportPart inner class not found in %s",
                    reportClass.getName()));
        }
        return classes[0];
    }

    private Method findGetReportPartMethod(Class<?> reportPartClass) throws NoSuchMethodException {
        return reportPartClass.getMethod("getReportPart");
    }

    private Method findSetStateMethod(Class<?> reportPartClass) throws NoSuchMethodException {
        for (Method method : reportPartClass.getMethods()) {
            final Class<?>[] parameters = method.getParameterTypes();
            if (parameters.length == 1 && List.class.isAssignableFrom(parameters[0])) {
                return method;
            }
        }
        throw new NoSuchMethodException(String.format("No get-states function found on report part class %s",
                reportPartClass.getName()));
    }

    private Method findSetReportMethod(Class<?> reportClass) throws NoSuchMethodException {
        for (Method method : HistoricReportType.class.getMethods()) {
            var params = method.getParameterTypes();
            if (params.length == 1 && reportClass.isAssignableFrom(params[0])) {
                return method;
            }
        }
        throw new NoSuchMethodException("No set-report method found for report class " + reportClass.getName());
    }
}
