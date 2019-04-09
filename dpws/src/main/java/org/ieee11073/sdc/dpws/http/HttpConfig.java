package org.ieee11073.sdc.dpws.http;

/**
 * HTTP configuration identifiers.
 */
public class HttpConfig {
    /**
     * Minimum number of a port range.
     *
     * This number is used to designate a minimum port number on HTTP server generation.
     *
     * - Data type: {@linkplain Integer}
     * - Use: optional
     */
    public static final String PORT_MIN = "Http.PortMin";

    /**
     * Maximum number of a port range.
     *
     * This number is used to designate a maximum port number on HTTP server generation.
     *
     * - Data type: {@linkplain Integer}
     * - Use: optional
     */
    public static final String PORT_MAX = "Http.PortMax";
}
