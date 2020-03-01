package org.somda.sdc.dpws.http;

import com.google.common.util.concurrent.Service;
import org.somda.sdc.dpws.soap.SoapConstants;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Simple HTTP server registry service.
 * <p>
 * Creates HTTP servers and registers handlers for context paths.
 * For proper shutdown of all servers {@link Service#stopAsync()} should be called.
 */
public interface HttpServerRegistry extends Service {
    /**
     * Creates an HTTP server at given URI.
     * <p>
     * If there was no running HTTP server found under the passed scheme and authority, this function starts a new one.
     *
     * @param schemeAndAuthority the scheme and authority where to access the HTTP server. If port number is
     *                           0, then a random open port is selected and will be part of the returned URI.
     * @return the actual assigned URI of the HTTP server.
     */
    String initHttpServer(String schemeAndAuthority);

    /**
     * Registers a handler for SOAP messages for the given scheme, authority and context path.
     * <p>
     * SOAP messages use the HTTP media type {@link SoapConstants#MEDIA_TYPE_SOAP} for request and response messages.
     *
     * @param schemeAndAuthority scheme and authority used to start a new or re-use an existing HTTP server.
     * @param contextPath        the context path where the given registry shall listen to.<br>
     *                           <em>Important note: the context path needs to start with a slash.</em>
     * @param handler            the handler callback that is invoked on a request to the given context path.
     * @return the actual full path of the HTTP server address the given handler listens to.
     * @see #initHttpServer(String)
     */
    String registerContext(String schemeAndAuthority, String contextPath, HttpHandler handler);

    /**
     * Registers a handler for HTTP requests destined to the given scheme, authority and context path.
     *
     * @param schemeAndAuthority scheme and authority used to start a new or re-use an existing HTTP server.
     * @param contextPath        the context path where the given registry shall listen to.<br>
     *                           <em>Important note: the context path needs to start with a slash.</em>
     * @param mediaType          the media type of the response the handler will produce.
     * @param handler            the handler callback that is invoked on a request to the given context path.
     * @return the actual full path of the HTTP server address the given handler listens to.
     * @see #initHttpServer(String)
     */
    String registerContext(String schemeAndAuthority, String contextPath, String mediaType, HttpHandler handler);

    /**
     * Removes a handler for the given scheme, authority and context path.
     * <p>
     * {@link HttpHandler#process(InputStream, OutputStream, org.somda.sdc.dpws.soap.CommunicationContext)} will not be called for
     * any request destined to the corresponding handler.
     * Requests to the corresponding are answered with an HTTP 404.
     *
     * @param schemeAndAuthority scheme and authority where the context shall be removed.
     * @param contextPath        the context path to remove.
     *                           <em>The context path needs to start with a slash.</em>
     */
    void unregisterContext(String schemeAndAuthority, String contextPath);
}
