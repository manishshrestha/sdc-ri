package org.somda.sdc.dpws.soap.wseventing.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.dpws.soap.wseventing.EventSourceFilterPlugin;
import org.somda.sdc.dpws.soap.wseventing.EventSourceInterceptor;

import java.util.Map;

/**
 * Creates a {@link EventSourceInterceptor} instances.
 */
public interface EventSourceInterceptorFactory {
    /**
     * Creates a new WS-Eventing source interceptor.
     *
     * @param eventSourceFilterPlugins a map of event source filters where map key is a supported filter dialect and
     *                                 value is custom (not action based) implementation of subscription filter.
     * @return a new {@link EventSourceInterceptor} instance.
     */
    EventSourceInterceptor createWsEventingEventSink(@Assisted Map<String, EventSourceFilterPlugin> eventSourceFilterPlugins);
}
