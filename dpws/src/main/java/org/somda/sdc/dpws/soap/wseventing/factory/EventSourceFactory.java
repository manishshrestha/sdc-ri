package org.somda.sdc.dpws.soap.wseventing.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.dpws.soap.wseventing.EventSource;
import org.somda.sdc.dpws.soap.wseventing.EventSourceFilterPlugin;

import java.util.Map;

/**
 * Creates a {@link EventSource} instances.
 */
public interface EventSourceFactory {
    /**
     * Creates a new WS-Eventing event source.
     *
     * @param eventSourceFilterPlugins a map of event source filters where map key is a supported filter dialect URI
     *                                 and value is a custom (not action-based) implementation of the filter dialect.
     * @return a new {@link EventSource} instance.
     */
    EventSource createEventSource(@Assisted Map<String, EventSourceFilterPlugin> eventSourceFilterPlugins);
}
