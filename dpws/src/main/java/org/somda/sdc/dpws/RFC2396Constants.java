package org.somda.sdc.dpws;

import java.util.Collections;

public class RFC2396Constants {
    private static final String HEX = "[a-fA-F0-9]";
    private static final String ESCAPED = "(%" + HEX + HEX + ")";

    private static final String UNRESERVED = "([a-zA-Z0-9\\-_.!~*'()])";

    private static final String RESERVED = "[;/?:@&=+$,]";
    private static final String URIC = "(" + RESERVED + "|" + UNRESERVED + "|" + ESCAPED + ")";

    private static final String FRAGMENT = "(" + URIC + "*)";

    private static final String QUERY = "(" + URIC + "*)";

    private static final String P_CHAR = "(" + UNRESERVED + "|" + ESCAPED + "|[:@&=+$,])";
    private static final String PARAM = "({=param}" + P_CHAR + "*)";
    private static final String SEGMENT = "({=segment}" + P_CHAR + "*(;" + PARAM + ")*)";
    private static final String PATH_SEGMENTS = "({=pathSegment}({=pathSegment1}" + SEGMENT + ")({=pathSegment2}(/" + SEGMENT + ")*))";

    private static final String PORT = "({=port}[0-9]*)";
    private static final String DEC_OCTET = "(1[0-9][0-9])|(2[0-4][0-9])|(25[0-5]|[0-9])|([1-9][0-9])";
    private static final String IPV4_ADDRESS = "({=ipv4}" + String.join(".", Collections.nCopies(4, DEC_OCTET)) + ")";
    private static final String TOPLABEL = "([a-zA-Z]|([a-zA-Z]([a-zA-Z0-9]|\\-)*[a-zA-Z0-9]))";
    private static final String DOMAINLABEL = "([a-zA-Z0-9]|([a-zA-Z0-9]([a-zA-Z0-9]|\\-)*[a-zA-Z0-9]))";
    private static final String HOSTNAME = "((" + DOMAINLABEL + "\\.)*" + TOPLABEL + "(\\.)?)";
    private static final String HOST = "({=host}" + HOSTNAME + "|" + IPV4_ADDRESS + ")";
    private static final String HOSTPORT = "({=hostPort}" + HOST + "(:" + PORT +")?)";

    public static final String USER_INFO = "({=userInfo}(" + UNRESERVED + "|" + ESCAPED + "|[;:&=+$,])*)";
    public static final String SERVER = "({=server}((" + USER_INFO + "@)?" + HOSTPORT +")?" + ")";

    private static final String REG_NAME = "({=regName}(" + UNRESERVED + "|" + ESCAPED + "|[$,;:@&=+])+)";

    public static final String AUTHORITY = "({=authority}" + SERVER + "|" + REG_NAME + ")";

    public static final String SCHEME_SEGMENT = "(?i:[a-z][a-z0-9+-.]*)";

    private static final String REL_SEGMENT = "(" + UNRESERVED + "|" + ESCAPED + "|[;@&=+$,])+";

    public static final String ABS_PATH = "({=absPath}/" + PATH_SEGMENTS + ")";
    public static final String NET_PATH = "({=netPath}//(" + AUTHORITY + ")(" + ABS_PATH + ")?)";
    public static final String REL_PATH = "({relPath}" + REL_SEGMENT + "(" + ABS_PATH + ")?)";

    private static final String URIC_NO_SLASH = "(" + UNRESERVED + "|" + ESCAPED + "|" + "[;?:@&=+$,])";

    private static final String OPAQUE_PART = "({opaquePart}(" + URIC_NO_SLASH + "(" + URIC + ")*))";
    public static final String HIER_PART = "({hierPart}(" + NET_PATH + "|" + ABS_PATH + ")(\\?({=absoluteUriQuery}" + QUERY + "))?)";

    public static final String RELATIVE_URI = "({relativeUri}(" + NET_PATH + "|" + ABS_PATH + "|" + REL_PATH + ")(\\?({=relativeUriQuery}" + QUERY + "))?)";

    public static final String ABSOLUTE_URI = "({absoluteUri}({scheme}" + SCHEME_SEGMENT + ")" +
            ":" + "(" + HIER_PART + "|" + OPAQUE_PART + "))";

    public static final String URI_REFERENCE = "(" + ABSOLUTE_URI + "|" + RELATIVE_URI + ")?(#({fragment}" + FRAGMENT + "))?";
}
