package org.ieee11073.sdc.dpws.http.grizzly;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.glassfish.grizzly.http.server.HttpServer;
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
import org.ieee11073.sdc.dpws.device.DeviceConfig;
import org.ieee11073.sdc.dpws.http.HttpHandler;
import org.ieee11073.sdc.dpws.http.HttpServerRegistry;
import org.ieee11073.sdc.dpws.http.HttpUriBuilder;
import org.ieee11073.sdc.dpws.soap.SoapConstants;
import org.ieee11073.sdc.dpws.soap.TransportInfo;
import org.ieee11073.sdc.dpws.soap.exception.MarshallingException;
import org.ieee11073.sdc.dpws.soap.exception.TransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
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
    private SSLContextConfigurator sslContextConfigurator; // null => no support for SSL enabled/configured

    @Inject
    GrizzlyHttpServerRegistry(HttpUriBuilder uriBuilder,
                              CryptoConfigurator cryptoConfigurator,
                              @Nullable @Named(CryptoConfig.CRYPTO_SETTINGS) CryptoSettings cryptoSettings) {
        this.uriBuilder = uriBuilder;
        addressRegistry = new HashMap<>();
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
    public URI registerContext(URI schemeAndAuthority, String contextPath, String mediaType, HttpHandler handler) {
        registryLock.lock();
        try {
            final String mapKey = calculateMapKey(schemeAndAuthority);
            final URI registeredBaseAddress = Optional.ofNullable(addressRegistry.get(mapKey)).orElseGet(() -> {
                URI uri = uriBuilder.buildUri(
                        schemeAndAuthority.getScheme(), schemeAndAuthority.getHost(), schemeAndAuthority.getPort());
                addressRegistry.put(mapKey, uri);
                return uri;
            });

            final HttpServer httpServer = Optional.ofNullable(serverRegistry.get(mapKey))
                    .orElseGet(() -> {
                        HttpServer srv = createHttpServer(registeredBaseAddress);
                        serverRegistry.put(mapKey, srv);
                        return srv;
                    });

            HandlerWrapper hw = new HandlerWrapper(handler);
            handlerRegistry.put(contextPath, hw);
            LOG.info("Register context path '{}' at HTTP server '{}'", contextPath, registeredBaseAddress);
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

            return URI.create(registeredBaseAddress.toString() + contextPath);
        } catch (UnknownHostException e) {
            LOG.warn("Cannot resolve host name.", e);
            throw new RuntimeException(e);
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
            String hostWithPort = calculateMapKey(schemeAndAuthority);
            Optional.ofNullable(serverRegistry.get(hostWithPort)).ifPresent(httpServer ->
            {
                Optional.ofNullable(handlerRegistry.get(contextPath)).ifPresent(handlerWrapper -> {
                    LOG.info("Unregister context path '{}'", contextPath);
                    handlerWrapper.deactivate();
                    handlerRegistry.remove(contextPath);
                });

                if (handlerRegistry.isEmpty()) {
                    LOG.info("No further HTTP handlers active. Shutdown HTTP server at '{}'", schemeAndAuthority);
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
     * Calculate http server map key, whereby host address is always used instead of DNS name.
     *
     * @throws UnknownHostException if host address cannot be resolved.
     */
    private String calculateMapKey(URI uri) throws UnknownHostException {
        InetAddress address = InetAddress.getByName(uri.getHost());
        return uri.getScheme().toLowerCase() + "://" + address.getHostAddress() + ":" + uri.getPort();
    }
}
