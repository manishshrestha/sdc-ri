package org.ieee11073.sdc.dpws;

/**
 * Configuration of the DPWS top level package.
 */
public class DpwsConfig {
    /**
     * Control default waiting time for futures called internally.
     *
     * - Data type: {@linkplain java.time.Duration}
     * - Use: optional
     */
    public static final String MAX_WAIT_FOR_FUTURES = "Dpws.MaxWaitForFutures";
}
