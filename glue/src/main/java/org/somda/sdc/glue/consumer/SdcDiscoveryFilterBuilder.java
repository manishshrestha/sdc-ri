package org.somda.sdc.glue.consumer;

import org.somda.sdc.biceps.model.participant.EnsembleContextState;
import org.somda.sdc.biceps.model.participant.LocationContextState;
import org.somda.sdc.dpws.client.DiscoveryFilter;
import org.somda.sdc.dpws.client.DiscoveryFilterBuilder;

import javax.xml.namespace.QName;

/**
 * A variant of the {@linkplain DiscoveryFilterBuilder} that adds scopes and types required by SDC.
 * <p>
 * The following type is assigned: todo put type here
 * The following scope is assigned: todo put scope here
 */
public class SdcDiscoveryFilterBuilder {
    private final DiscoveryFilterBuilder discoveryFilterBuilder;

    /**
     * Constructs a new object with empty types and scopes.
     */
    public SdcDiscoveryFilterBuilder() {
        this.discoveryFilterBuilder = new DiscoveryFilterBuilder();
        this.discoveryFilterBuilder.addScope("http://scope"); // todo add actual SDC Glue scope here
        this.discoveryFilterBuilder.addType(new QName("http://type", "Type")); // todo add actual MDPWS type here
    }

    /**
     * Adds a type.
     *
     * @param type the type as QName according to WS-Discovery.
     * @return this object.
     */
    public SdcDiscoveryFilterBuilder addType(QName type) {
        discoveryFilterBuilder.addType(type);
        return this;
    }

    /**
     * Adds a scope.
     *
     * @param scope the scope URI as string.
     * @return this object.
     */
    public SdcDiscoveryFilterBuilder addScope(String scope) {
        discoveryFilterBuilder.addScope(scope);
        return this;
    }

    /**
     * Adds a primary location context state instance identifier as scope.
     *
     * @param state the location context state.
     * @return this object.
     */
    public SdcDiscoveryFilterBuilder addContext(LocationContextState state) {
        // todo implement
        return this;
    }

    /**
     * Adds a primary ensemble context state instance identifier as scope.
     *
     * @param state the ensemble context state.
     * @return this object.
     */
    public SdcDiscoveryFilterBuilder addContext(EnsembleContextState state) {
        // todo implement
        return this;
    }

    /**
     * Gets a discovery filter with all types and scopes added via {@link #addType(QName)} and
     * {@link #addScope(String)}.
     *
     * @return a {@linkplain DiscoveryFilter} instance.
     */
    public DiscoveryFilter get() {
        return discoveryFilterBuilder.get();
    }
}
