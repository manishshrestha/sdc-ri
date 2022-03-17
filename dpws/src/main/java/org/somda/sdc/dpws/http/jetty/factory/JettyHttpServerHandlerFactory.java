package org.somda.sdc.dpws.http.jetty.factory;

import org.somda.sdc.dpws.http.HttpHandler;
import org.somda.sdc.dpws.http.jetty.JettyHttpServerHandler;

/**
 * Creates {@linkplain JettyHttpServerHandler} instances.
 */
public interface JettyHttpServerHandlerFactory {

    /**
     * Instantiates {@linkplain JettyHttpServerHandler} with the given objects and injected objects.
     *
     * @param mediaType media type of transmitted content
     * @param handler   to handle incoming requests
     * @return a new {@linkplain JettyHttpServerHandler}
     */
    JettyHttpServerHandler create(String mediaType, HttpHandler handler);

}
