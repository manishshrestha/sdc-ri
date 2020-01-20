package com.example.consumer1;

import com.example.ProviderMdibConstants;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.biceps.common.access.MdibAccessObserver;
import org.somda.sdc.biceps.common.event.*;

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
                    if (state.getDescriptorHandle().equals(ProviderMdibConstants.HANDLE_NUMERIC_DYNAMIC)) {
                        numMetricChanges++;
                        LOG.info("{} has changed", ProviderMdibConstants.HANDLE_NUMERIC_DYNAMIC);
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
            if (state.getDescriptorHandle().equals(ProviderMdibConstants.HANDLE_ALERT_CONDITION)) {
                numConditionChanges++;
                LOG.info("{} has changed", ProviderMdibConstants.HANDLE_ALERT_CONDITION);
            }
        });

    }

}
