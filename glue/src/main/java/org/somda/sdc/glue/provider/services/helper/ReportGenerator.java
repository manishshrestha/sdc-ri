package org.somda.sdc.glue.provider.services.helper;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.Subscribe;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.somda.sdc.biceps.common.MdibEntity;
import org.somda.sdc.biceps.common.access.MdibAccessObserver;
import org.somda.sdc.biceps.common.event.*;
import org.somda.sdc.biceps.model.message.*;
import org.somda.sdc.biceps.model.participant.AbstractState;
import org.somda.sdc.biceps.model.participant.MdibVersion;
import org.somda.sdc.biceps.model.participant.RealTimeSampleArrayMetricState;
import org.somda.sdc.dpws.device.EventSourceAccess;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.glue.common.ActionConstants;
import org.somda.sdc.glue.common.ReportMappings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

/**
 * Helper class to generate and send reports.
 * <p>
 * As there is currently support for one MDS only ({@link org.somda.sdc.biceps.common.MdibEntity} does not come
 * with an MDS handle reference), no SourceMds attribute is set and hence all changes go into one report part.
 */
public class ReportGenerator implements MdibAccessObserver {
    private static final Logger LOG = LoggerFactory.getLogger(ReportGenerator.class);
    private static final String REFLECTION_ERROR_STRING = "Reflection error caught. Sending of notification aborted.";

    private final EventSourceAccess eventSourceAccess;
    private final ObjectFactory bicepsMessageFactory;
    private final ReportMappings reportMappings;

    @AssistedInject
    ReportGenerator(@Assisted EventSourceAccess eventSourceAccess,
                    ObjectFactory bicepsMessageFactory,
                    ReportMappings reportMappings) {
        this.eventSourceAccess = eventSourceAccess;
        this.bicepsMessageFactory = bicepsMessageFactory;
        this.reportMappings = reportMappings;
    }

    @Subscribe
    void onAlertChange(AlertStateModificationMessage modificationMessage) {
        sendStateChange(ActionConstants.ACTION_EPISODIC_ALERT_REPORT,
                modificationMessage.getMdibAccess().getMdibVersion(),
                modificationMessage.getStates(),
                EpisodicAlertReport.class);
    }

    @Subscribe
    void onComponentChange(ComponentStateModificationMessage modificationMessage) {
        sendStateChange(ActionConstants.ACTION_EPISODIC_COMPONENT_REPORT,
                modificationMessage.getMdibAccess().getMdibVersion(),
                modificationMessage.getStates(),
                EpisodicComponentReport.class);
    }

    @Subscribe
    void onContextChange(ContextStateModificationMessage modificationMessage) {
        sendStateChange(ActionConstants.ACTION_EPISODIC_CONTEXT_REPORT,
                modificationMessage.getMdibAccess().getMdibVersion(),
                modificationMessage.getStates(),
                EpisodicContextReport.class);
    }

    @Subscribe
    void onMetricChange(MetricStateModificationMessage modificationMessage) {
        sendStateChange(ActionConstants.ACTION_EPISODIC_METRIC_REPORT,
                modificationMessage.getMdibAccess().getMdibVersion(),
                modificationMessage.getStates(),
                EpisodicMetricReport.class);
    }

    @Subscribe
    void onOperationChange(OperationStateModificationMessage modificationMessage) {
        sendStateChange(ActionConstants.ACTION_EPISODIC_OPERATIONAL_STATE_REPORT,
                modificationMessage.getMdibAccess().getMdibVersion(),
                modificationMessage.getStates(),
                EpisodicOperationalStateReport.class);
    }

