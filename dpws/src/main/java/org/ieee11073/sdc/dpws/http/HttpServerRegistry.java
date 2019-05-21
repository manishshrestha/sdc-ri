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
     * Register handlers on context paths for dedicated hosts.
     *
     * @param host        Local address on where to expose an HTTP server. The server is created and started on demand.
     * @param port        The port where to bind the context (if none given, random port is chosen).
     * @param contextPath A context path where the given host shall listen to.
     * @param handler     Whenever a request to the given context path is done,
     *                    {@link HttpHandler#process(InputStream, OutputStream, TransportInfo)} is called. Default
     *                    response media type is {@link SoapConstants#MEDIA_TYPE_SOAP}.
     * @return Full path of the HTTP server address including host name, port and context path.
     */
    URI registerContext(String host, Integer port, String contextPath, HttpHandler handler);

    /**
     * @param host        Local address on where to expose an HTTP server. The server is created and started on demand.
     * @param port        The port where to bind the context (if none given, random port is chosen).
     * @param contextPath A context path where the given hosts shall listen on.
     * @param mediaType   Media type of the response the handler will produce.
     * @param handler     Whenever a request to the given context path is done,
     *                    {@link HttpHandler#process(InputStream, OutputStream, TransportInfo)}  is called.
     * @return Full path of the HTTP server address including host name, port and context path.
     */
    URI registerContext(String host, Integer port, String contextPath, String mediaType, HttpHandler handler);

    /**
     * Remove context path listener.
     *
     * After that call no {@link HttpHandler#process(InputStream, OutputStream, TransportInfo)} will be called for
     * the given context path.
     *
     * @param host        The host where the context path shall be removed.
     * @param port        The port where the context path shall be removed.
     * @param contextPath The context path to remove.
     */
    void unregisterContext(String host, Integer port, String contextPath);
}
