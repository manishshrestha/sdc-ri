package org.somda.sdc.dpws.http.grizzly.factory;

import org.somda.sdc.dpws.http.HttpHandler;
import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.dpws.http.grizzly.GrizzlyHttpHandlerBroker;


public interface GrizzlyHttpHandlerBrokerFactory {
    
    GrizzlyHttpHandlerBroker create(@Assisted("mediaType") String mediaType, HttpHandler handler, @Assisted("requestedUri") String requestedUri);
}
