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
    @Deprecated(since = "1.1.0", forRemoval = true)
    JettyHttpServerHandler create(Boolean expectTLS, String mediaType, HttpHandler handler);

    /*
     * Instantiates {@linkplain JettyHttpServerHandlerFactory} with the given objects and injected objects.
     */
    JettyHttpServerHandler create(String mediaType, HttpHandler handler);

}