    @Subscribe
    void onDescriptionChange(DescriptionModificationMessage modificationMessage) {
        final DescriptionModificationReport report = bicepsMessageFactory.createDescriptionModificationReport();
        appendReport(report, DescriptionModificationType.DEL, modificationMessage.getDeletedEntities());
        appendReport(report, DescriptionModificationType.CRT, modificationMessage.getInsertedEntities());
        appendReport(report, DescriptionModificationType.UPT, modificationMessage.getUpdatedEntities());
        populateMdibVersion(report, modificationMessage.getMdibAccess().getMdibVersion());

        try {
            eventSourceAccess.sendNotification(ActionConstants.ACTION_DESCRIPTION_MODIFICATION_REPORT, report);
        } catch (MarshallingException e) {
            LOG.warn("Could not marshal message for description modification report with version {}: {}",
                    modificationMessage.getMdibAccess().getMdibVersion(), e.getMessage());
        } catch (TransportException e) {
            LOG.info("Failed to deliver notification for description modification report with version {}: {}",
                    modificationMessage.getMdibAccess().getMdibVersion(), e.getMessage());
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

    private void dispatchStateEvents(MdibVersion mdibVersion, List<MdibEntity> insertedEntities, List<MdibEntity> updatedEntities) {
        // expectedKeys = 5 because of the following event types
        // 1. alert changes
        // 2. component change
        // 3. context changes
        // 4. metric changes
        // 5. operation changes
        final Multimap<Class<? extends AbstractReport>, AbstractState> classifiedStates = ArrayListMultimap.create(5,
                insertedEntities.size() + updatedEntities.size());

        collectStates(classifiedStates, insertedEntities);
        collectStates(classifiedStates, updatedEntities);

        for (Class<? extends AbstractReport> aClass : classifiedStates.keySet()) {
            sendStateChange(
                    reportMappings.getEpisodicAction(aClass),
                    mdibVersion,
                    classifiedStates.get(aClass),
                    aClass);
        }
    }

    private void collectStates(Multimap<Class<? extends AbstractReport>, AbstractState> classifiedStates, List<MdibEntity> entities) {
        for (MdibEntity entity : entities) {
            classifiedStates.putAll(reportMappings.getEpisodicReportClass(entity.getStateClass()), entity.getStates());
        }
    }


    private void appendReport(DescriptionModificationReport report,
                              DescriptionModificationType modType,
                              List<MdibEntity> entities) {
        for (MdibEntity entity : entities) {
            final DescriptionModificationReport.ReportPart reportPart = bicepsMessageFactory.createDescriptionModificationReportReportPart();
            reportPart.getDescriptor().add(entity.getDescriptor());
            reportPart.getState().addAll(entity.getStates());
            reportPart.setParentDescriptor(entity.getParent().orElse(null));
            reportPart.setModificationType(modType);
            // todo DGr add source MDS here if available
            report.getReportPart().add(reportPart);
        }
    }

    private void sendWaveformChange(MdibVersion mdibVersion, List<RealTimeSampleArrayMetricState> states) {
        if (states.isEmpty()) {
            return;
        }

        WaveformStream waveformStream = new WaveformStream();
        populateMdibVersion(waveformStream, mdibVersion);

        waveformStream.setState(states);

        try {
            eventSourceAccess.sendNotification(ActionConstants.ACTION_WAVEFORM_STREAM, waveformStream);
        } catch (MarshallingException e) {
            LOG.warn("Could not marshal message for state action {} with version: {}. {}",
                    ActionConstants.ACTION_WAVEFORM_STREAM, mdibVersion, e.getMessage());
        } catch (TransportException e) {
            LOG.info("Failed to deliver notification for state action {} with version: {}. {}",
                    ActionConstants.ACTION_WAVEFORM_STREAM, mdibVersion, e.getMessage());
        }
    }

    private <T, V extends AbstractReport> void sendStateChange(String action,
                                                               MdibVersion mdibVersion,
                                                               Collection<T> states,
                                                               Class<V> reportClass) {
        // todo DGr add source MDS somewhere in this function if available

        if (states.isEmpty()) {
            return;
        }

        V report = null;
        try {
            final Constructor<V> reportCtor = reportClass.getConstructor();
            report = reportCtor.newInstance();

            final Class<?> reportPartClass = findReportPartClass(reportClass);
            final Constructor<?> reportPartCtor = reportPartClass.getConstructor();
            final Object reportPart = reportPartCtor.newInstance();

            final Object reportParts = findGetReportPartMethod(reportClass).invoke(report);
            if (!List.class.isAssignableFrom(reportParts.getClass())) {
                throw new NoSuchMethodException(String.format("Returned report parts was not a list, it was of type %s",
                        reportParts.getClass()));
            }
            ((List) reportParts).add(reportPart);

            populateMdibVersion(report, mdibVersion);
            findSetStateMethod(reportPartClass).invoke(reportPart, states);

        } catch (ReflectiveOperationException e) {
            LOG.warn(REFLECTION_ERROR_STRING, e);
            return;
        }

        try {
            eventSourceAccess.sendNotification(action, report);
        } catch (MarshallingException e) {
            LOG.warn("Could not marshal message for state action {} with version: {}. {}",
                    action, mdibVersion, e.getMessage());
        } catch (TransportException e) {
            LOG.info("Failed to deliver notification for state action {} with version: {}. {}",
                    action, mdibVersion, e.getMessage());
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

    void populateMdibVersion(AbstractReport report, MdibVersion mdibVersion) {
        report.setSequenceId(mdibVersion.getSequenceId().toString());
        report.setInstanceId(mdibVersion.getInstanceId());
        report.setMdibVersion(mdibVersion.getVersion());
    }
}
