package org.somda.sdc.glue.consumer;

import org.apache.commons.lang3.ArrayUtils;
import org.somda.sdc.dpws.service.HostingServiceProxy;
import org.somda.sdc.glue.common.ActionConstants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Container to provide connection options for remote SDC device connections.
 *
 * @see SdcRemoteDevicesConnector#connect(HostingServiceProxy, ConnectConfiguration)
 */
public class ConnectConfiguration {
    /**
     * List of all episodic report actions.
     */
    public static String[] EPISODIC_REPORTS = {
            ActionConstants.ACTION_EPISODIC_ALERT_REPORT,
            ActionConstants.ACTION_EPISODIC_COMPONENT_REPORT,
            ActionConstants.ACTION_EPISODIC_CONTEXT_REPORT,
            ActionConstants.ACTION_EPISODIC_METRIC_REPORT,
            ActionConstants.ACTION_EPISODIC_OPERATIONAL_STATE_REPORT,
            ActionConstants.ACTION_DESCRIPTION_MODIFICATION_REPORT,
            ActionConstants.ACTION_OPERATION_INVOKED_REPORT};

    /**
     * List of all periodic report actions.
     */
    public static String[] PERIODIC_REPORTS = {
            ActionConstants.ACTION_PERIODIC_ALERT_REPORT,
            ActionConstants.ACTION_PERIODIC_COMPONENT_REPORT,
            ActionConstants.ACTION_PERIODIC_CONTEXT_REPORT,
            ActionConstants.ACTION_PERIODIC_METRIC_REPORT,
            ActionConstants.ACTION_PERIODIC_OPERATIONAL_STATE_REPORT};

    /**
     * List of all streaming actions.
     */
    public static String[] STREAMING_REPORTS = {
            ActionConstants.ACTION_OBSERVED_VALUE_STREAM,
            ActionConstants.ACTION_WAVEFORM_SERVICE};

    /**
     * Commonly used subscriptions for remote SDC device synchronization.
     * <p>
     * Comprises all episodic reports plus waveforms.
     */
    public static String[] ALL_EPISODIC_AND_WAVEFORM_REPORTS = ArrayUtils.addAll(EPISODIC_REPORTS, ActionConstants.ACTION_WAVEFORM_SERVICE);

    /**
     * Commonly used subscriptions if only updates on description and contexts are desired.
     * <p>
     * This can be used in order to watch remote devices without receiving all of their data.
     * Note that this setup includes operation invoked reports by default.
     */
    public static String[] DESCRIPTION_AND_CONTEXTS = {
            ActionConstants.ACTION_EPISODIC_CONTEXT_REPORT,
            ActionConstants.ACTION_DESCRIPTION_MODIFICATION_REPORT,
            ActionConstants.ACTION_OPERATION_INVOKED_REPORT};

    private Collection<String> subscriptions;

    /**
     * Creates a configuration that subscribes nothing.
     */
    public ConnectConfiguration() {
        this.subscriptions = Collections.EMPTY_LIST;
    }

    /**
     * Creates a configuration with predefined subscriptions.
     *
     * @param subscriptions the subscriptions to be used during connection.
     */
    public ConnectConfiguration(Collection<String> subscriptions) {
        this.subscriptions = new ArrayList<>(subscriptions);
    }
}
