package org.somda.sdc.glue.provider.services.helper;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
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
import org.somda.sdc.biceps.common.event.WaveformStateModificationMessage;
import org.somda.sdc.biceps.model.message.AbstractAlertReport;
import org.somda.sdc.biceps.model.message.AbstractComponentReport;
import org.somda.sdc.biceps.model.message.AbstractContextReport;
import org.somda.sdc.biceps.model.message.AbstractMetricReport;
import org.somda.sdc.biceps.model.message.AbstractOperationalStateReport;
import org.somda.sdc.biceps.model.message.AbstractReport;
import org.somda.sdc.biceps.model.message.AbstractReportPart;
import org.somda.sdc.biceps.model.message.DescriptionModificationReport;
import org.somda.sdc.biceps.model.message.DescriptionModificationType;
import org.somda.sdc.biceps.model.message.EpisodicAlertReport;
import org.somda.sdc.biceps.model.message.EpisodicComponentReport;
import org.somda.sdc.biceps.model.message.EpisodicContextReport;
import org.somda.sdc.biceps.model.message.EpisodicMetricReport;
import org.somda.sdc.biceps.model.message.EpisodicOperationalStateReport;
import org.somda.sdc.biceps.model.message.ObjectFactory;
import org.somda.sdc.biceps.model.message.WaveformStream;
import org.somda.sdc.biceps.model.participant.AbstractState;
import org.somda.sdc.biceps.model.participant.MdibVersion;
import org.somda.sdc.biceps.model.participant.RealTimeSampleArrayMetricState;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.dpws.device.EventSourceAccess;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.glue.common.ActionConstants;
import org.somda.sdc.glue.common.MdibVersionUtil;
import org.somda.sdc.glue.common.ReportMappings;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Helper class to generate and send reports.
 * <p>
 * As there is currently support for one MDS only ({@link org.somda.sdc.biceps.common.MdibEntity} does not come
 * with an MDS handle reference), no SourceMds attribute is set and hence all changes go into one report part.
 */
public class ReportGenerator implements MdibAccessObserver {
    private static final Logger LOG = LogManager.getLogger(ReportGenerator.class);
    private static final String REFLECTION_ERROR_STRING = "Reflection error caught. Sending of notification aborted.";
    private static final String CAST_ERROR_STRING = "Casting error caught. Sending of notification aborted.";

    private final EventSourceAccess eventSourceAccess;
    private final ReportMappings reportMappings;
    private final MdibVersionUtil mdibVersionUtil;
    private final Logger instanceLogger;

    @AssistedInject
    ReportGenerator(@Assisted EventSourceAccess eventSourceAccess,
                    ReportMappings reportMappings,
                    MdibVersionUtil mdibVersionUtil,
                    @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.eventSourceAccess = eventSourceAccess;
        this.reportMappings = reportMappings;
        this.mdibVersionUtil = mdibVersionUtil;
    }

    /**
     * Tries to send a periodic state event given states and an MDIB version.
     *
     * @param states      the states to put to the report.
     * @param mdibVersion the MDIB version used for the report.
     * @param <T>         the state type.
     */
    public <T extends AbstractState> void sendPeriodicStateReport(List<T> states, MdibVersion mdibVersion) {
        if (states.isEmpty()) {
            return;
        }

        var reportClass = reportMappings.getPeriodicReportClass(states.get(0).getClass());
        sendStateChange(
            mdibVersion,
            states,
            reportMappings.getPeriodicReportBuilder(reportClass),
            reportMappings.getReportPartBuilder(reportClass)
        );
    }

    @Subscribe
    void onAlertChange(AlertStateModificationMessage modificationMessage) {
        sendStateChange(
            modificationMessage.getMdibAccess().getMdibVersion(),
            modificationMessage.getStates(),
            EpisodicAlertReport.builder(),
            AbstractAlertReport.ReportPart.builder()
        );
    }

    @Subscribe
    void onComponentChange(ComponentStateModificationMessage modificationMessage) {
        sendStateChange(
            modificationMessage.getMdibAccess().getMdibVersion(),
            modificationMessage.getStates(),
            EpisodicComponentReport.builder(),
            AbstractComponentReport.ReportPart.builder()
        );
    }

    @Subscribe
    void onContextChange(ContextStateModificationMessage modificationMessage) {
        sendStateChange(
            modificationMessage.getMdibAccess().getMdibVersion(),
            modificationMessage.getStates(),
            EpisodicContextReport.builder(),
            AbstractContextReport.ReportPart.builder()
        );
    }

