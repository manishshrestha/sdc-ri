package org.ieee11073.sdc.dpws;

import javax.xml.namespace.QName;
import java.time.Duration;

/**
 * DPWS constants.
 *
 * @see <a href="http://docs.oasis-open.org/ws-dd/dpws/1.1/os/wsdd-dpws-1.1-spec-os.html#_Toc228672112">Appendix B. Constants</a>
 */
public class DpwsConstants {
    /**
     * Defines the context package for JAXB.
     */
    public static final String JAXB_CONTEXT_PACKAGE = "org.ieee11073.sdc.dpws.model";

    /**
     * Defines the DPWS namespace.
     */
    public static final String NAMESPACE = "http://docs.oasis-open.org/ws-dd/ns/dpws/2009/01";

    /**
     * Defines the Application Level Transmission Delay defined in WS-Discovery, section 3.1.3.
     * <p>
     * Excerpt from WS-Discovery:
     * <blockquote>
     * As designated below, before sending some message types defined herein, a Target Service MUST wait for a timer
     * to elapse before sending the message using the bindings described above. This timer MUST be set to a random
     * value between 0 and APP_MAX_DELAY. Table 5 specifies the default value for this parameter.
     * </blockquote>
     */
    public static final Duration APP_MAX_DELAY = Duration.ofMillis(2_500L);

    /**
     * Defines the multicast port used to transmit discovery messages.
     */
    public static final int DISCOVERY_PORT = 3_702;

    /**
     * Defines the timeout after which a probe or resolve match shall be discarded.
     */
    public static final Duration MATCH_TIMEOUT = Duration.ofSeconds(10L);

    /**
     * Defines the maximum size for envelopes transmitted over TCP.
     * <p>
     * Unit: octets.
     * <p>
     * todo DGr MAX_ENVELOPE_SIZE should be configurable or increased such that it meets MDPWS's relaxed size of 4MB.
     */
    public static final int MAX_ENVELOPE_SIZE = 32_767;

    /**
     * Defines the maximum size for envelopes transmitted over UDP.
     * <p>
     * Unit: octets
     */
    public static final int MAX_UDP_ENVELOPE_SIZE = 4_096;

    /**
     * Defines the maximum size for different attributes introduced by DPWS.
     * <p>
     * Unit: Unicode characters
     */
    public static final int MAX_FIELD_SIZE = 256;

    /**
     * Defines the maximum site for URIs.
     * <p>
     * Unit: octets
     */
    public static final int MAX_URI_SIZE = 2_048;

    /**
     * Defines the retry number for unreliable UDP multicast traffic.
     */
    public static final int MULTICAST_UDP_REPEAT = 1;

    /**
     * Defines the maximum delay for the SOAP-over-UDP retransmission algorithm.
     */
    public static final Duration UDP_MAX_DELAY = Duration.ofMillis(250L);

    /**
     * Defines the minimum delay for the SOAP-over-UDP retransmission algorithm.
     */
    public static final Duration UDP_MIN_DELAY = Duration.ofMillis(50L);

    /**
     * Defines the upper delay for the SOAP-over-UDP retransmission algorithm.
     */
    public static final Duration UDP_UPPER_DELAY = Duration.ofMillis(450L);

    /**
     * Defines the retry number for unreliable UDP unicast traffic.
     */
    public static final int UNICAST_UDP_REPEAT = 1;

    /**
     * Defines the namespace for the DPWS ThisModel data structure.
     */
    public static final String MEX_DIALECT_THIS_MODEL = NAMESPACE + "/ThisModel";

    /**
     * Defines the namespace for the DPWS ThisDevice data structure.
     */
    public static final String MEX_DIALECT_THIS_DEVICE = NAMESPACE + "/ThisDevice";

    /**
     * Defines the namespace for the DPWS Relationship data structure.
     */
    public static final String MEX_DIALECT_RELATIONSHIP = NAMESPACE + "/Relationship";

    /**
     * Defines the minimum supported WS-Eventing dialect URI.
     */
    public static final String WS_EVENTING_SUPPORTED_DIALECT = NAMESPACE + "/Action";

    /**
     * Defines the DPWS relationship type for hosts (devices).
     */
    public static final String RELATIONSHIP_TYPE_HOST = NAMESPACE + "/host";

    /**
     * Defines the DPWS device type that is required to identify a DPWS compliant device during discovery.
     */
    public static final QName DEVICE_TYPE = new QName(NAMESPACE, "Device");
}
