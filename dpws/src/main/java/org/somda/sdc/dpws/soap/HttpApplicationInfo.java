package org.somda.sdc.dpws.soap;

import java.util.HashMap;
import java.util.Map;

public class HttpApplicationInfo extends ApplicationInfo {

    private final Map<String, String> httpHeaders;

    public HttpApplicationInfo(Map<String, String> httpHeaders) {
        this.httpHeaders = httpHeaders;
    }

    public Map<String, String> getHttpHeaders() {
        return new HashMap<>(httpHeaders);
    }
}