    @Subscribe
    void onMetricChange(MetricStateModificationMessage modificationMessage) {
        sendStateChange(
            modificationMessage.getMdibAccess().getMdibVersion(),
            modificationMessage.getStates(),
            EpisodicMetricReport.builder(),
            AbstractMetricReport.ReportPart.builder()
        );
    }

    @Subscribe
    void onOperationChange(OperationStateModificationMessage modificationMessage) {
        sendStateChange(
            modificationMessage.getMdibAccess().getMdibVersion(),
            modificationMessage.getStates(),
            EpisodicOperationalStateReport.builder(),
            AbstractOperationalStateReport.ReportPart.builder()
        );
    }

    @Subscribe
    void onDescriptionChange(DescriptionModificationMessage modificationMessage) {
        final var reportBuilder = DescriptionModificationReport.builder();
        reportBuilder.addReportPart(
            generateReportParts(DescriptionModificationType.DEL, modificationMessage.getDeletedEntities())
        );
        reportBuilder.addReportPart(
            generateReportParts(DescriptionModificationType.CRT, modificationMessage.getInsertedEntities())
        );
        reportBuilder.addReportPart(
            generateReportParts(DescriptionModificationType.UPT, modificationMessage.getUpdatedEntities())
        );

        try {
            mdibVersionUtil.setReportMdibVersion(modificationMessage.getMdibAccess().getMdibVersion(), reportBuilder);
            eventSourceAccess.sendNotification(ActionConstants.ACTION_DESCRIPTION_MODIFICATION_REPORT, reportBuilder.build());
        } catch (MarshallingException e) {
            instanceLogger.warn("Could not marshal message for description modification report with version {}: {}",
                    modificationMessage.getMdibAccess().getMdibVersion(), e.getMessage());
            instanceLogger.trace("Could not marshal message for description modification report", e);
        } catch (TransportException e) {
            instanceLogger.info(
                    "Failed to deliver notification for description modification report with version {}: {}",
                    modificationMessage.getMdibAccess().getMdibVersion(), e.getMessage());
            instanceLogger.trace("Failed to deliver notification for description modification report", e);
        }

        dispatchStateEvents(modificationMessage.getMdibAccess().getMdibVersion(),
                modificationMessage.getInsertedEntities(),
                modificationMessage.getUpdatedEntities());
    }

    @Subscribe
    void onWaveformChange(WaveformStateModificationMessage modificationMessage) {
        sendWaveformChange(
                modificationMessage.getMdibAccess().getMdibVersion(),
                modificationMessage.getStates()
        );
    }

    private void dispatchStateEvents(MdibVersion mdibVersion, List<MdibEntity> insertedEntities,
                                     List<MdibEntity> updatedEntities) {
        // expectedKeys (which pertains to the key size of the multimap below) = 6 because of the following event types
        // - alert changes
        // - component change
        // - context changes
        // - metric changes
        // - operation changes
        // - waveform changes
        final Multimap<Class<? extends AbstractReport>, AbstractState> classifiedStates = ArrayListMultimap.create(6,
                insertedEntities.size() + updatedEntities.size());

        collectStates(classifiedStates, insertedEntities);
        collectStates(classifiedStates, updatedEntities);

        for (Class<? extends AbstractReport> aClass : classifiedStates.keySet()) {
            sendStateChange(
                mdibVersion,
                classifiedStates.get(aClass),
                reportMappings.getEpisodicReportBuilder(aClass),
                reportMappings.getReportPartBuilder(aClass)
            );
        }
    }

    private void collectStates(Multimap<Class<?
            extends AbstractReport>, AbstractState> classifiedStates, List<MdibEntity> entities) {
        for (MdibEntity entity : entities) {
            classifiedStates.putAll(reportMappings.getEpisodicReportClass(entity.getStateClass()), entity.getStates());
        }
    }


    private List<DescriptionModificationReport.ReportPart> generateReportParts(
        DescriptionModificationType modType,
                              List<MdibEntity> entities) {

        return entities.stream().map(entity ->
            DescriptionModificationReport.ReportPart.builder()
                .addDescriptor(entity.getDescriptor())
                .addState(entity.getStates())
                .withParentDescriptor(entity.getParent().orElse(null))
                // todo DGr add source MDS here if available
                .withModificationType(modType)
            .build()
        ).collect(Collectors.toList());
    }

