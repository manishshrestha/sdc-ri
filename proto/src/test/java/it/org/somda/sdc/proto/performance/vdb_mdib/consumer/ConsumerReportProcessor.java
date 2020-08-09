package it.org.somda.sdc.proto.performance.vdb_mdib.consumer;

import com.example.Constants;
import com.google.common.eventbus.Subscribe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.common.access.MdibAccessObserver;
import org.somda.sdc.biceps.common.event.AbstractMdibAccessMessage;
import org.somda.sdc.biceps.common.event.AlertStateModificationMessage;
import org.somda.sdc.biceps.common.event.ContextStateModificationMessage;
import org.somda.sdc.biceps.common.event.DescriptionModificationMessage;
import org.somda.sdc.biceps.common.event.MetricStateModificationMessage;
import org.somda.sdc.biceps.common.event.OperationStateModificationMessage;
import org.somda.sdc.biceps.common.event.WaveformStateModificationMessage;

import java.time.Instant;

/**
 * This class handles incoming reports on the provider.
 * <p>
 * Every incoming report triggers the respective handler, or the generic onUpdate handler if no
 * specialized handler is found.
 */
public class ConsumerReportProcessor implements MdibAccessObserver {
    private static final Logger LOG = LogManager.getLogger(com.example.consumer1.ConsumerReportProcessor.class);

    public int numMetricChanges = 0;
    public int numConditionChanges = 0;
    public int waveformReportFrequency = 5;
    public long nextWaveformInfo = Instant.now().plusSeconds(waveformReportFrequency).toEpochMilli();
    public long waveformCount = 0;
    public long waveformStateCount = 0;

    @Subscribe
    void onUpdate(AbstractMdibAccessMessage updates) {
        if (updates instanceof WaveformStateModificationMessage) {
            // too spammy, needs separate handler
            return;
        }

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
//        LOG.info("New waveform");
            waveformCount++;
            waveformStateCount += modificationMessage.getStates().size();
            if (nextWaveformInfo < System.currentTimeMillis()) {
                LOG.info(
                        "Processed {} reports with a total of {} states over the last {}s",
                        waveformCount, waveformStateCount, waveformReportFrequency
                );
                waveformCount = 0;
                waveformStateCount = 0;
                nextWaveformInfo = Instant.now().plusSeconds(5).toEpochMilli();
            }
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

    @Subscribe
    void onDescriptionChange(DescriptionModificationMessage modificationMessage) {
        LOG.info("onDescriptionChange");
    }
}