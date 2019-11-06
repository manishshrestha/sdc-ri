package org.ieee11073.sdc.dpws.client;

/**
 * Configuration keys for the client.
 */
public class ClientConfig {
    /**
     * Controls maximum waiting time to get ResolveMatches information fetched from WS-Discovery.
     * <ul>
     * <li>Data type: {@linkplain java.time.Duration}
     * <li>Use: optional
     * </ul>
     */
    public static final String MAX_WAIT_FOR_RESOLVE_MATCHES = "Dpws.Client.MaxWaitForResolveMatches";

    /**
     * Enables (true) or disables (false) watchdog for hosting services.
     * <ul>
     * <li>Data type: {@linkplain Boolean}
     * <li>Use: optional
     * </ul>
     */
    public static final String ENABLE_WATCHDOG = "Dpws.Client.EnableWatchdog";

    /**
     * Configures the period for watchdog jobs.
     * <ul>
     * <li>Data type: {@linkplain java.time.Duration}
     * <li>Use: optional
     * </ul>
     */
    public static final String WATCHDOG_PERIOD = "Dpws.Client.WatchdogPeriod";

    /**
     * Configures auto-resolve for probes and hellos without XAddrs.
     * <ul>
     * <li>Data type: {@linkplain Boolean}
     * <li>Use: optional
     * </ul>
     */
    public static final String AUTO_RESOLVE = "Dpws.Client.AutoResolve";
}
