package org.ieee11073.sdc.dpws.http.grizzly;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import org.ieee11073.sdc.dpws.http.HttpHandler;
import org.ieee11073.sdc.dpws.http.HttpServerRegistry;
import org.ieee11073.sdc.dpws.http.HttpUriBuilder;
import org.ieee11073.sdc.dpws.soap.SoapConstants;
import org.ieee11073.sdc.dpws.soap.TransportInfo;
import org.ieee11073.sdc.dpws.soap.exception.MarshallingException;
import org.ieee11073.sdc.dpws.soap.exception.TransportException;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * {@linkplain HttpServerRegistry} implementation with Grizzly HTTP server.
 */
public class GrizzlyHttpServerRegistry extends AbstractIdleService implements HttpServerRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(GrizzlyHttpServerRegistry.class);

    private final Map<String, URI> addressRegistry;
    private final Map<String, HttpServer> serverRegistry;
    private final Map<String, HandlerWrapper> handlerRegistry;
    private final Lock registryLock;
    private final HttpUriBuilder uriBuilder;

    @Inject
    GrizzlyHttpServerRegistry(HttpUriBuilder uriBuilder) {
        this.uriBuilder = uriBuilder;
        addressRegistry = new HashMap<>();
        serverRegistry = new HashMap<>();
        handlerRegistry = new HashMap<>();
        registryLock = new ReentrantLock();
    }

    @Override
    protected void startUp() throws Exception {
        // nothing to do here - servers will be started on demand
        LOG.info("{} is running.", getClass().getSimpleName());
    }

    @Override
    protected void shutDown() throws Exception {
        LOG.info("Shut down running HTTP servers.");
        registryLock.lock();
        try {
            serverRegistry.forEach((uri, httpServer) -> {
                try {
                    httpServer.shutdown().get();
                    LOG.info("Shut down HTTP server at {}.", uri);
                } catch (Exception e) {
                    LOG.warn("HTTP server could not be stopped properly: {}", e.getMessage());
                }
            });
        } finally {
            registryLock.unlock();
        }
        LOG.info("HTTP servers shut down.");
    }

    @Override
    public URI registerContext(String host, Integer port, String contextPath, String mediaType, HttpHandler handler) {
        registryLock.lock();
        try {
            String mapKey = concatHostAndPort(host, port);
            URI baseAddress = Optional.ofNullable(addressRegistry.get(mapKey)).orElseGet(() -> {
                URI uri = uriBuilder.buildUri(host, port);
                addressRegistry.put(mapKey, uri);
                return uri;
            });

            HttpServer httpServer = Optional.ofNullable(serverRegistry.get(mapKey))
                    .orElseGet(() -> {
                        LOG.info("Setup HTTP server for address '{}'.", baseAddress);
                        HttpServer result = GrizzlyHttpServerFactory.createHttpServer(baseAddress, true);
                        serverRegistry.put(mapKey, result);
                        return result;
                    });

            HandlerWrapper hw = new HandlerWrapper(handler);
            handlerRegistry.put(contextPath, hw);
            LOG.info("Register context path '{}' at HTTP server '{}'", contextPath, baseAddress);
            org.glassfish.grizzly.http.server.HttpHandler hdlr = new org.glassfish.grizzly.http.server.HttpHandler(contextPath) {
                @Override
                public void service(Request request, Response response) throws Exception {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Request to {}", getName());
                    }
                    response.setStatus(HttpStatus.OK_200);
                    response.setContentType(mediaType);
                    try {
                        if (!hw.isActive()) {
                            response.setStatus(HttpStatus.NOT_IMPLEMENTED_501);
                            return;
                        }
                        handler.process(request.getInputStream(), response.getOutputStream(),
                                new TransportInfo(request.getScheme(), request.getLocalAddr(), request.getLocalPort()));
                    } catch (Exception e) {
                        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
                        response.getOutputStream().flush();
                    }
                }
            };
            httpServer.getServerConfiguration().addHttpHandler(hdlr, contextPath);

            return URI.create(baseAddress.toString() + contextPath);
        } catch (UnknownHostException e) {
            LOG.warn("Cannot resolve host name.", e);
            throw new RuntimeException(e);
        } finally {
            registryLock.unlock();
        }
    }

    @Override
    public URI registerContext(String host, Integer port, String contextPath, HttpHandler handler) {
        return registerContext(host, port, contextPath, SoapConstants.MEDIA_TYPE_SOAP, handler);
    }

    @Override
    public void unregisterContext(String host, Integer port, String contextPath) {
        registryLock.lock();
        try {
            String hostWithPort = concatHostAndPort(host, port);
            Optional.ofNullable(serverRegistry.get(hostWithPort)).ifPresent(httpServer ->
            {
                Optional.ofNullable(handlerRegistry.get(contextPath)).ifPresent(handlerWrapper -> {
                    LOG.info("Unregister context path '{}'", contextPath);
                    handlerWrapper.deactivate();
                    handlerRegistry.remove(contextPath);
                });

                if (handlerRegistry.isEmpty()) {
                    LOG.info("No further HTTP handlers active. Shutdown HTTP server at '{}:{}'", host, port);
                    httpServer.shutdown();
                    serverRegistry.remove(hostWithPort);
                }
            });
        } catch (UnknownHostException e) {
            LOG.warn("Cannot resolve host name.", e);
        } finally {
            registryLock.unlock();
        }
    }

    private class HandlerWrapper implements HttpHandler {
        private final HttpHandler handler;
        private final AtomicBoolean active;

        HandlerWrapper(HttpHandler handler) {
            this.handler = handler;
            this.active = new AtomicBoolean(true);
        }

        @Override
        public void process(InputStream inStream, OutputStream outStream, TransportInfo transportInfo)
                throws TransportException, MarshallingException {
            handler.process(inStream, outStream, transportInfo);
        }

        void deactivate() {
            active.set(false);
        }

        boolean isActive() {
            return active.get();
        }
    }

    /**
     * Concat host and port, whereby host address is always used instead of DNS name.
     *
     * @throws UnknownHostException if host address cannot be resolved.
     */
    private String concatHostAndPort(String host, Integer port) throws UnknownHostException {
        InetAddress address = InetAddress.getByName(host);
        return address.getHostAddress() + ":" + port;
    }
}
