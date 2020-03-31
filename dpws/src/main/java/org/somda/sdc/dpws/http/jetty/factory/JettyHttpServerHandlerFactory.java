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
     * @param expectTLS whether the connection is expected to use TLS (deprecated)
     * @param mediaType media type of transmitted content
     * @param handler   to handle incoming requests
     * @return a new {@linkplain JettyHttpServerHandler}
     * @deprecated use {@link #create(String, HttpHandler)} instead
     */
    @Deprecated(since = "1.1.0", forRemoval = true)
    JettyHttpServerHandler create(Boolean expectTLS, String mediaType, HttpHandler handler);

    /**
     * Instantiates {@linkplain JettyHttpServerHandler} with the given objects and injected objects.
     *
     * @param mediaType media type of transmitted content
     * @param handler   to handle incoming requests
     * @return a new {@linkplain JettyHttpServerHandler}
     */
    JettyHttpServerHandler create(String mediaType, HttpHandler handler);

}
