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
     */
    public static final String COMMUNICATION_LOG_DIRECTORY = "Dpws.CommunicationLogDirectory";

    /**
     * Defines the timeout the http client uses when connecting to an endpoint.
     *
     * <ul>
     * <li>Data type: {@linkplain java.time.Duration}
     * <li>Use: optional
     * </ul>
     */
    public static final String HTTP_CLIENT_CONNECT_TIMEOUT = "Dpws.HttpClientConnectTimeout";

    /**
     * Defines the timeout the http client uses when reading a response.
     *
     * <ul>
     * <li>Data type: {@linkplain java.time.Duration}
     * <li>Use: optional
     * </ul>
     */
    public static final String HTTP_CLIENT_READ_TIMEOUT = "Dpws.HttpClientReadTimeout";

    /**
     * Enable gzip compression support in Client and Server.
     *
     * <ul>
     * <li>Data type: {@linkplain Boolean}
     * <li>Use: optional
     * </ul>
     */
    public static final String HTTP_GZIP_COMPRESSION = "Dpws.GzipCompression";

}
