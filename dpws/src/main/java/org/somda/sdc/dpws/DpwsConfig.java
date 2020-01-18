package org.somda.sdc.dpws;

/**
 * Configuration of the DPWS top level package.
 *
 * @see org.somda.sdc.dpws.guice.DefaultDpwsConfigModule
 */
public class DpwsConfig {
    /**
     * Controls the default waiting time for futures that are called internally.
     * <ul>
     * <li>Data type: {@linkplain java.time.Duration}
     * <li>Use: optional
     * </ul>
     */
    public static final String MAX_WAIT_FOR_FUTURES = "Dpws.MaxWaitForFutures";

    /**
     * Configures the maximum SOAP envelope size.
     *
     * <ul>
     * <li>Data type: {@linkplain Integer}
     * <li>Use: optional
     * </ul>
     *
     * @see DpwsConstants#MAX_ENVELOPE_SIZE
     */
    public static final String MAX_ENVELOPE_SIZE = "Dpws.MaxEnvelopeSize";

    /**
     * Defines the sink for communication log messages in case a communication logger is enabled.
     *
     * <ul>
     * <li>Data type: {@linkplain java.io.File}
     * <li>Use: optional
     * </ul>
     *
     */
    public static final String COMMUNICATION_LOG_DIRECTORY = "Dpws.CommunicationLogDirectory";
}
