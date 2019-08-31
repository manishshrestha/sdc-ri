package org.ieee11073.sdc.dpws.http.grizzly;

import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpContainer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.ieee11073.sdc.dpws.crypto.CryptoConfig;
import org.ieee11073.sdc.dpws.crypto.CryptoConfigurator;
import org.ieee11073.sdc.dpws.crypto.CryptoSettings;
import org.ieee11073.sdc.dpws.http.HttpHandler;
import org.ieee11073.sdc.dpws.http.HttpServerRegistry;
import org.ieee11073.sdc.dpws.http.HttpUriBuilder;
import org.ieee11073.sdc.dpws.soap.SoapConstants;
import org.ieee11073.sdc.dpws.soap.TransportInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * {@linkplain HttpServerRegistry} implementation with Grizzly HTTP server.
 */
public class GrizzlyHttpServerRegistry extends AbstractIdleService implements HttpServerRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(GrizzlyHttpServerRegistry.class);

    private final Map<String, HttpServer> serverRegistry;
    private final Map<String, GrizzlyHttpHandlerBroker> handlerRegistry;
    private final Lock registryLock;
    private final HttpUriBuilder uriBuilder;
    private SSLContextConfigurator sslContextConfigurator; // null => no support for SSL enabled/configured

    @Inject
    GrizzlyHttpServerRegistry(HttpUriBuilder uriBuilder,
                              CryptoConfigurator cryptoConfigurator,
                              @Nullable @Named(CryptoConfig.CRYPTO_SETTINGS) CryptoSettings cryptoSettings) {
        this.uriBuilder = uriBuilder;
        serverRegistry = new HashMap<>();
        handlerRegistry = new HashMap<>();
        registryLock = new ReentrantLock();
        configureSsl(cryptoConfigurator, cryptoSettings);

    }

    private void configureSsl(CryptoConfigurator cryptoConfigurator,
                              @Nullable CryptoSettings cryptoSettings) {
        if (cryptoSettings == null) {
            sslContextConfigurator = null;
            return;
        }

        try {
            sslContextConfigurator = cryptoConfigurator.createSslContextConfiguratorFromCryptoConfig(cryptoSettings);
        } catch (IllegalArgumentException e) {
            LOG.warn("Could not read server crypto config, fallback to system properties.");
            sslContextConfigurator = cryptoConfigurator.createSslContextConfiguratorSystemProperties();
        }
    }

    @Override
    protected void startUp() {
        // nothing to do here - servers will be started on demand
        LOG.info("{} is running.", getClass().getSimpleName());
    }

    @Override
    protected void shutDown() {
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
    public URI registerContext(URI schemeAndAuthority, String contextPath, String mediaType, HttpHandler handler) {
        if (!contextPath.startsWith("/")) {
            throw new RuntimeException(String.format("Context path needs to start with a slash, but is %s",
                    contextPath));
        }

        registryLock.lock();
        try {
            HttpServerInfo httpServerInfo = makeHttpServerIfNotExistingFor(schemeAndAuthority);
            GrizzlyHttpHandlerBroker handlerBroker = new GrizzlyHttpHandlerBroker(mediaType, handler);
            String mapKeyString = makeMapKey(httpServerInfo.getUri(), contextPath);
            URI mapKeyUri = URI.create(mapKeyString);
            handlerRegistry.put(mapKeyString, handlerBroker);

            LOG.info("Register context path '{}' at HTTP server '{}'", contextPath, httpServerInfo.getUri());
            httpServerInfo.getHttpServer().getServerConfiguration().addHttpHandler(handlerBroker, contextPath);
            return mapKeyUri;
        } catch (UnknownHostException e) {
            LOG.warn("Unexpected URI conversion error.");
            throw new RuntimeException(e);
        } finally {
            registryLock.unlock();
        }
    }

    @Override
    public URI initHttpServer(URI schemeAndAuthority) {
        registryLock.lock();
        try {
            return makeHttpServerIfNotExistingFor(schemeAndAuthority).getUri();
        } finally {
            registryLock.unlock();
        }
    }

    @Override
    public URI registerContext(URI schemeAndAuthority, String contextPath, HttpHandler handler) {
        return registerContext(schemeAndAuthority, contextPath, SoapConstants.MEDIA_TYPE_SOAP, handler);
    }

    @Override
    public void unregisterContext(URI schemeAndAuthority, String contextPath) {
        registryLock.lock();
        try {
            String serverRegistryKey = makeMapKey(schemeAndAuthority);
            String httpHandlerRegistryKey = makeMapKey(schemeAndAuthority, contextPath);
            Optional.ofNullable(serverRegistry.get(serverRegistryKey)).ifPresent(httpServer ->
            {
                Optional.ofNullable(handlerRegistry.get(httpHandlerRegistryKey)).ifPresent(handlerWrapper -> {
                    LOG.info("Unregister context path '{}'", contextPath);
                    handlerRegistry.remove(contextPath);
                    httpServer.getServerConfiguration().removeHttpHandler(handlerWrapper);
                });

                if (handlerRegistry.isEmpty()) {
                    LOG.info("No further HTTP handlers active. Shutdown HTTP server at '{}'", schemeAndAuthority);
                    httpServer.shutdown();
                    serverRegistry.remove(serverRegistryKey);
                }
            });
        } catch (UnknownHostException e) {
            LOG.warn("Cannot resolve host name.", e);
            throw new RuntimeException(e);
        } finally {
            registryLock.unlock();
        }
    }

    private HttpServerInfo makeHttpServerIfNotExistingFor(URI schemeAndAuthority) {
        try {
            final String mapKey = makeMapKey(schemeAndAuthority);
            Optional<HttpServer> httpServer = Optional.ofNullable(serverRegistry.get(mapKey));
            if (httpServer.isPresent()) {
                LOG.debug("Re-use running HTTP server from URI: {}", schemeAndAuthority);
                return new HttpServerInfo(httpServer.get(), schemeAndAuthority);
            }

            LOG.debug("Init new HTTP server from URI: {}", schemeAndAuthority);
            HttpServer newHttpServer = createHttpServer(URI.create(mapKey));

            NetworkListener netListener = Iterables.get(newHttpServer.getListeners(), 0);
            // schemeAndAuthority is picked on purpose; netListener is always null
            URI uri = uriBuilder.buildUri(schemeAndAuthority.getScheme(), netListener.getHost(), netListener.getPort());

            serverRegistry.put(makeMapKey(uri), newHttpServer);
            LOG.debug("New HTTP server initialized: {}", uri);
            return new HttpServerInfo(newHttpServer, uri);
        } catch (UnknownHostException e) {
            LOG.warn("Cannot resolve host name.", e);
            throw new RuntimeException(e);
        } catch (IndexOutOfBoundsException e) {
            LOG.warn("No network listener found for requested HTTP server: {}", schemeAndAuthority);
            throw new RuntimeException(e);
        }
    }

    private HttpServer createHttpServer(URI uri) {
        LOG.info("Setup HTTP server for address '{}'", uri);
        if (uri.getScheme().equalsIgnoreCase("http")) {
            return GrizzlyHttpServerFactory.createHttpServer(uri, true);
        }
        if (sslContextConfigurator != null && uri.getScheme().equalsIgnoreCase("https")) {
            final SSLEngineConfigurator sslEngineConfigurator = new SSLEngineConfigurator(sslContextConfigurator);
            sslEngineConfigurator.setNeedClientAuth(true).setClientMode(false); //.setWantClientAuth(false);
            return GrizzlyHttpServerFactory.createHttpServer(
                    uri,
                    (GrizzlyHttpContainer) null,
                    true,
                    sslEngineConfigurator,
                    true);
        }

        throw new RuntimeException(String.format("HTTP server setup failed. Unknown scheme: %s", uri.getScheme()));
    }

    /**
     * Calculate http server map key:
     *
     * - scheme is transformed to lower case.
     * - host address is used instead of DNS name.
     *
     * @throws UnknownHostException if host address cannot be resolved.
     */
    private String makeMapKey(URI uri) throws UnknownHostException {
        InetAddress address = InetAddress.getByName(uri.getHost());
        return uriBuilder.buildUri(uri.getScheme().toLowerCase(), address.getHostAddress(), uri.getPort()).toString();
    }

    /**
     * Calculate http server handler map key:
     *
     * - scheme is transformed to lower case.
     * - host address is used instead of DNS name.
     * - context path is appended to base URI.
     *
     * @throws UnknownHostException if host address cannot be resolved.
     */
    private String makeMapKey(URI uri, String contextPath) throws UnknownHostException {
        return makeMapKey(uri) + contextPath;
    }

    private class GrizzlyHttpHandlerBroker extends org.glassfish.grizzly.http.server.HttpHandler {
        private final String mediaType;
        private final HttpHandler handler;

        GrizzlyHttpHandlerBroker(String mediaType,
                                 HttpHandler handler) {
            this.mediaType = mediaType;
            this.handler = handler;
        }

        @Override
        public void service(Request request, Response response) throws Exception {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Request to {}", getName());
            }

            response.setStatus(HttpStatus.OK_200);
            response.setContentType(mediaType);

            try {
                handler.process(request.getInputStream(), response.getOutputStream(),
                        new TransportInfo(request.getScheme(), request.getLocalAddr(), request.getLocalPort()));
            } catch (Exception e) {
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
                response.getOutputStream().flush();
                response.getOutputStream().write(e.getMessage().getBytes());
            }
        }
    }

    private class HttpServerInfo {
        private HttpServer httpServer;
        private URI uri;

        public HttpServerInfo(HttpServer httpServer, URI uri) {
            this.httpServer = httpServer;
            this.uri = uri;
        }

        public HttpServer getHttpServer() {
            return httpServer;
        }

        public URI getUri() {
            return uri;
        }
    }
}
