package org.somda.sdc.dpws.client;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Filter information to be used by {@link Client#probe(DiscoveryFilter)}.
 */
public class DiscoveryFilter {
    private final List<QName> types;
    private final List<String> scopes;
    private final String discoveryId;

    private static final AtomicInteger discoveryIdCounter = new AtomicInteger(0);

    /**
     * Creates a new discovery filter with a discovery id that is unique across one application instance.
     *
     * @param types  the types to match.
     * @param scopes the scopes to match.
     * @see <a href="http://docs.oasis-open.org/ws-dd/discovery/1.1/os/wsdd-discovery-1.1-spec-os.html#_Toc234231831">WS-Discovery Probe</a>
     */
    public DiscoveryFilter(List<QName> types, List<String> scopes) {
        this.types = types;
        this.scopes = scopes;
        this.discoveryId = Integer.toString(discoveryIdCounter.incrementAndGet());
    }

    public List<QName> getTypes() {
        return types;
    }

    public List<String> getScopes() {
        return scopes;
    }

    /**
     * Gets the discovery id.
     * <p>
     * The discovery id is used to distinguish between probe requests.
     *
     * @return the discover id of this filter.
     */
    public String getDiscoveryId() {
        return discoveryId;
    }
}
