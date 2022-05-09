package org.somda.sdc.glue.common;

import com.google.inject.Inject;
import org.somda.sdc.biceps.model.message.*;
import org.somda.sdc.biceps.model.participant.AbstractAlertState;
import org.somda.sdc.biceps.model.participant.AbstractContextState;
import org.somda.sdc.biceps.model.participant.AbstractDeviceComponentState;
import org.somda.sdc.biceps.model.participant.AbstractMetricState;
import org.somda.sdc.biceps.model.participant.AbstractOperationState;
import org.somda.sdc.biceps.model.participant.AbstractState;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Reflection utility that provides class mappings between reports and other information items.
 */
public class ReportMappings {
    private final Map<Class<? extends AbstractReport>, Class<? extends AbstractState>> reportStateMapping;
    private final Map<Class<? extends AbstractState>, Class<? extends AbstractReport>> episodicStateReportMapping;
    private final Map<Class<? extends AbstractState>, Class<? extends AbstractReport>> periodicStateReportMapping;
    private final Map<Class<? extends AbstractReport>, Supplier<? extends AbstractReport.Builder>> episodicStateReportBuilderMapping;
    private final Map<Class<? extends AbstractReport>, Supplier<? extends AbstractReport.Builder>> periodicStateReportBuilderMapping;
    private final Map<Class<? extends AbstractReport>, Supplier<? extends AbstractReportPart.Builder>> reportPartBuilderMapping;

    private final Map<Class<?>, String> episodicReportActionMapping;
    private final Map<Class<?>, String> periodicReportActionMapping;

    private final Set<Class<? extends AbstractReport>> reportMessageTypes;
    private final Set<Class<? extends AbstractState>> reportStateTypes;

    @Inject
    ReportMappings() {
        reportStateMapping = Map.of(
                AbstractAlertReport.class, AbstractAlertState.class,
                AbstractComponentReport.class, AbstractDeviceComponentState.class,
                AbstractContextReport.class, AbstractContextState.class,
                AbstractMetricReport.class, AbstractMetricState.class,
                AbstractOperationalStateReport.class, AbstractOperationState.class);
        reportMessageTypes = Collections.unmodifiableSet(reportStateMapping.keySet());

        episodicStateReportMapping = Map.of(
                AbstractAlertState.class, EpisodicAlertReport.class,
                AbstractDeviceComponentState.class, EpisodicComponentReport.class,
                AbstractContextState.class, EpisodicContextReport.class,
                AbstractMetricState.class, EpisodicMetricReport.class,
                AbstractOperationState.class, EpisodicOperationalStateReport.class);

        periodicStateReportMapping = Map.of(
                AbstractAlertState.class, PeriodicAlertReport.class,
                AbstractDeviceComponentState.class, PeriodicComponentReport.class,
                AbstractContextState.class, PeriodicContextReport.class,
                AbstractMetricState.class, PeriodicMetricReport.class,
                AbstractOperationState.class, PeriodicOperationalStateReport.class);

        episodicStateReportBuilderMapping = Map.of(
            EpisodicAlertReport.class, EpisodicAlertReport::builder,
            EpisodicComponentReport.class, EpisodicComponentReport::builder,
            EpisodicContextReport.class, EpisodicContextReport::builder,
            EpisodicMetricReport.class, EpisodicMetricReport::builder,
            EpisodicOperationalStateReport.class, EpisodicOperationalStateReport::builder
        );

        periodicStateReportBuilderMapping = Map.of(
            PeriodicAlertReport.class, PeriodicAlertReport::builder,
            PeriodicComponentReport.class, PeriodicComponentReport::builder,
            PeriodicContextReport.class, PeriodicContextReport::builder,
            PeriodicMetricReport.class, PeriodicMetricReport::builder,
            PeriodicOperationalStateReport.class, PeriodicOperationalStateReport::builder
        );

        reportPartBuilderMapping = Map.of(
            PeriodicAlertReport.class, AbstractAlertReport.ReportPart::builder,
            PeriodicComponentReport.class, AbstractComponentReport.ReportPart::builder,
            PeriodicContextReport.class, AbstractContextReport.ReportPart::builder,
            PeriodicMetricReport.class, AbstractMetricReport.ReportPart::builder,
            PeriodicOperationalStateReport.class, AbstractOperationalStateReport.ReportPart::builder,

            EpisodicAlertReport.class, AbstractAlertReport.ReportPart::builder,
            EpisodicComponentReport.class, AbstractComponentReport.ReportPart::builder,
            EpisodicContextReport.class, AbstractContextReport.ReportPart::builder,
            EpisodicMetricReport.class, AbstractMetricReport.ReportPart::builder,
            EpisodicOperationalStateReport.class, AbstractOperationalStateReport.ReportPart::builder
        );


        reportStateTypes = Set.of(
                AbstractAlertState.class,
                AbstractDeviceComponentState.class,
                AbstractContextState.class,
                AbstractMetricState.class,
                AbstractOperationState.class);

        episodicReportActionMapping = Map.of(
            EpisodicAlertReport.class, ActionConstants.ACTION_EPISODIC_ALERT_REPORT,
            EpisodicComponentReport.class, ActionConstants.ACTION_EPISODIC_COMPONENT_REPORT,
            EpisodicContextReport.class, ActionConstants.ACTION_EPISODIC_CONTEXT_REPORT,
            EpisodicMetricReport.class, ActionConstants.ACTION_EPISODIC_METRIC_REPORT,
            EpisodicOperationalStateReport.class, ActionConstants.ACTION_EPISODIC_OPERATIONAL_STATE_REPORT,

            EpisodicAlertReport.Builder.class, ActionConstants.ACTION_EPISODIC_ALERT_REPORT,
            EpisodicComponentReport.Builder.class, ActionConstants.ACTION_EPISODIC_COMPONENT_REPORT,
            EpisodicContextReport.Builder.class, ActionConstants.ACTION_EPISODIC_CONTEXT_REPORT,
            EpisodicMetricReport.Builder.class, ActionConstants.ACTION_EPISODIC_METRIC_REPORT,
            EpisodicOperationalStateReport.Builder.class, ActionConstants.ACTION_EPISODIC_OPERATIONAL_STATE_REPORT
        );

        periodicReportActionMapping = Map.of(
            PeriodicAlertReport.class, ActionConstants.ACTION_PERIODIC_ALERT_REPORT,
            PeriodicComponentReport.class, ActionConstants.ACTION_PERIODIC_COMPONENT_REPORT,
            PeriodicContextReport.class, ActionConstants.ACTION_PERIODIC_CONTEXT_REPORT,
            PeriodicMetricReport.class, ActionConstants.ACTION_PERIODIC_METRIC_REPORT,
            PeriodicOperationalStateReport.class, ActionConstants.ACTION_PERIODIC_OPERATIONAL_STATE_REPORT,

            PeriodicAlertReport.Builder.class, ActionConstants.ACTION_PERIODIC_ALERT_REPORT,
            PeriodicComponentReport.Builder.class, ActionConstants.ACTION_PERIODIC_COMPONENT_REPORT,
            PeriodicContextReport.Builder.class, ActionConstants.ACTION_PERIODIC_CONTEXT_REPORT,
            PeriodicMetricReport.Builder.class, ActionConstants.ACTION_PERIODIC_METRIC_REPORT,
            PeriodicOperationalStateReport.Builder.class, ActionConstants.ACTION_PERIODIC_OPERATIONAL_STATE_REPORT
        );
    }

