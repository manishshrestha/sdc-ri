package org.somda.sdc.dpws;

import javax.xml.namespace.QName;
import java.time.Duration;
import java.util.Collections;

/**
 * DPWS constants.
 *
 * @see <a href="http://docs.oasis-open.org/ws-dd/dpws/1.1/os/wsdd-dpws-1.1-spec-os.html#_Toc228672112"
 * >Appendix B. Constants</a>
 */
public class DpwsConstants {
    /**
     * Defines the context package for JAXB.
     */
    public static final String JAXB_CONTEXT_PACKAGE = "org.somda.sdc.dpws.model";

    /**
     * Resource path to DPWS XML Schema.
     */
    public static final String SCHEMA_PATH = "wsdd-dpws-1.1-schema.xsd";

    /**
     * Defines the DPWS namespace.
     */
    public static final String NAMESPACE = "http://docs.oasis-open.org/ws-dd/ns/dpws/2009/01";

    /**
     * Defines the preferred prefix for the DPWS namespace.
     */
    public static final String NAMESPACE_PREFIX = "dpws";

    /**
     * Defines the multicast port used to transmit discovery messages.
     */
    public static final int DISCOVERY_PORT = 3_702;

    /**
     * Defines the maximum size for envelopes transmitted over TCP.
     * <p>
     * The maximum envelope size is currently not verified, i.e., SOAP messages of any size are neither
     * detected nor rejected.
     * CAVEAT: lower layer protocol implementations may have some restrictions that are out of scope for this constant.
     * <p>
     * Unit: octets.
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
     * Defines the maximum size for URIs.
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
     *
     * @see #MULTICAST_UDP_REPEAT
     */
    public static final int UNICAST_UDP_REPEAT = MULTICAST_UDP_REPEAT;

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

    /**
     * URI scheme for SOAP-over-UDP.
     */
    public static final String URI_SCHEME_SOAP_OVER_UDP = "soap.udp";

    /**
     * SegmentNz and Segment regex definitions.
     */
    public static final String SCHEME_SEGMENT = "(?i:[a-z][a-z0-9+-.]*)";
    private static final String ALLOWED_CHARS = "[a-zA-Z0-9\\-._~!$&'()*+,;=:@]";
    private static final String P_CHAR = "(?:(?:%[a-fA-F0-9]{2})+|(?:" + ALLOWED_CHARS + ")+)";
    public static final String SEGMENT_NZ_REGEX = P_CHAR + "+";
    public static final String SEGMENT_REGEX = P_CHAR + "*";
    private static final String DEC_OCTET = "(1[0-9][0-9])|(2[0-4][0-9])|(25[0-5]|[0-9])|([1-9][0-9])";
    private static final String IPV4_ADDRESS = String.join(".", Collections.nCopies(4, DEC_OCTET));
    private static final String REG_NAME = "(?:(?:%[a-fA-F0-9]{2})+|(?:" + "[a-zA-Z0-9\\-._~!$&'()*+,;=]" + ")+)*";
    private static final String USER_INFO = "(?:(?:%[a-fA-F0-9]{2})+|(?:" + "[a-zA-Z0-9\\-._~!$&'()*+,;=:]" + ")+)*";
    private static final String HEXDIG = "[a-fA-F0-9]";
    private static final String IPV_FUTURE = "v" + HEXDIG + "." + "[a-zA-Z0-9\\-._~!$&'()*+,;=:]";
    // The following might require an (?a) flag in other implementations than java.util.regex.Pattern
    private static final String IPV6_ADDRESS = "\\s*((([0-9A-Fa-f]{1,4}:){7}([0-9A-Fa-f]{1,4}|:))|" +
            "(([0-9A-Fa-f]{1,4}:){6}(:[0-9A-Fa-f]{1,4}|" +
            "((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3})|:))|" +
            "(([0-9A-Fa-f]{1,4}:){5}(((:[0-9A-Fa-f]{1,4}){1,2})|" +
            ":((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3})|:))|" +
            "(([0-9A-Fa-f]{1,4}:){4}(((:[0-9A-Fa-f]{1,4}){1,3})|" +
            "((:[0-9A-Fa-f]{1,4})?:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)" +
            "(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|" +
            ":))|(([0-9A-Fa-f]{1,4}:){3}(((:[0-9A-Fa-f]{1,4}){1,4})|" +
            "((:[0-9A-Fa-f]{1,4}){0,2}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)" +
            "(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|" +
            "(([0-9A-Fa-f]{1,4}:){2}(((:[0-9A-Fa-f]{1,4}){1,5})|" +
            "((:[0-9A-Fa-f]{1,4}){0,3}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)" +
            "(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|" +
            "(([0-9A-Fa-f]{1,4}:){1}(((:[0-9A-Fa-f]{1,4}){1,6})|" +
            "((:[0-9A-Fa-f]{1,4}){0,4}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)" +
            "(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|" +
            "(:(((:[0-9A-Fa-f]{1,4}){1,7})|" +
            "((:[0-9A-Fa-f]{1,4}){0,5}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)" +
            "(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:)))" +
            "(%[a-fA-F0-9]{2})?\\s*";
    private static final String IP_LITERAL = "\\[" + "((" + IPV6_ADDRESS + ")|(" + IPV_FUTURE + "))" + "\\]";
    private static final String HOST = "({=host}(" + REG_NAME + "|" + IPV4_ADDRESS + "|" + IP_LITERAL + "))";
    public static final String AUTHORITY = "(({=userInfo}" + USER_INFO + ")@)?" + HOST + "(:({=port}[0-9]*))?";
    private static final String PATH_EMPTY = "";
    private static final String PATH_ROOTLESS = SEGMENT_NZ_REGEX + "(/" + SEGMENT_REGEX + ")*";
    private static final String PATH_NOSCHEME = "[a-zA-Z0-9\\-._~!$&'()*+,;=@]+" + "(/" + SEGMENT_REGEX + ")*";
    private static final String PATH_ABSOLUTE = "/(" + SEGMENT_NZ_REGEX + "(/" + SEGMENT_REGEX + ")*" + ")*";
    private static final String PATH_ABEMPTY = "(/" + SEGMENT_REGEX + ")*";
    private static final String PATH = "(" +
            PATH_ABEMPTY + "|" +
            PATH_ABSOLUTE + "|" +
            PATH_NOSCHEME + "|" +
            PATH_ROOTLESS + "|" +
            PATH_EMPTY + "|" +
            ")";
    private static final String QUERY = "(" + P_CHAR + "|/|\\?)*";
    private static final String FRAGMENT = QUERY;

