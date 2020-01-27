package org.somda.sdc.dpws.factory;

import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.dpws.CommunicationLog;
import org.somda.sdc.dpws.CommunicationLogImpl;
import org.somda.sdc.dpws.DpwsConfig;
import org.somda.sdc.dpws.TransportBinding;
import org.somda.sdc.dpws.TransportBindingException;
import org.somda.sdc.dpws.crypto.CryptoConfig;
import org.somda.sdc.dpws.crypto.CryptoConfigurator;
import org.somda.sdc.dpws.crypto.CryptoSettings;
import org.somda.sdc.dpws.soap.SoapConstants;
import org.somda.sdc.dpws.soap.SoapMarshalling;
import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.SoapUtil;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;

import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.URI;
import java.time.Duration;

public class ApacheTransportBindingFactoryImpl implements TransportBindingFactory {

    private static final Logger LOG = LoggerFactory.getLogger(TransportBinding.class);

    private static final String SCHEME_SOAP_OVER_UDP = "soap.udp";
    private static final String SCHEME_HTTP = "http";
    private static final String SCHEME_HTTPS = "https";

    private final SoapMarshalling marshalling;
    private final SoapUtil soapUtil;
    private Duration clientConnectTimeout;
    private Duration clientReadTimeout;
    private final CommunicationLog communicationLog;

    private final HttpClient client;
    private HttpClient securedClient; // if null => no cryptography configured/enabled

    @Inject
    ApacheTransportBindingFactoryImpl(SoapMarshalling marshalling,
                                      SoapUtil soapUtil,
                                      CryptoConfigurator cryptoConfigurator,
                                      @Nullable @Named(CryptoConfig.CRYPTO_SETTINGS) CryptoSettings cryptoSettings,
                                      CommunicationLog communicationLog,
                                      @Named(DpwsConfig.HTTP_CLIENT_CONNECT_TIMEOUT) Duration clientConnectTimeout,
                                      @Named(DpwsConfig.HTTP_CLIENT_READ_TIMEOUT) Duration clientReadTimeout) {
        this.marshalling = marshalling;
        this.soapUtil = soapUtil;
        this.clientConnectTimeout = clientConnectTimeout;
        this.clientReadTimeout = clientReadTimeout;
        this.communicationLog = communicationLog;
        this.client = buildBaseClient().build();

        configureSecuredClient(cryptoConfigurator, cryptoSettings);
    }

    private HttpClientBuilder buildBaseClient() {
        var socketConfig = SocketConfig.custom()
                .setTcpNoDelay(true)
                .build();

        // set the timeout for all requests
        var requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout((int) clientReadTimeout.toMillis())
                .setConnectTimeout((int) clientConnectTimeout.toMillis())
                .setSocketTimeout((int) clientConnectTimeout.toMillis())
                .build();

        return HttpClients.custom()
                .setDefaultSocketConfig(socketConfig)
                .setDefaultRequestConfig(requestConfig)
                // only allow one connection per host
                .setMaxConnPerRoute(1)
                // allow reusing ssl connections in the pool
                .disableConnectionState()
                // retry every request just once in case the socket has died
                .setRetryHandler(new DefaultHttpRequestRetryHandler(1, false))
                // disable gzip compression for now
                .disableContentCompression();
    }

    private void configureSecuredClient(CryptoConfigurator cryptoConfigurator, @Nullable CryptoSettings cryptoSettings) {
        if (cryptoSettings == null) {
            securedClient = null;
            return;
        }

        SSLContext sslContext;
        try {
            sslContext = cryptoConfigurator.createSslConfiguratorFromCryptoConfig(cryptoSettings).createSSLContext();
        } catch (IllegalArgumentException e) {
            LOG.warn("Could not read client crypto config, fallback to system properties");
            sslContext = cryptoConfigurator.createSslConfiguratorFromSystemProperties().createSSLContext();
        }

        this.securedClient = buildBaseClient()
                .setSSLContext(sslContext)
                .setSSLHostnameVerifier((s, sslSession) -> true)
                .build();
    }

    @Override
    public TransportBinding createTransportBinding(URI endpointUri) throws UnsupportedOperationException {
        // To keep things simple, this method directly checks if there is a SOAP-UDP or HTTP(S) binding
        // No plug-and-play feature is implemented that dispatches, based on the URI scheme, to endpoint processor
        // factories

        String scheme = endpointUri.getScheme();
        if (scheme.equalsIgnoreCase(SCHEME_SOAP_OVER_UDP)) {
            throw new UnsupportedOperationException("SOAP-over-UDP is currently not supported by the TransportBindingFactory");
        } else if (scheme.equalsIgnoreCase(SCHEME_HTTP)) {
            return createHttpBinding(endpointUri);
        } else if (scheme.equalsIgnoreCase(SCHEME_HTTPS)) {
            return createHttpBinding(endpointUri);
        } else {
            throw new UnsupportedOperationException(String.format("Unsupported transport binding requested: %s", scheme));
        }
    }

