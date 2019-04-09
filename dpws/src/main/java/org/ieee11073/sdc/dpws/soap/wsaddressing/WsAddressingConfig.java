package org.ieee11073.sdc.dpws.soap.wsaddressing;

/**
 * Configuration of WS-Addressing package.
 */
public class WsAddressingConfig {
    /**
     * Control maximum amount of cached SOAP messages for SOAP message duplication detection.
     *
     * - Data type: {@linkplain Integer}
     * - Use: optional
     */
    public static final String MESSAGE_ID_CACHE_SIZE = "WsAddressing.MessageIdCacheSize";

    /**
     * Set to true to let servers ignore message ids.
     *
     * - Data type: {@linkplain Boolean}
     * - Use: optional
     */
    public static final String IGNORE_MESSAGE_IDS = "WsAddressing.IgnoreMessageIds";
}
