package org.somda.sdc.dpws.http.grizzly;

import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.glassfish.grizzly.http.CompressionConfig;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpContainer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.CommunicationLogImpl;
import org.somda.sdc.dpws.DpwsConfig;
import org.somda.sdc.dpws.crypto.CryptoConfig;
import org.somda.sdc.dpws.crypto.CryptoConfigurator;
import org.somda.sdc.dpws.crypto.CryptoSettings;
import org.somda.sdc.dpws.http.HttpHandler;
import org.somda.sdc.dpws.http.HttpServerRegistry;
import org.somda.sdc.dpws.http.HttpUriBuilder;
import org.somda.sdc.dpws.soap.SoapConstants;
import org.somda.sdc.dpws.soap.TransportInfo;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * {@linkplain HttpServerRegistry} implementation based on Grizzly HTTP servers.
 */
public class GrizzlyHttpServerRegistry extends AbstractIdleService implements HttpServerRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(GrizzlyHttpServerRegistry.class);

    private final Map<String, HttpServer> serverRegistry;
    private final Map<String, GrizzlyHttpHandlerBroker> handlerRegistry;
    private final Lock registryLock;
    private final HttpUriBuilder uriBuilder;
    private final CommunicationLog communicationLog;
    private final boolean enableGzipCompression;
    private SSLContextConfigurator sslContextConfigurator; // null => no support for SSL enabled/configured

    @Inject
    GrizzlyHttpServerRegistry(HttpUriBuilder uriBuilder,
                              CryptoConfigurator cryptoConfigurator,
                              @Nullable @Named(CryptoConfig.CRYPTO_SETTINGS) CryptoSettings cryptoSettings,
                              CommunicationLog communicationLog,
                              @Named(DpwsConfig.HTTP_GZIP_COMPRESSION) boolean enableGzipCompression) {
        this.uriBuilder = uriBuilder;
        this.communicationLog = communicationLog;
        serverRegistry = new HashMap<>();
        handlerRegistry = new HashMap<>();
        registryLock = new ReentrantLock();
        this.enableGzipCompression = enableGzipCompression;
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
            LOG.warn("Could not read server crypto config, fallback to system properties");
            sslContextConfigurator = cryptoConfigurator.createSslContextConfiguratorSystemProperties();
        }
    }

    @Override
    protected void startUp() {
        // nothing to do here - servers will be started on demand
        LOG.info("{} is running", getClass().getSimpleName());
    }

    @Override
    protected void shutDown() {
        LOG.info("Shut down running HTTP servers");
        registryLock.lock();
        try {
            serverRegistry.forEach((uri, httpServer) -> {
                try {
                    httpServer.shutdown().get();
                    LOG.info("Shut down HTTP server at {}", uri);
                } catch (Exception e) {
                    LOG.warn("HTTP server could not be stopped properly: {}", e.getMessage());
                }
            });
        } finally {
            registryLock.unlock();
        }
        LOG.info("HTTP servers shut down");
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
            String mapKeyString = makeMapKey(httpServerInfo.getUri(), contextPath);
            URI mapKeyUri = URI.create(mapKeyString);
            GrizzlyHttpHandlerBroker handlerBroker = new GrizzlyHttpHandlerBroker(mediaType, handler, mapKeyUri.toString());
            handlerRegistry.put(mapKeyString, handlerBroker);

            LOG.info("Register context path '{}' at HTTP server '{}'", contextPath, httpServerInfo.getUri());
            httpServerInfo.getHttpServer().getServerConfiguration().addHttpHandler(handlerBroker, contextPath);
            return mapKeyUri;
        } catch (UnknownHostException e) {
            LOG.warn("Unexpected URI conversion error");
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
            LOG.warn("Cannot resolve host name", e);
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
            // schemeAndAuthority.getScheme() is picked on purpose; netListener.getScheme() is always null for unknown
            // reasons. As getScheme() from schemeAndAuthority also does the job, further investigation is neglected
            URI uri = uriBuilder.buildUri(schemeAndAuthority.getScheme(), netListener.getHost(), netListener.getPort());

            serverRegistry.put(makeMapKey(uri), newHttpServer);
            LOG.debug("New HTTP server initialized: {}", uri);
            return new HttpServerInfo(newHttpServer, uri);
        } catch (UnknownHostException e) {
            LOG.warn("Cannot resolve host name", e);
            throw new RuntimeException(e);
        } catch (IndexOutOfBoundsException e) {
            LOG.warn("No network listener found for requested HTTP server: {}", schemeAndAuthority, e);
            throw new RuntimeException(e);
        } catch (IOException e) {
            LOG.warn("Could not start http server for: {}", schemeAndAuthority, e);
            throw new RuntimeException(e);
        }
    }

    private HttpServer createHttpServer(URI uri) throws IOException {
        LOG.info("Setup HTTP server for address '{}'", uri);
        if (uri.getScheme().equalsIgnoreCase("http")) {
            var server = GrizzlyHttpServerFactory.createHttpServer(uri, false);
            enableCompression(server);
            server.start();
            return server;
        }
        if (sslContextConfigurator != null && uri.getScheme().equalsIgnoreCase("https")) {
            final SSLEngineConfigurator sslEngineConfigurator = new SSLEngineConfigurator(sslContextConfigurator);
            sslEngineConfigurator.setNeedClientAuth(true).setClientMode(false);
            var server = GrizzlyHttpServerFactory.createHttpServer(
                    uri,
                    (GrizzlyHttpContainer) null,
                    true,
                    sslEngineConfigurator,
                    false);
            enableCompression(server);
            server.start();
            return server;
        }
        throw new RuntimeException(String.format("HTTP server setup failed. Unknown scheme: %s", uri.getScheme()));
    }

    private void enableCompression(HttpServer server) {
        if (enableGzipCompression) {
            CompressionConfig compressionConfig =
                    server.getListener("grizzly").getCompressionConfig();
            compressionConfig.setCompressionMode(CompressionConfig.CompressionMode.ON); // the mode
            compressionConfig.setCompressionMinSize(1); // the min amount of bytes to compress
            compressionConfig.setDecompressionEnabled(true);
            // the mime types to compress
            compressionConfig.setCompressibleMimeTypes(
                    "text/plain", "text/html",
                    SoapConstants.MEDIA_TYPE_SOAP, SoapConstants.MEDIA_TYPE_WSDL
            );
        }
    }

    /*
     * Calculate http server map key:
     * - scheme is transformed to lower case.
     * - host address is used instead of DNS name.
     *
     * throws UnknownHostException if host address cannot be resolved.
     */
    private String makeMapKey(URI uri) throws UnknownHostException {
        InetAddress address = InetAddress.getByName(uri.getHost());
        return uriBuilder.buildUri(uri.getScheme().toLowerCase(), address.getHostAddress(), uri.getPort()).toString();
    }

    /*
     * Calculate http server handler map key:
     * - scheme is transformed to lower case.
     * - host address is used instead of DNS name.
     * - context path is appended to base URI.
     *
     * throws UnknownHostException if host address cannot be resolved.
     */
    private String makeMapKey(URI uri, String contextPath) throws UnknownHostException {
        return makeMapKey(uri) + contextPath;
    }

    private class GrizzlyHttpHandlerBroker extends org.glassfish.grizzly.http.server.HttpHandler {
        private final String mediaType;
        private final HttpHandler handler;
        private final String requestedUri;

        GrizzlyHttpHandlerBroker(String mediaType,
                                 HttpHandler handler,
                                 String requestedUri) {
            this.mediaType = mediaType;
            this.handler = handler;
            this.requestedUri = requestedUri;
        }

        @Override
        public void service(Request request, Response response) throws Exception {
            InputStream input = request.getInputStream();
            
            LOG.debug("Request to {}", requestedUri);
            input = communicationLog.logHttpMessage(CommunicationLogImpl.HttpDirection.INBOUND_REQUEST,
                        request.getRemoteHost(), request.getRemotePort(), input);

            response.setStatus(HttpStatus.OK_200);
            response.setContentType(mediaType);
            
            OutputStream output = communicationLog.logHttpMessage(CommunicationLogImpl.HttpDirection.OUTBOUND_RESPONSE,
                        request.getRemoteHost(), request.getRemotePort(), response.getOutputStream());
            
            try {
            	
            	handler.process(input, output,
                        new TransportInfo(request.getScheme(), request.getLocalAddr(), request.getLocalPort()));
            	
            } catch (Exception e) {
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
                
                output.flush();
                output.write(e.getMessage().getBytes());
                LOG.error("Internal server error processing request.", e);
            }
        }
    }

    private class HttpServerInfo {
        private final HttpServer httpServer;
        private final URI uri;

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
