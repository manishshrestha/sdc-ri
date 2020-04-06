package org.somda.sdc.dpws.soap;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to provide application layer information for http.
 */
public class HttpApplicationInfo extends ApplicationInfo {

    private final ListMultimap<String, String> headers;

    /**
     * Creates an instance using http headers.
     * <p>
     * <em>All keys will be converted to lower case.</em>
     *
     * @param httpHeaders map of available headers.
     */
    @Deprecated(since = "1.1.0", forRemoval = true)
    public HttpApplicationInfo(Map<String, String> httpHeaders) {
        this.headers = ArrayListMultimap.create();
        // convert all entries to lower case
        httpHeaders.forEach((key, value) -> headers.put(key.toLowerCase(), value));
    }

    /**
     * Creates an instance using http headers.
     * <p>
     * <em>All keys will be converted to lower case.</em>
     *
     * @param httpHeaders multimap of available headers.
     */
    public HttpApplicationInfo(ListMultimap<String, String> httpHeaders) {
        this.headers = ArrayListMultimap.create();
        // convert all entries to lower case
        httpHeaders.forEach((key, value) -> headers.put(key.toLowerCase(), value));
    }

    /**
     * Retrieve http headers as map.
     * <p>
     * For each header key with multiple entries, the content will be merged into a comma separated list.
     * All keys are lower case.
     *
     * @return Map of all headers
     */
    @Deprecated(since = "1.1.0", forRemoval = true)
    public Map<String, String> getHttpHeaders() {
        // http header names are case-insensitive according to rfc7230 so ensure we use a map which represents this
        // see https://tools.ietf.org/html/rfc7230#section-3.2
        Map<String, String> map = new HashMap<>();
        headers.asMap().forEach((key, values) -> {
            var valueString = String.join(",", values);
            map.put(key, valueString);
        });
        return map;
    }

    /**
     * Retrieve http headers as multimap.
     * <p>
     * Each value seen for a key will be added to the list of values for said key.
     * All keys are lower case.
     *
     * @return Map of all headers
     */
    public ListMultimap<String, String> getHeaders() {
        return ArrayListMultimap.create(headers);
    }
}
