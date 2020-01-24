package org.somda.sdc.glue.common;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.biceps.model.participant.InstanceIdentifier;
import org.somda.sdc.biceps.model.participant.LocationDetail;
import org.somda.sdc.glue.common.helper.UrlUtf8;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Utility class to map location detail to and from URIs in accordance with SDC Glue section 9.4.1.2.
 */
public class LocationDetailQueryMapper {
    private static final Logger LOG = LoggerFactory.getLogger(LocationDetailQueryMapper.class);

    /**
     * Creates a URI out of a location context instance identifier and location detail.
     *
     * @param instanceIdentifier a location context instance identifier.
     * @param locationDetail     the location detail to append.
     * @return a URI with appended location detail parameters or the URI if something went wrong during URI re-construction.
     */
    public static URI createWithLocationDetailQuery(InstanceIdentifier instanceIdentifier, LocationDetail locationDetail) {
        final URI uri = ContextIdentificationMapper.fromInstanceIdentifier(instanceIdentifier,
                ContextIdentificationMapper.ContextSource.Location);
        StringBuilder queryParams = new StringBuilder("?");
        int count = 0;
        for (LocationDetailFields field : LocationDetailFields.values()) {
            try {
                final Method getter = field.getGetter();
                final var key = field.getQueryKey();
                final var value = (String) getter.invoke(locationDetail);
                if (Strings.isNullOrEmpty(value)) {
                    continue;
                }
                if (count++ > 0) {
                    queryParams.append('&');
                }
                queryParams.append(key).append('=').append(UrlUtf8.encode(value));
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassCastException e) {
                // Ignore reflection exceptions
                LOG.warn("Unexpected reflection exception occurred during location detail appending of field " +
                        field.toString(), e);
            }
        }

        final String queryParamsString = queryParams.toString();
        return URI.create(uri.getScheme() + ":" + uri.getRawSchemeSpecificPart() +
                (queryParamsString.equals("?") ? "" : queryParamsString));
    }

    /**
     * Reads location detail query parameters from the given URI.
     *
     * @param uri the URI to parse.
     * @return a {@link LocationDetail} instance in which every field is filled that has an existing location detail
     * query parameter in <em>uri</em>.
     */
    public static LocationDetail readLocationDetailQuery(URI uri) {
        final var locationDetail = new LocationDetail();
        final var queryItems = splitQuery(uri);
        for (LocationDetailFields field : LocationDetailFields.values()) {
            final var values = queryItems.get(field.getQueryKey());
            if (values != null && !values.isEmpty()) {
                try {
                    final Method setter = field.getSetter();
                    setter.invoke(locationDetail, UrlUtf8.decode(values.get(0)));
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    // Ignore reflection exceptions
                    LOG.warn("Unexpected reflection exception occurred during location detail reading of field " +
                            field.toString(), e);
                }
            }
        }

        return locationDetail;
    }

    private static Map<String, List<String>> splitQuery(URI uri) {
        final Map<String, List<String>> queryPairs = new LinkedHashMap<>();
        final String[] keyValuePaira = uri.getRawQuery().split("&");
        for (String pair : keyValuePaira) {
            final int equalCharIndex = pair.indexOf("=");
            final String key = equalCharIndex > 0 ? UrlUtf8.decode(pair.substring(0, equalCharIndex)) : pair;
            if (!queryPairs.containsKey(key)) {
                queryPairs.put(key, new LinkedList<>());
            }
            if (equalCharIndex > 0 && pair.length() > equalCharIndex + 1) {
                queryPairs.get(key).add(UrlUtf8.decode(pair.substring(equalCharIndex + 1)));
            }
        }
        return queryPairs;
    }

    private enum LocationDetailFields {
        FACILITY("fac", "Facility"),
        BUILDING("bldng", "Building"),
        POINT_OF_CARE("poc", "PoC"),
        FLOOR("flr", "Floor"),
        ROOM("rm", "Room"),
        BED("bed", "Bed");

        private final String queryKey;
        private final String name;

        LocationDetailFields(String queryKey, String name) {

            this.queryKey = queryKey;
            this.name = name;
        }

        String getQueryKey() {
            return queryKey;
        }

        Method getSetter() throws NoSuchMethodException {
            return LocationDetail.class.getDeclaredMethod("set" + name, String.class);
        }

        Method getGetter() throws NoSuchMethodException {
            return LocationDetail.class.getDeclaredMethod("get" + name);
        }
    }
}