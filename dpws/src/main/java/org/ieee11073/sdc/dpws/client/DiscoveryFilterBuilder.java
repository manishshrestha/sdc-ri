package org.ieee11073.sdc.dpws.client;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link DiscoveryFilter} convenience builder with method chaining.
 */
public class DiscoveryFilterBuilder {
    private final List<QName> types;
    private final List<String> scopes;

    /**
     * Constructs a new object with empty types and scopes.
     */
    public DiscoveryFilterBuilder() {
        this.types = new ArrayList<>();
        this.scopes = new ArrayList<>();
    }

    /**
     * Adds a type.
     *
     * @param type the type as QName according to WS-Discovery.
     * @return this object.
     */
    public DiscoveryFilterBuilder addType(QName type) {
        types.add(type);
        return this;
    }

    /**
     * Adds a scope.
     *
     * @param scope the scope URI as string.
     * @return this object.
     */
    public DiscoveryFilterBuilder addScope(String scope) {
        scopes.add(scope);
        return this;
    }

    /**
     * Gets a discovery filter with all types and scopes added via {@link #addType(QName)} and
     * {@link #addScope(String)}.
     *
     * @return a {@linkplain DiscoveryFilter} instance.
     */
    public DiscoveryFilter get() {
        return new DiscoveryFilter(types, scopes);
    }
}
