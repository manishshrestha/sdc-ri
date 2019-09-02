package org.ieee11073.sdc.dpws.http;

import com.google.common.util.concurrent.Service;
import org.ieee11073.sdc.dpws.soap.TransportInfo;
import org.ieee11073.sdc.dpws.soap.SoapConstants;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

/**
 * Simple HTTP server registry service.
 *
 * Create HTTP server on demand given a dedicated configuration and registerOrUpdate handlers for context paths.
 *
 * For proper shutdown of all servers, {@link Service#stopAsync()} should be called.
 */
public interface HttpServerRegistry extends Service {

    /**
     * Create HTTP server at given URI if not existing, start and return.
     * @param schemeAndAuthority The scheme and authority where to access the HTTP server. If port number is
     *                           0, a random open port is selected and part of the returned URI.
     * @return The actual assigned URI of the HTTP server.
     */
    URI initHttpServer(URI schemeAndAuthority);

    /**
     * Register handlers for SOAP messsages on context paths for dedicated hosts.
     *
     * @param schemeAndAuthority Base address where to deploy the context path.
     * @param contextPath A context path where the given host shall listen to.
     *                    The context path needs to start with a slash.
     * @param handler     Whenever a request to the given context path is done,
     *                    {@link HttpHandler#process(InputStream, OutputStream, TransportInfo)} is called. Default
     *                    response media type is {@link SoapConstants#MEDIA_TYPE_SOAP}.
     * @return Full path of the HTTP server address including host name, port and context path.
     */
    URI registerContext(URI schemeAndAuthority, String contextPath, HttpHandler handler);

    /**
     * @param schemeAndAuthority Base address where to deploy the context path.
     * @param contextPath A context path where the given hosts shall listen on.
     *                    The context path needs to start with a slash.
     * @param mediaType   Media type of the response the handler will produce.
     * @param handler     Whenever a request to the given context path is done,
     *                    {@link HttpHandler#process(InputStream, OutputStream, TransportInfo)}  is called.
     * @return Full path of the HTTP server address including host name, port and context path.
     */
    URI registerContext(URI schemeAndAuthority, String contextPath, String mediaType, HttpHandler handler);

    /**
     * Remove context path listener.
     *
     * After that call no {@link HttpHandler#process(InputStream, OutputStream, TransportInfo)} will be called for
     * the given context path.
     *
     * @param schemeAndAuthority Scheme and authority where the context shall be removed.
     * @param contextPath The context path to remove.
     */
    void unregisterContext(URI schemeAndAuthority, String contextPath);
}
