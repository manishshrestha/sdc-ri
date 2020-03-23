package org.somda.sdc.dpws.http.jetty;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpScheme;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.io.ssl.SslConnection;
import org.eclipse.jetty.server.AbstractConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.OptionalSslConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.DpwsConfig;
import org.somda.sdc.dpws.crypto.CryptoConfig;
import org.somda.sdc.dpws.crypto.CryptoConfigurator;
import org.somda.sdc.dpws.crypto.CryptoSettings;
import org.somda.sdc.dpws.device.DeviceConfig;
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
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.time.Duration;
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

    private final CommunicationLog communicationLog;
    private final Map<String, Server> serverRegistry;
    private final Map<String, JettyHttpServerHandler> handlerRegistry;
    private final Map<String, ContextHandler> contextWrapperRegistry;
    private final Map<Server, ContextHandlerCollection> contextHandlerMap;
    private final Lock registryLock;
    private final HttpUriBuilder uriBuilder;
    private final boolean enableGzipCompression;
    private final int minCompressionSize;
    private final String[] tlsProtocols;
    private final String[] enabledCiphers;
    private final HostnameVerifier hostnameVerifier;
    private final boolean enableHttp;
    private final boolean enableHttps;
    private final Duration connectionTimeout;
    private SSLContext sslContext;

    @Inject
    JettyHttpServerRegistry(HttpUriBuilder uriBuilder,
                            CryptoConfigurator cryptoConfigurator,
                            @Nullable @Named(CryptoConfig.CRYPTO_SETTINGS) CryptoSettings cryptoSettings,
                            JettyHttpServerHandlerFactory jettyHttpServerHandlerFactory,
                            @Named(DpwsConfig.HTTP_GZIP_COMPRESSION) boolean enableGzipCompression,
                            @Named(DpwsConfig.HTTP_RESPONSE_COMPRESSION_MIN_SIZE) int minCompressionSize,
                            @Named(CryptoConfig.CRYPTO_TLS_ENABLED_VERSIONS) String[] tlsProtocols,
                            @Named(CryptoConfig.CRYPTO_TLS_ENABLED_CIPHERS) String[] enabledCiphers,
                            @Named(CryptoConfig.CRYPTO_DEVICE_HOSTNAME_VERIFIER) HostnameVerifier hostnameVerifier,
                            @Named(DpwsConfig.HTTPS_SUPPORT) boolean enableHttps,
                            @Named(DpwsConfig.HTTP_SUPPORT) boolean enableHttp,
                            @Named(DpwsConfig.HTTP_SERVER_CONNECTION_TIMEOUT) Duration connectionTimeout,
                            // TODO: Remove these for 2.0.0
                            @Named(DeviceConfig.SECURED_ENDPOINT) boolean legacyEnableHttps,
                            @Named(DeviceConfig.UNSECURED_ENDPOINT) boolean legacyEnableHttp,
                            CommunicationLog communicationLog) {
        this.uriBuilder = uriBuilder;
        this.jettyHttpServerHandlerFactory = jettyHttpServerHandlerFactory;
        this.enableGzipCompression = enableGzipCompression;
        this.minCompressionSize = minCompressionSize;
        this.tlsProtocols = tlsProtocols;
        this.enabledCiphers = enabledCiphers;
        this.hostnameVerifier = hostnameVerifier;
        this.communicationLog = communicationLog;
        this.enableHttps = enableHttps | legacyEnableHttps;
        this.enableHttp = enableHttp | legacyEnableHttp;
        this.connectionTimeout = connectionTimeout;
        serverRegistry = new HashMap<>();
        handlerRegistry = new HashMap<>();
        contextHandlerMap = new HashMap<>();
        contextWrapperRegistry = new HashMap<>();
        registryLock = new ReentrantLock();
        configureSsl(cryptoConfigurator, cryptoSettings);

        if (!this.enableHttp && !this.enableHttps) {
            throw new RuntimeException("Http and https are disabled, cannot continue");
        }

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

    // TODO: 2.0.0 - return all created URIs, i.e. http and https
    @Override
    public String initHttpServer(String schemeAndAuthority) {
        registryLock.lock();
        try {
            var server = makeHttpServer(schemeAndAuthority);
            var uriString = server.getURI().toString();
            if (uriString.endsWith("/")) {
                uriString = uriString.substring(0, uriString.length() - 1);
            }
            var serverUri = URI.create(uriString);
            var requestedUri = URI.create(schemeAndAuthority);
            if (!serverUri.getScheme().equals(requestedUri.getScheme())) {
                try {
                    serverUri = replaceScheme(serverUri, requestedUri.getScheme());
                } catch (URISyntaxException e) {
                    LOG.error("Unexpected error while creating server uri return value: {}", e.getMessage());
                    LOG.trace("Unexpected error while creating server uri return value", e);
                }
            }
            return serverUri.toString();
        } finally {
            registryLock.unlock();
        }
    }

    // TODO: 2.0.0 - return all created URIs, i.e. http and https
    @Override
    public String registerContext(String schemeAndAuthority, String contextPath, HttpHandler handler) {
        return registerContext(schemeAndAuthority, contextPath, SoapConstants.MEDIA_TYPE_SOAP, handler);
    }

    // TODO: 2.0.0 - return all created URIs, i.e. http and https
    @Override
    public String registerContext(String schemeAndAuthority, String contextPath, String mediaType, HttpHandler handler) {
        if (!contextPath.startsWith("/")) {
            throw new RuntimeException(String.format("Context path needs to start with a slash, but is %s",
                    contextPath));
        }

        registryLock.lock();
        try {
            Server server = makeHttpServer(schemeAndAuthority);
            String mapKey;
            try {
                mapKey = makeMapKey(server.getURI().toString(), contextPath);
            } catch (UnknownHostException e) {
                LOG.error("Unexpected URI conversion error", e);
                throw new RuntimeException("Unexpected URI conversion error");
            }
            URI mapKeyUri = URI.create(mapKey);

            JettyHttpServerHandler endpointHandler = this.jettyHttpServerHandlerFactory.create(mediaType, handler);

            ContextHandler context = new ContextHandler(contextPath);
            context.setHandler(endpointHandler);
            context.setAllowNullPathInfo(true);

            this.handlerRegistry.put(mapKeyUri.toString(), endpointHandler);
            this.contextWrapperRegistry.put(contextPath, context);

            ContextHandlerCollection contextHandler = this.contextHandlerMap.get(server);
            contextHandler.addHandler(context);

            context.start();

            // use requested scheme for response
            var contextUri = replaceScheme(mapKeyUri, URI.create(schemeAndAuthority).getScheme());
            return contextUri.toString();
        } catch (Exception e) {
            LOG.error("Registering context {} failed.", contextPath, e);
            throw new RuntimeException(e);
        } finally {
            registryLock.unlock();
        }
    }

    @Override
    public void unregisterContext(String schemeAndAuthority, String contextPath) {
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

    private Server makeHttpServer(String uri) {
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
        Server httpServer = createHttpServer(URI.create(uri));
        try {
            httpServer.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        var serverUri = httpServer.getURI().toString();
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
        if (!isSupportedScheme(uri)) {
            throw new RuntimeException(String.format("HTTP server setup failed. Unsupported scheme: %s", uri.getScheme()));

        }
        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setSecureScheme(HttpScheme.HTTPS.asString());

        var server = new Server(new InetSocketAddress(
                uri.getHost(),
                uri.getPort()));

        ContextHandlerCollection context = new ContextHandlerCollection();
        server.setHandler(context);
        this.contextHandlerMap.put(server, context);

        CommunicationLogHandlerWrapper commlogHandler = new CommunicationLogHandlerWrapper(communicationLog, sslContext != null);
        commlogHandler.setHandler(server.getHandler());
        server.setHandler(commlogHandler);

        // wrap the context handler in a gzip handler
        if (this.enableGzipCompression) {
            GzipHandler gzipHandler = new GzipHandler();
            gzipHandler.setIncludedMethods(
                    HttpMethod.PUT.asString(),
                    HttpMethod.POST.asString(),
                    HttpMethod.GET.asString()
            );
            gzipHandler.setInflateBufferSize(2048);
            gzipHandler.setHandler(server.getHandler());
            gzipHandler.setMinGzipSize(minCompressionSize);
            gzipHandler.setIncludedMimeTypes(
                    "text/plain", "text/html",
                    SoapConstants.MEDIA_TYPE_SOAP, SoapConstants.MEDIA_TYPE_WSDL
            );
            server.setHandler(gzipHandler);
        }

        if (sslContext != null && enableHttps) {
            SslContextFactory.Server contextFactory = new SslContextFactory.Server();
            contextFactory.setSslContext(sslContext);
            contextFactory.setNeedClientAuth(true);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Enabled protocols: {}", List.of(tlsProtocols));
            }
            // reset excluded protocols to force only included protocols
            contextFactory.setExcludeProtocols();
            contextFactory.setIncludeProtocols(tlsProtocols);

            // reset excluded ciphers to force only included protocols
            contextFactory.setExcludeCipherSuites();
            contextFactory.setIncludeCipherSuites(enabledCiphers);

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

            var connectionFactory = new SslConnectionFactory(contextFactory, HttpVersion.HTTP_1_1.asString());
            ServerConnector httpsConnector;
            if (enableHttp) {
                httpsConnector = new ServerConnector(server,
                        new OptionalSslConnectionFactory(connectionFactory, HttpVersion.HTTP_1_1.asString()),
                        connectionFactory,
                        new HttpConnectionFactory(httpsConfig));
            } else {
                httpsConnector = new ServerConnector(server,
                        connectionFactory,
                        new HttpConnectionFactory(httpsConfig));
            }
            httpsConnector.setIdleTimeout(connectionTimeout.toMillis());
            httpsConnector.setHost(uri.getHost());
            httpsConnector.setPort(uri.getPort());

            server.setConnectors(new Connector[]{httpsConnector});
        }

        return server;
    }


    /*
     * Calculate http server map key:
     * - scheme is replaced by httpx to compare entries independent of used scheme
     * - host address is used instead of DNS name.
     *
     * throws UnknownHostException if host address cannot be resolved.
     */
    private String makeMapKey(String uri) throws UnknownHostException {
        URI parsedUri = URI.create(uri);
        InetAddress address = InetAddress.getByName(parsedUri.getHost());
        return uriBuilder.buildUri("httpx", address.getHostAddress(), parsedUri.getPort());
    }

    private String makeMapKey(String uri, String contextPath) throws UnknownHostException {
        return makeMapKey(uri) + contextPath;
    }

    private URI replaceScheme(URI baseUri, String scheme) throws URISyntaxException {
        return new URI(scheme, baseUri.getUserInfo(),
                baseUri.getHost(), baseUri.getPort(),
                baseUri.getPath(), baseUri.getQuery(),
                baseUri.getFragment());
    }

    private boolean isSupportedScheme(URI address) {
        return (enableHttp && HttpScheme.HTTP.asString().toLowerCase()
                .equals(address.getScheme().toLowerCase()))
                || (enableHttps && HttpScheme.HTTPS.asString().toLowerCase()
                .equals(address.getScheme().toLowerCase()));
    }
}
