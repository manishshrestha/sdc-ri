package org.somda.sdc.dpws.http.apache.helper;

import org.apache.http.Header;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ApacheClientHelper {

    /**
     * Converts apache headers into a Map.
     *
     * @param allHeaders array of apache {@linkplain Header} elements
     * @return Map of all header entries
     */
    public static Map<String, String> allHeadersToMap(Header[] allHeaders) {
        Map<String, String> mappedHeaders = new HashMap<>();
        Arrays.stream(allHeaders).forEach(header -> mappedHeaders.put(
                header.getName(),
                header.getValue()
        ));
        return mappedHeaders;
    }
}
