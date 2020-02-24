package org.somda.sdc.dpws.http.jetty;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.io.ssl.SslConnection;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.dpws.DpwsConfig;
import org.somda.sdc.dpws.crypto.CryptoConfig;
import org.somda.sdc.dpws.crypto.CryptoConfigurator;
import org.somda.sdc.dpws.crypto.CryptoSettings;
import org.somda.sdc.dpws.http.HttpHandler;
import org.somda.sdc.dpws.http.HttpServerRegistry;
import org.somda.sdc.dpws.http.HttpUriBuilder;
import org.somda.sdc.dpws.http.jetty.factory.JettyHttpServerHandlerFactory;
import org.somda.sdc.dpws.soap.SoapConstants;

import javax.annotation.Nullable;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * {@linkplain HttpServerRegistry} implementation based on Jetty HTTP servers.
 */
public class JettyHttpServerRegistry extends AbstractIdleService implements HttpServerRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(JettyHttpServerRegistry.class);

    private JettyHttpServerHandlerFactory jettyHttpServerHandlerFactory;

    private final Map<String, Server> serverRegistry;
    private final Map<String, JettyHttpServerHandler> handlerRegistry;
    private final Map<String, ContextHandler> contextWrapperRegistry;
    private final Map<Server, ContextHandlerCollection> contextHandlerMap;
    private final Lock registryLock;
    private final HttpUriBuilder uriBuilder;
    private final boolean enableGzipCompression;
    private final int minCompressionSize;
    private final String[] tlsProtocols;
    private final HostnameVerifier hostnameVerifier;
    private SSLContext sslContext;

    @Inject
    JettyHttpServerRegistry(HttpUriBuilder uriBuilder,
                            CryptoConfigurator cryptoConfigurator,
                            @Nullable @Named(CryptoConfig.CRYPTO_SETTINGS) CryptoSettings cryptoSettings,
                            JettyHttpServerHandlerFactory jettyHttpServerHandlerFactory,
                            @Named(DpwsConfig.HTTP_GZIP_COMPRESSION) boolean enableGzipCompression,
                            @Named(DpwsConfig.HTTP_RESPONSE_COMPRESSION_MIN_SIZE) int minCompressionSize,
                            @Named(CryptoConfig.CRYPTO_TLS_ENABLED_VERSIONS) String[] tlsProtocols,
                            @Named(CryptoConfig.CRYPTO_DEVICE_HOSTNAME_VERIFIER) HostnameVerifier hostnameVerifier) {
        this.uriBuilder = uriBuilder;
        this.jettyHttpServerHandlerFactory = jettyHttpServerHandlerFactory;
        this.enableGzipCompression = enableGzipCompression;
        this.minCompressionSize = minCompressionSize;
        this.tlsProtocols = tlsProtocols;
        this.hostnameVerifier = hostnameVerifier;
        serverRegistry = new HashMap<>();
        handlerRegistry = new HashMap<>();
        contextHandlerMap = new HashMap<>();
        contextWrapperRegistry = new HashMap<>();
        registryLock = new ReentrantLock();
        configureSsl(cryptoConfigurator, cryptoSettings);

    }

    @Override
    protected void startUp() throws Exception {
        // nothing to do here - servers will be started on demand
        LOG.info("{} is running", getClass().getSimpleName());
    }

    @Override
    protected void shutDown() throws Exception {
        LOG.info("Shut down running HTTP servers");
        registryLock.lock();
        try {

            serverRegistry.forEach((uri, server) -> {
                try {

                    server.stop();
                    LOG.info("Shut down HTTP server at {}", uri);

                    ContextHandlerCollection contextHandlerCollection = contextHandlerMap.remove(server);
                    if (contextHandlerCollection != null) {
                        contextHandlerCollection.stop();
                        LOG.info("Shut down HTTP context handler collection at {}", uri);
                    }

                    this.handlerRegistry.forEach(
                            (handlerUri, handler) -> {
                                try {
                                    handler.stop();
                                } catch (Exception e) {
                                    LOG.warn("HTTP handler could not be stopped properly", e);
                                }
                            }
                    );
                    handlerRegistry.clear();

                    contextWrapperRegistry.forEach((contextPath, wrapper) -> {
                                try {
                                    wrapper.stop();
                                } catch (Exception e) {
                                    LOG.warn("HTTP handler wrapper could not be stopped properly", e);
                                }
                            }
                    );
                    contextWrapperRegistry.clear();

                } catch (Exception e) {
                    LOG.warn("HTTP server could not be stopped properly", e);
                }
            });

            serverRegistry.clear();

        } finally {
            registryLock.unlock();
        }
    }

    @Override
    public URI initHttpServer(URI schemeAndAuthority) {
        registryLock.lock();
        try {
            var server = makeHttpServer(schemeAndAuthority);
            var uriString = server.getURI().toString();
            if (uriString.endsWith("/")) {
                uriString = uriString.substring(0, uriString.length() - 1);
            }
            return URI.create(uriString);
        } finally {
            registryLock.unlock();
        }
    }

    @Override
    public URI registerContext(URI schemeAndAuthority, String contextPath, HttpHandler handler) {
        return registerContext(schemeAndAuthority, contextPath, SoapConstants.MEDIA_TYPE_SOAP, handler);
    }

    @Override
    public URI registerContext(URI schemeAndAuthority, String contextPath, String mediaType, HttpHandler handler) {
        if (!contextPath.startsWith("/")) {
            throw new RuntimeException(String.format("Context path needs to start with a slash, but is %s",
                    contextPath));
        }

        registryLock.lock();
        try {
            Server server = makeHttpServer(schemeAndAuthority);
            String mapKey;
            try {
                mapKey = makeMapKey(server.getURI(), contextPath);
            } catch (UnknownHostException e) {
                LOG.error("Unexpected URI conversion error", e);
                throw new RuntimeException("Unexpected URI conversion error");
            }
            URI mapKeyUri = URI.create(mapKey);

            JettyHttpServerHandler endpointHandler = this.jettyHttpServerHandlerFactory.create(
                    this.sslContext != null, mediaType, handler);

            ContextHandler context = new ContextHandler(contextPath);
            context.setHandler(endpointHandler);
            context.setAllowNullPathInfo(true);

            this.handlerRegistry.put(mapKeyUri.toString(), endpointHandler);
            this.contextWrapperRegistry.put(contextPath, context);

            ContextHandlerCollection contextHandler = this.contextHandlerMap.get(server);
            contextHandler.addHandler(context);

            context.start();

            return mapKeyUri;

        } catch (Exception e) {
            LOG.error("Registering context {} failed.", contextPath, e);
            throw new RuntimeException(e);
        } finally {
            registryLock.unlock();
        }
    }

    @Override
    public void unregisterContext(URI schemeAndAuthority, String contextPath) {
        registryLock.lock();
        try {
            String serverRegistryKey;
            String httpHandlerRegistryKey;

            try {
                serverRegistryKey = makeMapKey(schemeAndAuthority);
                httpHandlerRegistryKey = makeMapKey(schemeAndAuthority, contextPath);
            } catch (UnknownHostException e) {
                LOG.error("Unexpected URI conversion error", e);
                throw new RuntimeException("Unexpected URI conversion error");
            }

            Optional.ofNullable(serverRegistry.get(serverRegistryKey)).ifPresent(httpServer ->
            {
                Optional.ofNullable(handlerRegistry.get(httpHandlerRegistryKey)).ifPresent(handlerWrapper -> {
                    LOG.info("Unregister context path '{}'", contextPath);
                    handlerRegistry.remove(httpHandlerRegistryKey);
                    ContextHandler removedHandler = contextWrapperRegistry.remove(contextPath);
                    ContextHandlerCollection servletContextHandler = contextHandlerMap.get(httpServer);
                    servletContextHandler.removeHandler(removedHandler);
                });

                if (handlerRegistry.isEmpty()) {
                    LOG.info("No further HTTP handlers active. Shutdown HTTP server at '{}'", schemeAndAuthority);
                    try {
                        httpServer.stop();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    serverRegistry.remove(serverRegistryKey);
                }
            });
        } finally {
            registryLock.unlock();
        }

    }


    private void configureSsl(CryptoConfigurator cryptoConfigurator,
                              @Nullable CryptoSettings cryptoSettings) {
        if (cryptoSettings == null) {
            sslContext = null;
            return;
        }

        try {
            sslContext = cryptoConfigurator.createSslContextFromCryptoConfig(cryptoSettings);
        } catch (IllegalArgumentException |
                KeyStoreException |
                UnrecoverableKeyException |
                CertificateException |
                NoSuchAlgorithmException |
                IOException |
                KeyManagementException e) {
            LOG.warn("Could not read server crypto config, fallback to system properties");
            sslContext = cryptoConfigurator.createSslContextFromSystemProperties();
        }
    }

    private Server makeHttpServer(URI uri) {
        String mapKey;
        try {
            mapKey = makeMapKey(uri);
        } catch (UnknownHostException e) {
            LOG.error("Unexpected URI conversion error", e);
            throw new RuntimeException("Unexpected URI conversion error");
        }

        Optional<Server> oldServer = Optional.ofNullable(serverRegistry.get(mapKey));
        if (oldServer.isPresent()) {
            LOG.debug("Re-use running HTTP server from URI: {}", oldServer.get().getURI().getHost());
            return oldServer.get();
        }

        LOG.debug("Init new HTTP server from URI: {}", uri);
        Server httpServer = createHttpServer(uri);
        try {
            httpServer.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        URI serverUri = httpServer.getURI();

        try {
            serverRegistry.put(makeMapKey(serverUri), httpServer);
        } catch (UnknownHostException e) {
            LOG.error("Unexpected URI conversion error", e);
            throw new RuntimeException("Unexpected URI conversion error");
        }
        LOG.debug("New HTTP server initialized: {}", uri);
        return httpServer;
    }

    private Server createHttpServer(URI uri) {
        LOG.info("Setup HTTP server for address '{}'", uri);
        if (!uri.getScheme().toLowerCase().startsWith("http")) {
            throw new RuntimeException(String.format("HTTP server setup failed. Unknown scheme: %s", uri.getScheme()));

        }
        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setSecureScheme("https");

        var server = new Server(new InetSocketAddress(
                uri.getHost(),
                uri.getPort()));

        ContextHandlerCollection context = new ContextHandlerCollection();
        server.setHandler(context);
        this.contextHandlerMap.put(server, context);

        // wrap the context handler in a gzip handler
        if (this.enableGzipCompression) {
            GzipHandler gzipHandler = new GzipHandler();
            gzipHandler.setIncludedMethods("PUT", "POST", "GET");
            gzipHandler.setInflateBufferSize(2048);
            gzipHandler.setHandler(context);
            gzipHandler.setMinGzipSize(minCompressionSize);
            gzipHandler.setIncludedMimeTypes(
                    "text/plain", "text/html",
                    SoapConstants.MEDIA_TYPE_SOAP, SoapConstants.MEDIA_TYPE_WSDL
            );
            server.setHandler(gzipHandler);
        }

        if (sslContext != null && uri.getScheme().equalsIgnoreCase("https")) {
            SslContextFactory.Server fac = new SslContextFactory.Server();
            fac.setSslContext(sslContext);
            fac.setNeedClientAuth(true);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Enabled protocols: {}", List.of(tlsProtocols));
            }
            fac.setIncludeProtocols(tlsProtocols);
            // reset excluded protocols to force only included protocols
            fac.setExcludeProtocols();

            HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
            SecureRequestCustomizer src = new SecureRequestCustomizer();
            var clientVerifier = new HttpConfiguration.Customizer() {
                @Override
                public void customize(Connector connector, HttpConfiguration channelConfig, Request request) {
                    var numRequest = request.getHttpChannel().getRequests();
                    if (numRequest != 1) {
                        LOG.debug("Connection already verified");
                        return;
                    }
                    EndPoint endp = request.getHttpChannel().getEndPoint();
                    if (endp instanceof SslConnection.DecryptedEndPoint) {
                        SslConnection.DecryptedEndPoint sslEndp = (SslConnection.DecryptedEndPoint) endp;
                        SslConnection sslConnection = sslEndp.getSslConnection();
                        SSLEngine sslEngine = sslConnection.getSSLEngine();

                        var session = sslEngine.getSession();
                        endp.getLocalAddress().getHostName();

                        if (!hostnameVerifier.verify(sslEndp.getLocalAddress().getHostName(), session)) {
                            LOG.debug("HostnameVerifier has filtered request, marking request as handled and aborting request");
                            request.setHandled(true);
                            request.getHttpChannel().abort(new Exception("HostnameVerifier has rejected request"));
                        }
                    }
                }
            };
            httpsConfig.addCustomizer(clientVerifier);
            httpsConfig.addCustomizer(src);

            ServerConnector httpsConnector = new ServerConnector(server,
                    new SslConnectionFactory(fac, "http/1.1"),
                    new HttpConnectionFactory(httpsConfig));
            httpsConnector.setIdleTimeout(50000);

            server.setConnectors(new Connector[]{httpsConnector});
        }

        return server;
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

    private String makeMapKey(URI uri, String contextPath) throws UnknownHostException {
        return makeMapKey(uri) + contextPath;
    }
}
