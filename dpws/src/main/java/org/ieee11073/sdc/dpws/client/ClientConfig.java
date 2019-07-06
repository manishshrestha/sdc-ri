package org.ieee11073.sdc.dpws.client;

/**
 * General configuration of client functionality.
 */
public class ClientConfig {
    /**
     * Control maximum waiting time to get ResolveMatches information fetched from WS-Discovery
     * <p>
     * - Data type: {@linkplain java.time.Duration}
     * - Use: optional
     */
    public static final String MAX_WAIT_FOR_RESOLVE_MATCHES = "Dpws.Client.MaxWaitForResolveMatches";

    /**
     * Enable (true) or disable (false) watchdog for hosting services.
     * <p>
     * - Data type: {@linkplain Boolean}
     * - Use: optional
     */
    public static final String ENABLE_WATCHDOG = "Dpws.Client.EnableWatchdog";

    /**
     * Configure period for watchdog jobs.
     * <p>
     * - Data type: {@linkplain java.time.Duration}
     * - Use: optional
     */
    public static final String WATCHDOG_PERIOD = "Dpws.Client.WatchdogPeriod";

    /**
     * Configure auto-resolve for probes and hellos without XAddrs.
     * <p>
     * - Data type: {@linkplain Boolean}
     * - Use: optional
     */
    public static final String AUTO_RESOLVE = "Dpws.Client.AutoResolve";
}