    private static final String PARAM = P_CHAR + "*";
    private static final String SEGMENT = P_CHAR + "*(;" + PARAM + ")*";
    private static final String PATH_SEGMENTS = SEGMENT + "(/" + SEGMENT + ")*";

    private static final String ESCAPED = "(%" + HEXDIG + HEXDIG + ")";
    private static final String RESERVED = "[;/?:@&=+$,]";
    private static final String UNRESERVED = "[a-zA-Z0-9\\-_.!~*'()]";
    private static final String REL_SEGMENT = "(" + UNRESERVED + "|" + ESCAPED + "|[;@&=+$,])+";

    private static final String ABS_PATH = "({=path}/" + PATH_SEGMENTS + ")";
    private static final String NET_PATH = "({=netPath}//({=authority}" + AUTHORITY + ")(" + ABS_PATH + ")?)";
    private static final String REL_PATH = "({relPath}" + REL_SEGMENT + "(" + ABS_PATH + ")?)";

    public static final String RELATIVE_URI = "({relativeUri}(" + NET_PATH + "|" + ABS_PATH + "|" + REL_PATH + ")(\\?({=relativeUriQuery}" + QUERY + "))?)";

    private static final String URIC = "(" + RESERVED + "|" + UNRESERVED + "|" + ESCAPED + ")";
    private static final String URIC_NO_SLASH = "(" + UNRESERVED + "|" + ESCAPED + "|" + "[;?:@&=+$,])";
    private static final String OPAQUE_PART = "({opaquePart}(" + URIC_NO_SLASH + URIC + ")*)";
    private static final String HIER_PART = "({hierPart}(" + NET_PATH + "|" + ABS_PATH + ")(\\?({=absoluteUriQuery}" + QUERY + "))?)";

    public static final String ABSOLUTE_URI = "({absoluteUri}({scheme}" + SCHEME_SEGMENT + ")" +
            ":" + "(" + HIER_PART + "|" + OPAQUE_PART + "))";

    public static final String URI_REFERENCE = "(" + ABSOLUTE_URI + "|" + RELATIVE_URI + ")?(#({fragment}" + FRAGMENT + "))?";

    // Added negative lookahead for "//" to prevent authority from being interpreted as path
    public static final String URI_REGEX = "^(" +
            "({scheme}" + SCHEME_SEGMENT + ")" +
            ":" +
            "(//({authority}" + AUTHORITY + "))?" +
            "({path}" +
            "(?(authority)" +
            "(((?=/)" + PATH + ")|)|((?!//)" + PATH + ")" +
            ")" +
            ")" +
            "(\\?({query}" + QUERY + "))?" +
            "(#({fragment}" + FRAGMENT + "))?" +
            ")$";

    // The "&" character had to be excluded from the "pchar" definition as it already is used a delimiter.
    // This is a bug in the GLUE standard. The "&" should not be allowed, except as a delimiter.
    private static final String QUERY_ITEM_SEGMENT = "(?:(?:%[a-fA-F0-9]{2})+|(?:[a-zA-Z0-9\\-._~!$'()*+,;=:@])+)";
    private static final String QUERY_ITEM = "((fac=" + QUERY_ITEM_SEGMENT + ")|" +
            "(bldng=" + QUERY_ITEM_SEGMENT + ")|" +
            "(poc=" + QUERY_ITEM_SEGMENT + ")|" +
            "(flr=" + QUERY_ITEM_SEGMENT + ")|" +
            "(rm=" + QUERY_ITEM_SEGMENT + ")|" +
            "(bed=" + QUERY_ITEM_SEGMENT + "))";
    public static final String LOC_CTXT_QUERY = "^(" + QUERY_ITEM + "(&" + QUERY_ITEM + ")*)?$";
}
