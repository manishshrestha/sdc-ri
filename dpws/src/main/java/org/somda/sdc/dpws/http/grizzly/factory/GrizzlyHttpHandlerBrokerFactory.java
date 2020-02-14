package org.somda.sdc.dpws.http.grizzly.factory;

import org.somda.sdc.dpws.http.HttpHandler;
import org.somda.sdc.dpws.http.grizzly.GrizzlyHttpHandlerBroker;

import com.google.inject.assistedinject.Assisted;

/*
 * Creates {@linkplain GrizzlyHttpHandlerBroker} instances.
 */
public interface GrizzlyHttpHandlerBrokerFactory {
    
    /*
     * Instantiates {@linkplain GrizzlyHttpHandlerBroker} with the given objects and injected objects.
     */
    GrizzlyHttpHandlerBroker create(@Assisted("mediaType") String mediaType, HttpHandler handler, @Assisted("requestedUri") String requestedUri);
}
