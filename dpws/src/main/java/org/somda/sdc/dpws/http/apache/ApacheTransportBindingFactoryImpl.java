package org.somda.sdc.dpws.http.apache;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.DpwsConfig;
import org.somda.sdc.dpws.DpwsConstants;
import org.somda.sdc.dpws.TransportBinding;
import org.somda.sdc.dpws.crypto.CryptoConfig;
import org.somda.sdc.dpws.crypto.CryptoConfigurator;
import org.somda.sdc.dpws.crypto.CryptoSettings;
import org.somda.sdc.dpws.factory.TransportBindingFactory;
import org.somda.sdc.dpws.soap.SoapMarshalling;
import org.somda.sdc.dpws.soap.SoapUtil;

import javax.annotation.Nullable;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.net.URI;
import java.time.Duration;

public class ApacheTransportBindingFactoryImpl implements TransportBindingFactory {

    private static final Logger LOG = LoggerFactory.getLogger(TransportBinding.class);

    private static final String SCHEME_SOAP_OVER_UDP = DpwsConstants.URI_SCHEME_SOAP_OVER_UDP;
    private static final String SCHEME_HTTP = "http";
    private static final String SCHEME_HTTPS = "https";

    private final SoapMarshalling marshalling;
    private final SoapUtil soapUtil;
    private final boolean enableGzipCompression;
    private final Duration clientConnectTimeout;
    private final Duration clientReadTimeout;

    private final HttpClient client;

    private final String[] tlsProtocols;
    private final HostnameVerifier hostnameVerifier;
    private final String[] enabledCiphers;
    private HttpClient securedClient; // if null => no cryptography configured/enabled

    private ClientTransportBindingFactory clientTransportBindingFactory;
    private final CommunicationLog communicationLog;

    @Inject
    ApacheTransportBindingFactoryImpl(SoapMarshalling marshalling, SoapUtil soapUtil,
                                      CryptoConfigurator cryptoConfigurator,
                                      @Nullable @Named(CryptoConfig.CRYPTO_SETTINGS) CryptoSettings cryptoSettings,
                                      @Named(DpwsConfig.HTTP_CLIENT_CONNECT_TIMEOUT) Duration clientConnectTimeout,
                                      @Named(DpwsConfig.HTTP_CLIENT_READ_TIMEOUT) Duration clientReadTimeout,
                                      @Named(DpwsConfig.HTTP_GZIP_COMPRESSION) boolean enableGzipCompression,
                                      ClientTransportBindingFactory clientTransportBindingFactory,
                                      @Named(CryptoConfig.CRYPTO_TLS_ENABLED_VERSIONS) String[] tlsProtocols,
                                      @Named(CryptoConfig.CRYPTO_TLS_ENABLED_CIPHERS) String[] enabledCiphers,
                                      @Named(CryptoConfig.CRYPTO_CLIENT_HOSTNAME_VERIFIER) HostnameVerifier hostnameVerifier,
                                      CommunicationLog communicationLog) {
        this.marshalling = marshalling;
        this.soapUtil = soapUtil;
        this.clientConnectTimeout = clientConnectTimeout;
        this.clientReadTimeout = clientReadTimeout;
        this.enableGzipCompression = enableGzipCompression;
        this.clientTransportBindingFactory = clientTransportBindingFactory;
        this.communicationLog = communicationLog;
        this.client = buildBaseClient().build();
        this.tlsProtocols = tlsProtocols;
        this.enabledCiphers = enabledCiphers;
        this.hostnameVerifier = hostnameVerifier;

        configureSecuredClient(cryptoConfigurator, cryptoSettings);
    }

    private HttpClientBuilder buildBaseClient() {
        var socketConfig = SocketConfig.custom().setTcpNoDelay(true).build();

        // set the timeout for all requests
        var requestConfig = RequestConfig.custom().setConnectionRequestTimeout((int) clientReadTimeout.toMillis())
                .setConnectTimeout((int) clientConnectTimeout.toMillis())
                .setSocketTimeout((int) clientConnectTimeout.toMillis()).build();

        var clientBuilder = HttpClients.custom().setDefaultSocketConfig(socketConfig)
                // attach interceptors to enable communication log capabilities including message headers
                .addInterceptorLast(new CommunicationLogHttpRequestInterceptor(communicationLog))
                .addInterceptorLast(new CommunicationLogHttpResponseInterceptor(communicationLog))
                .setDefaultRequestConfig(requestConfig)
                // only allow one connection per host
                .setMaxConnPerRoute(1)
                // allow reusing ssl connections in the pool
                .disableConnectionState()
                // retry every request just once in case the socket has died
                .setRetryHandler(new DefaultHttpRequestRetryHandler(1, false));
        if (!enableGzipCompression) {
            // disable gzip compression
            clientBuilder.disableContentCompression();

        }
        return clientBuilder;
    }

    private void configureSecuredClient(CryptoConfigurator cryptoConfigurator,
            @Nullable CryptoSettings cryptoSettings) {
        if (cryptoSettings == null) {
            securedClient = null;
            return;
        }

        SSLContext sslContext;
        try {
            sslContext = cryptoConfigurator.createSslContextFromCryptoConfig(cryptoSettings);
        } catch (Exception e) {
            LOG.error("Could not read client crypto config, fallback to system properties", e);
            sslContext = cryptoConfigurator.createSslContextFromSystemProperties();
        }

        SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
                sslContext,
                tlsProtocols,
                enabledCiphers,
                hostnameVerifier
        );

        this.securedClient = buildBaseClient()
                .setSSLContext(sslContext)
                .setSSLSocketFactory(socketFactory)
                .build();
    }

    @Override
    public TransportBinding createTransportBinding(String endpointUri) throws UnsupportedOperationException {
        // To keep things simple, this method directly checks if there is a SOAP-UDP or
        // HTTP(S) binding
        // No plug-and-play feature is implemented that dispatches, based on the URI
        // scheme, to endpoint processor
        // factories

        String scheme = URI.create(endpointUri).getScheme();
        if (scheme.equalsIgnoreCase(SCHEME_SOAP_OVER_UDP)) {
            throw new UnsupportedOperationException(
                    "SOAP-over-UDP is currently not supported by the TransportBindingFactory");
        } else if (scheme.equalsIgnoreCase(SCHEME_HTTP)) {
            return createHttpBinding(endpointUri);
        } else if (scheme.equalsIgnoreCase(SCHEME_HTTPS)) {
            return createHttpBinding(endpointUri);
        } else {
            throw new UnsupportedOperationException(
                    String.format("Unsupported transport binding requested: %s", scheme));
        }
    }

    @Override
    public TransportBinding createHttpBinding(String endpointUri) throws UnsupportedOperationException {
        var scheme = URI.create(endpointUri).getScheme();
        if (client != null && scheme.equalsIgnoreCase("http")) {
            return this.clientTransportBindingFactory.create(client, endpointUri, marshalling, soapUtil);
        }
        if (securedClient != null && scheme.equalsIgnoreCase("https")) {
            return this.clientTransportBindingFactory.create(securedClient, endpointUri, marshalling, soapUtil);
        }

        throw new UnsupportedOperationException(
                String.format("Binding with scheme %s is currently not supported", scheme));
    }

}
