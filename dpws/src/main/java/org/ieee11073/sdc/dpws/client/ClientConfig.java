package org.ieee11073.sdc.dpws.client;

import org.ieee11073.sdc.dpws.crypto.CryptoSettings;

/**
 * General configuration of client functionality.
 */
public class ClientConfig {
    /**
     * Control maximum waiting time to get ResolveMatches information fetched from WS-Discovery
     *
     * - Data type: {@linkplain java.time.Duration}
     * - Use: optional
     */
    public static final String MAX_WAIT_FOR_RESOLVE_MATCHES = "Dpws.Client.MaxWaitForResolveMatches";

    /**
     * Enable (true) or disable (false) watchdog for hosting services.
     *
     * - Data type: {@linkplain Boolean}
     * - Use: optional
     */
    public static final String ENABLE_WATCHDOG = "Dpws.Client.EnableWatchdog";

    /**
     * Configure period for watchdog jobs.
     *
     * - Data type: {@linkplain java.time.Duration}
     * - Use: optional
     */
    public static final String WATCHDOG_PERIOD = "Dpws.Client.WatchdogPeriod";

    /**
     * Configure auto-resolve for probes and hellos without XAddrs.
     *
     * - Data type: {@linkplain Boolean}
     * - Use: optional
     */
    public static final String AUTO_RESOLVE = "Dpws.Client.AutoResolve";

    /**
     * Used to retrieve SSL configuration.
     *
     * - Data type: {@link CryptoSettings}
     * - Use: optional
     */
    public static final String CRYPTO_SETTINGS = "Dpws.Client.CryptoSettings";
}