    public Class<? extends AbstractReport> getEpisodicReportClass(Class<? extends AbstractState> stateClass) {
        return getReportClass(stateClass, episodicStateReportMapping);
    }

    public Class<? extends AbstractReport> getPeriodicReportClass(Class<? extends AbstractState> stateClass) {
        return getReportClass(stateClass, periodicStateReportMapping);
    }

    public AbstractReportPart.Builder<?> getReportPartBuilder(Class<? extends AbstractReport> reportClass) {
        return reportPartBuilderMapping.get(reportClass).get();

    }

    public AbstractReport.Builder<?> getEpisodicReportBuilder(Class<? extends AbstractReport> reportClass) {
        return episodicStateReportBuilderMapping.get(reportClass).get();
    }

    public AbstractReport.Builder<?> getPeriodicReportBuilder(Class<? extends AbstractReport> reportClass) {
        return periodicStateReportBuilderMapping.get(reportClass).get();
    }


    private static Class<? extends AbstractReport> getReportClass(Class<? extends AbstractState> stateClass,
                                                                  Map<Class<? extends AbstractState>,
                                                                          Class<? extends AbstractReport>> mapping) {
        Class<?> superClass = stateClass;
        while (superClass != null) {
            final Class<? extends AbstractReport> reportClass = mapping.get(superClass);
            if (reportClass != null) {
                return reportClass;
            }
            superClass = superClass.getSuperclass();
        }
        throw new RuntimeException(String.format("Unknown state class found: %s", stateClass));
    }

    public String getEpisodicAction(Class<? extends AbstractReport.Builder<?>> reportClass) {
        return getAction(reportClass, episodicReportActionMapping).orElseThrow(() ->
                new UnknownReportClassFoundException(reportClass));
    }

    public String getPeriodicAction(Class<? extends AbstractReport.Builder<?>> reportClass) {
        return getAction(reportClass, periodicReportActionMapping).orElseThrow(() ->
               new UnknownReportClassFoundException(reportClass));
    }

    public String getAction(Class<? extends AbstractReport.Builder<?>> reportClass) {
        return getAction(reportClass, episodicReportActionMapping).orElseGet(() ->
                getAction(reportClass, periodicReportActionMapping).orElseThrow(() ->
                            new UnknownReportClassFoundException(reportClass)));
    }

    private Optional<String> getAction(Class<? extends AbstractReport.Builder<?>> reportClass,
                                       Map<Class<?>, String> mapping) {
        return Optional.ofNullable(mapping.get(reportClass));
    }

    public static class UnknownReportClassFoundException extends RuntimeException {
        public UnknownReportClassFoundException(Class<? extends AbstractReport.Builder<?>> reportClass) {
            super(String.format("Unknown report class found: %s", reportClass));
        }
    }
}
