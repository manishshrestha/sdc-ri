package org.somda.sdc.glue.consumer;

/**
 * Configuration for the SDC consumer side.
 *
 * @see org.somda.sdc.glue.guice.DefaultGlueConfigModule
 */
public class ConsumerConfig {
    /**
     * Configures the period for {@linkplain SdcRemoteDevice} watchdog jobs.
     *
     * <ul>
     * <li>Data type: {@linkplain java.time.Duration}
     * <li>Use: optional
     * </ul>
     */
    public static final String WATCHDOG_PERIOD = "SdcGlue.Consumer.WatchdogPeriod";

    /**
     * Configures the default expiration time requested for subscribe requests.
     *
     * <ul>
     * <li>Data type: {@linkplain java.time.Duration}
     * <li>Use: optional
     * </ul>
     */
    public static final String REQUESTED_EXPIRES = "SdcGlue.Consumer.RequestedExpires";


    /**
     * Configures the default waiting time for any responses in request-response patterns.
     * <p>
     * <em>Please note that internally only seconds-precision is supported. Fractional parts will be cut off.</em>
     *
     * <ul>
     * <li>Data type: {@linkplain java.time.Duration}
     * <li>Use: optional
     * </ul>
     */
    public static final String RESPONSE_WAITING_TIME = "SdcGlue.Consumer.ResponseWaitingTime";

    /**
     * Default timeout for awaiting of transaction objects.
     * <p>
     * This duration is used to sort out stale reports.
     * Its value defines the threshold from which on a report counts as stale.
     *
     * <ul>
     * <li>Data type: {@linkplain java.time.Duration}
     * <li>Use: optional
     * </ul>
     */
    public static final String AWAITING_TRANSACTION_TIMEOUT = "SdcGlue.Consumer.AwaitingTransactionTimeout";

}