    @Override
    public TransportBinding createHttpBinding(URI endpointUri) throws UnsupportedOperationException {
        if (client != null && endpointUri.getScheme().equalsIgnoreCase("http")) {
            return new HttpClientTransportBinding(client, endpointUri, marshalling, soapUtil);
        }
        if (securedClient != null && endpointUri.getScheme().equalsIgnoreCase("https")) {
            return new HttpClientTransportBinding(securedClient, endpointUri, marshalling, soapUtil);
        }

        throw new UnsupportedOperationException(String.format("Binding with scheme %s is currently not supported", endpointUri.getScheme()));
    }

    private class HttpClientTransportBinding implements TransportBinding {
        private final SoapMarshalling marshalling;
        private final SoapUtil soapUtil;
        private HttpClient client;
        private final URI clientUri;

        HttpClientTransportBinding(HttpClient client,
                                   URI clientUri,
                                   SoapMarshalling marshalling,
                                   SoapUtil soapUtil) {
            LOG.debug("Creating HttpClientTransportBinding for {}", clientUri);
            this.client = client;
            this.clientUri = clientUri;
            this.marshalling = marshalling;
            this.soapUtil = soapUtil;
        }

        @Override
        public void onNotification(SoapMessage notification) throws TransportBindingException {
            // Ignore the result even if there is one
            try {
                onRequestResponse(notification);
            } catch (SoapFaultException e) {
                // Swallow exception, rationale:
                // we assume that notifications have no response and therefore no soap exception that could be thrown
            }
        }

        @Override
        public SoapMessage onRequestResponse(SoapMessage request) throws TransportBindingException, SoapFaultException {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try {
                marshalling.marshal(request.getEnvelopeWithMappedHeaders(), outputStream);
            } catch (JAXBException e) {
                LOG.warn("Marshalling of a message failed: {}", e.getMessage());
                LOG.trace("Marshalling of a message failed", e);
                throw new TransportBindingException(
                        String.format("Sending of a request failed due to marshalling problem: %s", e.getMessage()));
            }

            if (LOG.isDebugEnabled()) {
                communicationLog.logHttpMessage(CommunicationLogImpl.HttpDirection.OUTBOUND_REQUEST,
                        this.clientUri.getHost(), this.clientUri.getPort(), outputStream.toByteArray());
            }

            // create post request and set content type to SOAP
            HttpPost post = new HttpPost(this.clientUri);
            post.setHeader("Accept", SoapConstants.MEDIA_TYPE_SOAP);
            post.setHeader("Content-type", SoapConstants.MEDIA_TYPE_SOAP);

            // attach payload
            var requestEntity = new ByteArrayEntity(outputStream.toByteArray());
            post.setEntity(requestEntity);

            LOG.debug("Sending POST request to {}", this.clientUri);
            HttpResponse response;

            try {
                // no retry handling is required as apache httpclient already does
                response = this.client.execute(post);
            } catch (SocketException e) {
                LOG.error("No response received in request to {}", this.clientUri, e);
                throw new TransportBindingException(e);
            } catch (IOException e) {
                LOG.error("Unexpected IO exception on request to {}", this.clientUri);
                throw new TransportBindingException("No response received");
            }

            if (response.getStatusLine().getStatusCode() >= 300) {
                throw new TransportBindingException(
                        String.format(
                                "Endpoint was not able to process request. HTTP status code: %s",
                                response.getStatusLine()
                        )
                );
            }

            HttpEntity entity = response.getEntity();
            InputStream inputStream;
            try {
                inputStream = entity.getContent();
                final byte[] bytes = ByteStreams.toByteArray(inputStream);
                inputStream = new ByteArrayInputStream(bytes);
            } catch (IOException e) {
                LOG.error("Couldn't read response", e);
                inputStream = new ByteArrayInputStream(new byte[0]);
            }
            // TODO: Do we really want to attach the communication log to a loglevel?
            if (LOG.isDebugEnabled()) {
                inputStream = communicationLog.logHttpMessage(
                        CommunicationLogImpl.HttpDirection.OUTBOUND_RESPONSE,
                        this.clientUri.getHost(),
                        this.clientUri.getPort(),
                        inputStream
                );
            }
            try {
                if (inputStream.available() > 0) {
                    SoapMessage msg = soapUtil.createMessage(marshalling.unmarshal(inputStream));
                    if (msg.isFault()) {
                        throw new SoapFaultException(msg);
                    }

                    return msg;
                }
            } catch (JAXBException e) {
                LOG.debug("Unmarshalling of a message failed: {}", e.getMessage());
                LOG.trace("Unmarshalling of a message failed.", e);
                throw new TransportBindingException(
                        String.format(
                                "Receiving of a response failed due to unmarshalling problem: %s",
                                e.getMessage()
                        )
                );
            } catch (IOException e) {
                LOG.debug("Error occurred while processing response: {}", e.getMessage());
                LOG.trace("Error occurred while processing response", e);
            } finally {
                try {
                    // ensure the entire response was consumed, just in case
                    EntityUtils.consume(response.getEntity());
                } catch (IOException e) {
                    // if this fails, we will either all die or it doesn't matter at all...
                }
            }

            return soapUtil.createMessage();
        }

        @Override
        public void close() {
            // no action on HTTP
        }
    }


}
