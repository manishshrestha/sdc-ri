package org.ieee11073.sdc.glue.common;

import com.google.inject.Inject;
import org.ieee11073.sdc.biceps.model.message.*;
import org.ieee11073.sdc.biceps.model.participant.*;

import java.util.*;

public class ReportMappings {
    private final Map<Class<? extends AbstractReport>, Class<? extends AbstractState>> reportStateMapping;
    private final Map<Class<? extends AbstractState>, Class<? extends AbstractReport>> episodicStateReportMapping;
    private final Map<Class<? extends AbstractReport>, String> reportActionMapping;

    private final Set<Class<? extends AbstractReport>> reportMessageTypes;
    private final Set<Class<? extends AbstractState>> reportStateTypes;

    @Inject
    ReportMappings() {
        Map<Class<? extends AbstractReport>, Class<? extends AbstractState>> modifiableReportStateMapping = new HashMap<>();
        modifiableReportStateMapping.put(AbstractAlertReport.class, AbstractAlertState.class);
        modifiableReportStateMapping.put(AbstractComponentReport.class, AbstractDeviceComponentState.class);
        modifiableReportStateMapping.put(AbstractContextReport.class, AbstractContextState.class);
        modifiableReportStateMapping.put(AbstractMetricReport.class, AbstractMetricState.class);
        modifiableReportStateMapping.put(AbstractOperationalStateReport.class, AbstractOperationState.class);
        reportStateMapping = Collections.unmodifiableMap(modifiableReportStateMapping);
        reportMessageTypes = Collections.unmodifiableSet(reportStateMapping.keySet());

        final Map<Class<? extends AbstractState>, Class<? extends AbstractReport>> modifiableEpisodicStateReportMapping = new HashMap<>();
        modifiableEpisodicStateReportMapping.put(AbstractAlertState.class, EpisodicAlertReport.class);
        modifiableEpisodicStateReportMapping.put(AbstractDeviceComponentState.class, EpisodicComponentReport.class);
        modifiableEpisodicStateReportMapping.put(AbstractContextState.class, EpisodicContextReport.class);
        modifiableEpisodicStateReportMapping.put(AbstractMetricState.class, EpisodicMetricReport.class);
        modifiableEpisodicStateReportMapping.put(AbstractOperationState.class, EpisodicOperationalStateReport.class);
        episodicStateReportMapping = Collections.unmodifiableMap(modifiableEpisodicStateReportMapping);

        Set<Class<? extends AbstractState>> modifiableReportStateTypes = new HashSet<>();
        modifiableReportStateTypes.add(AbstractAlertState.class);
        modifiableReportStateTypes.add(AbstractDeviceComponentState.class);
        modifiableReportStateTypes.add(AbstractContextState.class);
        modifiableReportStateTypes.add(AbstractMetricState.class);
        modifiableReportStateTypes.add(AbstractOperationState.class);
        reportStateTypes = Collections.unmodifiableSet(modifiableReportStateTypes);

        Map<Class<? extends AbstractReport>, String> modifiableReportActionMapping = new HashMap<>();
        modifiableReportActionMapping.put(AbstractAlertReport.class, ActionConstants.ACTION_EPISODIC_ALERT_REPORT);
        modifiableReportActionMapping.put(AbstractComponentReport.class, ActionConstants.ACTION_EPISODIC_COMPONENT_REPORT);
        modifiableReportActionMapping.put(AbstractContextReport.class, ActionConstants.ACTION_EPISODIC_CONTEXT_REPORT);
        modifiableReportActionMapping.put(AbstractMetricReport.class, ActionConstants.ACTION_EPISODIC_METRIC_REPORT);
        modifiableReportActionMapping.put(AbstractOperationalStateReport.class, ActionConstants.ACTION_PERIODIC_OPERATIONAL_STATE_REPORT);
        reportActionMapping = Collections.unmodifiableMap(modifiableReportActionMapping);
    }

    public Class<? extends AbstractReport> getEpisodicReportClass(Class<? extends AbstractState> stateClass) {
        Class<?> superClass = stateClass;
        while (superClass != null) {
            final Class<? extends AbstractReport> reportClass = episodicStateReportMapping.get(superClass);
            if (reportClass != null) {
                return reportClass;
            }
            superClass = superClass.getSuperclass();
        }
        throw new RuntimeException(String.format("Unknown state class found: %s", stateClass));
    }

    public String getEpisodicAction(Class<? extends AbstractReport> reportClass) {
        Class<?> superClass = reportClass;
        while (superClass != null) {
            final String action = reportActionMapping.get(superClass);
            if (action != null) {
                return action;
            }
            superClass = superClass.getSuperclass();
        }
        throw new RuntimeException(String.format("Unknown report class found: %s", reportClass));
    }
}
