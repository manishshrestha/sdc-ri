package org.somda.sdc.dpws.http.jetty;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import jakarta.servlet.http.HttpServletRequest;
import java.util.stream.Stream;

/**
 * Jetty utilities.
 */
public class JettyUtil {
    /**
     * Returns all available headers from an incoming request.
     *
     * @param request to extract headers from.
     * @return extracted headers as a multimap, without duplicates.
     */
    static ListMultimap<String, String> getRequestHeaders(HttpServletRequest request) {
        ListMultimap<String, String> requestHeaderMap = ArrayListMultimap.create();
        var nameIter = request.getHeaderNames().asIterator();
        Stream.generate(() -> null) // what
                .takeWhile(x -> nameIter.hasNext())
                .map(n -> nameIter.next().toLowerCase())
                // filter duplicates which occur because of capitalization
                .distinct()
                .forEach(
                        headerName -> {
                            var headers = request.getHeaders(headerName);
                            headers.asIterator().forEachRemaining(header ->
                                    requestHeaderMap.put(headerName, header)
                            );
                        }
                );
        return requestHeaderMap;
    }
}
