package org.somda.sdc.dpws;

import org.somda.sdc.dpws.http.jetty.JettyHttpServerHandler;

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
     * <p>
     * Implementations of {@link CommunicationLogSink} may ignore this configuration item as not every sink
     * writes to files.
     *
     * <ul>
     * <li>Data type: {@linkplain java.io.File}
     * <li>Use: optional
     * </ul>
     */
    public static final String COMMUNICATION_LOG_SINK_DIRECTORY = "Dpws.CommunicationLogSinkDirectory";

    /**
     * Defines if the communication log shall include HTTP header information.
     * <p>
     * Implementations of {@link CommunicationLogSink} may ignore this configuration item.
     *
     * <ul>
     * <li>Data type: {@linkplain Boolean}
     * <li>Use: optional
     * </ul>
     */
    public static final String COMMUNICATION_LOG_WITH_HTTP_HEADERS = "Dpws.CommunicationLogWithHttpHeaders";

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

    /**
     * Minimum number of bytes a message needs to have in order to trigger compression.
     *
     * <ul>
     * <li>Data type: {@linkplain Integer}
     * <li>Use: optional
     * </ul>
     */
    public static final String HTTP_RESPONSE_COMPRESSION_MIN_SIZE = "Dpws.GzipCompressionMinSize";

    /**
     * Defines the timeout the http server uses for connections.
     *
     * <ul>
     * <li>Data type: {@linkplain Boolean}
     * <li>Use: optional
     * </ul>
     */
    public static final String HTTP_SERVER_CONNECTION_TIMEOUT = "Dpws.HttpServerConnectionTimeout";

    /**
     * Enables HTTPS communication for Client and Server.
     *
     * <ul>
     * <li>Data type: {@linkplain Boolean}
     * <li>Use: optional
     * </ul>
     */
    public static final String HTTPS_SUPPORT = "Dpws.EnableHttps";

    /**
     * Enables unsecured HTTP communication for Client and Server.
     *
     * <ul>
     * <li>Data type: {@linkplain Boolean}
     * <li>Use: optional
     * </ul>
     */
    public static final String HTTP_SUPPORT = "Dpws.EnableHttp";

    /**
     * Enforces chunked requests and responses.
     *
     * Note, that chunked responses and requests can still occur when this is turned off.
     *
     * <ul>
     * <li>Data type: {@linkplain Boolean}
     * <li>Use: optional
     * </ul>
     */
    public static final String ENFORCE_HTTP_CHUNKED_TRANSFER = "Dpws.EnforceHttpChunked";
}
