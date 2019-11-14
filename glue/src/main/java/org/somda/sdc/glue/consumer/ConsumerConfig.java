package org.somda.sdc.glue.consumer;

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
}
