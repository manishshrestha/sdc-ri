package com.example.consumer1;

import com.example.Constants;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.biceps.common.access.MdibAccessObserver;
import org.somda.sdc.biceps.common.event.AbstractMdibAccessMessage;
import org.somda.sdc.biceps.common.event.AlertStateModificationMessage;
import org.somda.sdc.biceps.common.event.ContextStateModificationMessage;
import org.somda.sdc.biceps.common.event.MetricStateModificationMessage;
import org.somda.sdc.biceps.common.event.OperationStateModificationMessage;
import org.somda.sdc.biceps.common.event.WaveformStateModificationMessage;

/**
 * This class handles incoming reports on the provider.
 * <p>
 * Every incoming report triggers the respective handler, or the generic onUpdate handler if no
 * specialized handler is found.
 */
public class ConsumerReportProcessor implements MdibAccessObserver {
    private static final Logger LOG = LoggerFactory.getLogger(ConsumerReportProcessor.class);

    public int numMetricChanges = 0;
    public int numConditionChanges = 0;

    @Subscribe
    void onUpdate(AbstractMdibAccessMessage updates) {
        LOG.debug("onUpdate: {}", updates.toString());
    }

    @Subscribe
    void onMetricChange(MetricStateModificationMessage modificationMessage) {
        LOG.info("onMetricChange");
        modificationMessage.getStates().forEach(
                state -> {
                    LOG.info(state.toString());
                    if (state.getDescriptorHandle().equals(Constants.HANDLE_NUMERIC_DYNAMIC)) {
                        numMetricChanges++;
                        LOG.info("{} has changed", Constants.HANDLE_NUMERIC_DYNAMIC);
                    }
                }
        );
    }

    @Subscribe
    void onWaveformChange(WaveformStateModificationMessage modificationMessage) {
        LOG.info("New waveform");
    }

    @Subscribe
    void onContextChange(ContextStateModificationMessage modificationMessage) {
        LOG.info("Context change");
    }

    @Subscribe
    void onAlertChange(AlertStateModificationMessage modificationMessage) {
        LOG.info("onAlertChange");
        modificationMessage.getStates().forEach(state -> {
            if (state.getDescriptorHandle().equals(Constants.HANDLE_ALERT_CONDITION)) {
                numConditionChanges++;
                LOG.info("{} has changed", Constants.HANDLE_ALERT_CONDITION);
            }
        });
    }

    @Subscribe
    void onOperationChange(OperationStateModificationMessage modificationMessage) {
        LOG.info("onOperationChange");
    }

}
