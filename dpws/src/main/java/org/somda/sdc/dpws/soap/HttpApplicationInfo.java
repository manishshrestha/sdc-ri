package org.somda.sdc.dpws.soap;

import java.util.Map;
import java.util.TreeMap;

public class HttpApplicationInfo extends ApplicationInfo {

    private final Map<String, String> httpHeaders;

    public HttpApplicationInfo(Map<String, String> httpHeaders) {
        this.httpHeaders = httpHeaders;
    }

    public Map<String, String> getHttpHeaders() {
        // http header names are case-insensitive according to rfc7230 so ensure we use a map which represents this
        // see https://tools.ietf.org/html/rfc7230#section-3.2
        Map<String, String> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        map.putAll(httpHeaders);
        return map;
    }
}
