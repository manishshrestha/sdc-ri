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
     * Construct a new object with empty types and scopes.
     */
    public DiscoveryFilterBuilder() {
        this.types = new ArrayList<>();
        this.scopes = new ArrayList<>();
    }

    /**
     * Add a single type.
     */
    public DiscoveryFilterBuilder addType(QName type) {
        types.add(type);
        return this;
    }

    /**
     * Add a single scope.
     */
    public DiscoveryFilterBuilder addScope(String scope) {
        scopes.add(scope);
        return this;
    }

    /**
     * Get discovery filter with all types and scopes added through {@link #addType(QName)} and
     * {@link #addScope(String)}.
     */
    public DiscoveryFilter get() {
        return new DiscoveryFilter(types, scopes);
    }
}
