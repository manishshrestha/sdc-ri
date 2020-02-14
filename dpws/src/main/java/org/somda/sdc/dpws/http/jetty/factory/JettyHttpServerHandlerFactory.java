package org.somda.sdc.dpws.http.jetty.factory;

import org.somda.sdc.dpws.http.HttpHandler;
import org.somda.sdc.dpws.http.jetty.JettyHttpServerHandler;

/*
 * Creates {@linkplain JettyHttpServerHandlerFactory} instances.
 */
public interface JettyHttpServerHandlerFactory {
    
    /*
     * Instantiates {@linkplain JettyHttpServerHandlerFactory} with the given objects and injected objects.
     */
    JettyHttpServerHandler create(String mediaType, HttpHandler handler);

}
