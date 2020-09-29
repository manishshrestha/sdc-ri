package org.somda.sdc.dpws;

public class RFC2396Constants {
    private static final String HEX = "[a-fA-F0-9]";
    private static final String ESCAPED = "(%" + HEX + HEX + ")";

    private static final String UNRESERVED = "([a-zA-Z0-9\\-_.!~*'()])";

    private static final String RESERVED = "[;/?:@&=+$,]";
    private static final String URIC = "(" + RESERVED + "|" + UNRESERVED + "|" + ESCAPED + ")";

    private static final String FRAGMENT = "(" + URIC + "*)";

    private static final String QUERY = "(" + URIC + "*)";

    private static final String P_CHAR = "(" + UNRESERVED + "|" + ESCAPED + "|[:@&=+$,])";
    private static final String PARAM = "(" + P_CHAR + "*)";
    private static final String SEGMENT = "(" + P_CHAR + "*(;" + PARAM + ")*)";
    private static final String PATH_SEGMENTS = "((" + SEGMENT + ")((/" + SEGMENT + ")*))";

    private static final String PORT = "([0-9]*)";

    private static final String IPV4_ADDRESS = "([0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+)";
    private static final String TOPLABEL = "([a-zA-Z]|([a-zA-Z]([a-zA-Z0-9]|\\-)*[a-zA-Z0-9]))";
    private static final String DOMAINLABEL = "([a-zA-Z0-9]|([a-zA-Z0-9]([a-zA-Z0-9]|\\-)*[a-zA-Z0-9]))";
    private static final String HOSTNAME = "((" + DOMAINLABEL + "\\.)*" + TOPLABEL + "(\\.)?)";
    private static final String HOST = "(" + HOSTNAME + "|" + IPV4_ADDRESS + ")";
    private static final String HOSTPORT = "(" + HOST + "(:" + PORT +")?)";

    public static final String USER_INFO = "((" + UNRESERVED + "|" + ESCAPED + "|[;:&=+$,])*)";
    public static final String SERVER = "(((" + USER_INFO + "@)?" + HOSTPORT +")?" + ")";

    private static final String REG_NAME = "((" + UNRESERVED + "|" + ESCAPED + "|[$,;:@&=+])+)";

    public static final String AUTHORITY = "^(" + SERVER + "|" + REG_NAME + ")$";

    public static final String SCHEME_SEGMENT = "(?i:[a-z][a-z0-9+-.]*)";

    private static final String REL_SEGMENT = "(" + UNRESERVED + "|" + ESCAPED + "|[;@&=+$,])+";

    public static final String ABS_PATH = "(/" + PATH_SEGMENTS + ")";
    public static final String NET_PATH = "(//(" + AUTHORITY + ")(" + ABS_PATH + ")?)";
    public static final String REL_PATH = "(" + REL_SEGMENT + "(" + ABS_PATH + ")?)";

    private static final String URIC_NO_SLASH = "(" + UNRESERVED + "|" + ESCAPED + "|" + "[;?:@&=+$,])";

    private static final String OPAQUE_PART = "((" + URIC_NO_SLASH + "(" + URIC + ")*))";
    public static final String HIER_PART = "((" + NET_PATH + "|" + ABS_PATH + ")(\\?({absoluteUriQuery}" + QUERY + "))?)";

    public static final String RELATIVE_URI = "({relativeUri}(" + NET_PATH + "|" + ABS_PATH + "|" + REL_PATH + ")(\\?({relativeUriQuery}" + QUERY + "))?)";

    public static final String ABSOLUTE_URI = "({absoluteUri}({scheme}" + SCHEME_SEGMENT + ")" +
            ":" + "(" + HIER_PART + "|" + OPAQUE_PART + "))";

    public static final String URI_REFERENCE = "(" + ABSOLUTE_URI + "|" + RELATIVE_URI + ")?(#({fragment}" + FRAGMENT + "))?";
}
