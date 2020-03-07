package org.somda.sdc.glue.provider.services.helper;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.Subscribe;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.somda.sdc.glue.common.MdibVersionUtil;
import org.somda.sdc.glue.common.ReportMappings;

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
    private final MdibVersionUtil mdibVersionUtil;

    @AssistedInject
    ReportGenerator(@Assisted EventSourceAccess eventSourceAccess,
                    ObjectFactory bicepsMessageFactory,
                    ReportMappings reportMappings,
                    MdibVersionUtil mdibVersionUtil) {
        this.eventSourceAccess = eventSourceAccess;
        this.bicepsMessageFactory = bicepsMessageFactory;
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
                reportClass);
    }

    @Subscribe
    void onAlertChange(AlertStateModificationMessage modificationMessage) {
        sendStateChange(
                modificationMessage.getMdibAccess().getMdibVersion(),
                modificationMessage.getStates(),
                EpisodicAlertReport.class);
    }

    @Subscribe
    void onComponentChange(ComponentStateModificationMessage modificationMessage) {
        sendStateChange(
                modificationMessage.getMdibAccess().getMdibVersion(),
                modificationMessage.getStates(),
                EpisodicComponentReport.class);
    }

    @Subscribe
    void onContextChange(ContextStateModificationMessage modificationMessage) {
        sendStateChange(
                modificationMessage.getMdibAccess().getMdibVersion(),
                modificationMessage.getStates(),
                EpisodicContextReport.class);
    }

    @Subscribe
    void onMetricChange(MetricStateModificationMessage modificationMessage) {
        sendStateChange(
                modificationMessage.getMdibAccess().getMdibVersion(),
                modificationMessage.getStates(),
                EpisodicMetricReport.class);
    }

    @Subscribe
    void onOperationChange(OperationStateModificationMessage modificationMessage) {
        sendStateChange(
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

        try {
            mdibVersionUtil.setMdibVersion(modificationMessage.getMdibAccess().getMdibVersion(), report);
            eventSourceAccess.sendNotification(ActionConstants.ACTION_DESCRIPTION_MODIFICATION_REPORT, report);
        } catch (MarshallingException e) {
            LOG.warn("Could not marshal message for description modification report with version {}: {}",
                    modificationMessage.getMdibAccess().getMdibVersion(), e.getMessage());
        } catch (TransportException e) {
            LOG.info("Failed to deliver notification for description modification report with version {}: {}",
                    modificationMessage.getMdibAccess().getMdibVersion(), e.getMessage());
        } catch (ReflectiveOperationException e) {
            LOG.warn(REFLECTION_ERROR_STRING, e);
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

        try {
            WaveformStream waveformStream = new WaveformStream();
            mdibVersionUtil.setMdibVersion(mdibVersion, waveformStream);
            waveformStream.setState(states);
            eventSourceAccess.sendNotification(ActionConstants.ACTION_WAVEFORM_STREAM, waveformStream);
        } catch (MarshallingException e) {
            LOG.warn("Could not marshal message for state action {} with version: {}. {}",
                    ActionConstants.ACTION_WAVEFORM_STREAM, mdibVersion, e.getMessage());
        } catch (TransportException e) {
            LOG.info("Failed to deliver notification for state action {} with version: {}. {}",
                    ActionConstants.ACTION_WAVEFORM_STREAM, mdibVersion, e.getMessage());
        } catch (ReflectiveOperationException e) {
            LOG.warn(REFLECTION_ERROR_STRING, e);
        }
    }

    private <T, V extends AbstractReport> void sendStateChange(MdibVersion mdibVersion,
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

            mdibVersionUtil.setMdibVersion(mdibVersion, report);
            findSetStateMethod(reportPartClass).invoke(reportPart, states);

        } catch (ReflectiveOperationException e) {
            LOG.warn(REFLECTION_ERROR_STRING, e);
            return;
        }

        var action = reportMappings.getAction(reportClass);
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
}