    private void sendWaveformChange(MdibVersion mdibVersion, List<RealTimeSampleArrayMetricState> states) {
        if (states.isEmpty()) {
            return;
        }

        try {
            var waveformStream = WaveformStream.builder();
            mdibVersionUtil.setReportMdibVersion(mdibVersion, waveformStream);
            waveformStream.withState(states);
            eventSourceAccess.sendNotification(ActionConstants.ACTION_WAVEFORM_STREAM, waveformStream.build());
        } catch (MarshallingException e) {
            instanceLogger.warn("Could not marshal message for state action {} with version: {}. {}",
                    ActionConstants.ACTION_WAVEFORM_STREAM, mdibVersion, e.getMessage());
            instanceLogger.trace("Could not marshal message for state action {}",
                    ActionConstants.ACTION_WAVEFORM_STREAM, e);
        } catch (TransportException e) {
            instanceLogger.info("Failed to deliver notification for state action {} with version: {}. {}",
                    ActionConstants.ACTION_WAVEFORM_STREAM, mdibVersion, e.getMessage());
            instanceLogger.trace("Failed to deliver notification for state action {}",
                    ActionConstants.ACTION_WAVEFORM_STREAM, e);
        }
    }



    private <T, V extends AbstractReport.Builder<?>, U extends AbstractReportPart.Builder<?>> void sendStateChange(
        MdibVersion mdibVersion, Collection<T> states, V reportClassBuilder, U reportPartBuilder
        ) {
        // todo DGr add source MDS somewhere in this function if available

        if (states.isEmpty()) {
            return;
        }

        mdibVersionUtil.setReportMdibVersion(mdibVersion, reportClassBuilder);

        try {
            // handle all the cases!
            if (reportClassBuilder instanceof AbstractAlertReport.Builder) {
                var partBuilder = (AbstractAlertReport.ReportPart.Builder) reportPartBuilder;
                partBuilder.addAlertState(states);
                ((AbstractAlertReport.Builder<?>) reportClassBuilder).addReportPart(partBuilder.build());
            } else if (reportClassBuilder instanceof AbstractComponentReport.Builder) {
                var partBuilder = (AbstractComponentReport.ReportPart.Builder) reportPartBuilder;
                partBuilder.addComponentState(states);
                ((AbstractComponentReport.Builder<?>) reportClassBuilder).addReportPart(partBuilder.build());
            } else if (reportClassBuilder instanceof AbstractContextReport.Builder) {
                var partBuilder = (AbstractContextReport.ReportPart.Builder) reportPartBuilder;
                partBuilder.addContextState(states);
                ((AbstractContextReport.Builder<?>) reportClassBuilder).addReportPart(partBuilder.build());
            } else if (reportClassBuilder instanceof AbstractMetricReport.Builder) {
                var partBuilder = (AbstractMetricReport.ReportPart.Builder) reportPartBuilder;
                partBuilder.addMetricState(states);
                ((AbstractMetricReport.Builder<?>) reportClassBuilder).addReportPart(partBuilder.build());
            } else if (reportClassBuilder instanceof AbstractOperationalStateReport.Builder) {
                var partBuilder = (AbstractOperationalStateReport.ReportPart.Builder) reportPartBuilder;
                partBuilder.addOperationState(states);
                ((AbstractOperationalStateReport.Builder<?>) reportClassBuilder).addReportPart(partBuilder.build());
            }
        } catch (ClassCastException e) {
            instanceLogger.warn(CAST_ERROR_STRING, e);
            return;
        }

        var action = reportMappings.getAction((Class<? extends AbstractReport.Builder<?>>) reportClassBuilder.getClass());
        try {
            eventSourceAccess.sendNotification(action, reportClassBuilder.build());
        } catch (MarshallingException e) {
            instanceLogger.warn("Could not marshal message for state action {} with version: {}. {}",
                    action, mdibVersion, e.getMessage());
            instanceLogger.trace("Could not marshal message for state action {}",
                    action, e);
        } catch (TransportException e) {
            instanceLogger.info("Failed to deliver notification for state action {} with version: {}. {}",
                    action, mdibVersion, e.getMessage());
            instanceLogger.trace("Failed to deliver notification for state action {}", action, e);
        }
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
}
