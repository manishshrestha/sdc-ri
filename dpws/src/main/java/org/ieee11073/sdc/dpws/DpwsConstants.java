package org.ieee11073.sdc.dpws;

import javax.xml.namespace.QName;
import java.time.Duration;

/**
 * DPWS constants.
 *
 * @see <a href="http://docs.oasis-open.org/ws-dd/dpws/1.1/os/wsdd-dpws-1.1-spec-os.html#_Toc228672112">Appendix B. Constants</a>
 */
public class DpwsConstants {
    public static final String JAXB_CONTEXT_PACKAGE = "org.ieee11073.sdc.dpws.model";

    public static final String NAMESPACE = "http://docs.oasis-open.org/ws-dd/ns/dpws/2009/01";
    public static final Duration APP_MAX_DELAY = Duration.ofMillis(2_500L);
    public static final int DISCOVERY_PORT = 3_702;
    public static final Duration MATCH_TIMEOUT = Duration.ofSeconds(10L);

    /**
     * Unit: octets.
     */
    public static final int MAX_ENVELOPE_SIZE = 32_767;

    /**
     * Unit: octets
     */
    public static final int MAX_UDP_ENVELOPE_SIZE = 4_096;

    /**
     * Unit: Unicode characters
     */
    public static final int MAX_FIELD_SIZE = 256;

    /**
     * Unit: octets
     */
    public static final int MAX_URI_SIZE = 2_048;

    public static final int MULTICAST_UDP_REPEAT = 1;

    public static final Duration UDP_MAX_DELAY = Duration.ofMillis(250L);

    public static final Duration UDP_MIN_DELAY = Duration.ofMillis(50L);

    public static final Duration UDP_UPPER_DELAY = Duration.ofMillis(450L);

    public static final int UNICAST_UDP_REPEAT = 1;

    public static final String MEX_DIALECT_THIS_MODEL = NAMESPACE + "/ThisModel";

    public static final String MEX_DIALECT_THIS_DEVICE = NAMESPACE + "/ThisDevice";

    public static final String MEX_DIALECT_RELATIONSHIP = NAMESPACE + "/Relationship";

    public static final String WS_EVENTING_SUPPORTED_DIALECT = NAMESPACE + "/Action";

    public static final String RELATIONSHIP_TYPE_HOST = NAMESPACE + "/host";

    public static final QName DEVICE_TYPE = new QName(NAMESPACE, "Device");
}
