package org.ieee11073.sdc.dpws.soap.wsdiscovery;

/**
 * Configuration of WS-Discovery package.
 */
public class WsDiscoveryConfig {
    /**
     * Control maximum waiting time for ProbeMatches messages.
     *
     * - Data type: {@linkplain java.time.Duration}
     * - Use: optional
     */
    public static final String MAX_WAIT_FOR_PROBE_MATCHES = "WsDiscovery.MaxWaitForProbeMatches";

    /**
     * Control maximum waiting time for ResolveMatches messages.
     *
     * - Data type: {@linkplain java.time.Duration}
     * - Use: optional
     */
    public static final String MAX_WAIT_FOR_RESOLVE_MATCHES = "WsDiscovery.MaxWaitForResolveMatches";

    /**
     * Control maximum buffer size for incoming ProbeMatches messages.
     *
     * - Data type: {@linkplain Integer}
     * - Use: optional
     */
    public static final String PROBE_MATCHES_BUFFER_SIZE = "WsDiscovery.MaxProbeMatchesBufferSize";

    /**
     * Control maximum buffer size for incoming ResolveMatches messages.
     *
     * - Data type: {@linkplain Integer}
     * - Use: optional
     */
    public static final String RESOLVE_MATCHES_BUFFER_SIZE = "WsDiscovery.MaxResolveMatchesBufferSize";
}
