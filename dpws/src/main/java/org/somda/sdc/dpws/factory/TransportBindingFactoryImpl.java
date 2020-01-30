package org.somda.sdc.dpws.factory;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.glassfish.jersey.SslConfigurator;
import org.glassfish.jersey.client.ClientProperties;
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
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;

/**
 * Default implementation of {@link TransportBindingFactory}.
 */
public class TransportBindingFactoryImpl implements TransportBindingFactory {
    private static final Logger LOG = LoggerFactory.getLogger(TransportBinding.class);

    private static final String SCHEME_SOAP_OVER_UDP = "soap.udp";
    private static final String SCHEME_HTTP = "http";
    private static final String SCHEME_HTTPS = "https";

    private final SoapMarshalling marshalling;
    private final SoapUtil soapUtil;
    private final CommunicationLog communicationLog;
    private Duration clientConnectTimeout;
    private Duration clientReadTimeout;

    private final Client client;
    private Client securedClient; // if null => no cryptography configured/enabled

    @Inject
    TransportBindingFactoryImpl(SoapMarshalling marshalling,
                                SoapUtil soapUtil,
                                CryptoConfigurator cryptoConfigurator,
                                @Nullable @Named(CryptoConfig.CRYPTO_SETTINGS) CryptoSettings cryptoSettings,
                                CommunicationLog communicationLog,
                                @Named (DpwsConfig.HTTP_CLIENT_CONNECT_TIMEOUT) Duration clientConnectTimeout,
                                @Named (DpwsConfig.HTTP_CLIENT_READ_TIMEOUT) Duration clientReadTimeout) {
        this.marshalling = marshalling;
        this.soapUtil = soapUtil;
        this.communicationLog = communicationLog;
        this.clientConnectTimeout = clientConnectTimeout;
        this.clientReadTimeout = clientReadTimeout;
        this.client = ClientBuilder.newClient();

        configureSecuredClient(cryptoConfigurator, cryptoSettings);
    }

    private void configureSecuredClient(CryptoConfigurator cryptoConfigurator,
                                        @Nullable CryptoSettings cryptoSettings) {
        if (cryptoSettings == null) {
            securedClient = null;
            return;
        }

        SslConfigurator sslConfigurator;
        try {
            sslConfigurator = cryptoConfigurator.createSslConfiguratorFromCryptoConfig(cryptoSettings);
        } catch (IllegalArgumentException e) {
            LOG.warn("Could not read client crypto config, fallback to system properties");
            sslConfigurator = cryptoConfigurator.createSslConfiguratorFromSystemProperties();
        }

        this.securedClient = ClientBuilder.newBuilder()
                .sslContext(sslConfigurator.createSSLContext())
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
        private final WebTarget remoteEndpoint;
        private final SoapMarshalling marshalling;
        private final SoapUtil soapUtil;

        HttpClientTransportBinding(Client client,
                                   URI clientUri,
                                   SoapMarshalling marshalling,
                                   SoapUtil soapUtil) {
            //Client client = ClientBuilder.newClient();
            this.remoteEndpoint = client.target(clientUri);
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
                        remoteEndpoint.getUri().getHost(), remoteEndpoint.getUri().getPort(), outputStream.toByteArray());
            }

            Response response = remoteEndpoint
                    .request(SoapConstants.MEDIA_TYPE_SOAP)
                    .accept(SoapConstants.MEDIA_TYPE_SOAP)
                    .property(ClientProperties.CONNECT_TIMEOUT, (int) clientConnectTimeout.toMillis())
                    .property(ClientProperties.READ_TIMEOUT, (int) clientReadTimeout.toMillis())
                    .post(Entity.entity(outputStream.toString(), SoapConstants.MEDIA_TYPE_SOAP));

            if (response.getStatus() >= 300) {
                throw new TransportBindingException(
                        String.format("Endpoint was not able to process request. HTTP status code: %s",
                                response.getStatus()));
            }

            InputStream inputStream = response.readEntity(InputStream.class);

            // TODO: This is a workaround for some odd behavior encountered when communicating with
            //  pysdc using an encrypted connection. It turns out, inputStream.available() is quite unreliable
            //  and might be 0 while still having incoming content. But in case it actually is zero, we don't
            //  want to pass the stream to JAXB to fail hard. The correct way to determine whether there is
            //  data remaining in the stream would be to just read the stream until it's done,
            //  so this is what we're doing here.
            try {
                inputStream = new ByteArrayInputStream(inputStream.readAllBytes());
            } catch (IOException e) {
                LOG.error("Could not copy incoming data into new input stream", e);
            }

            if (LOG.isDebugEnabled()) {
                inputStream = communicationLog.logHttpMessage(CommunicationLogImpl.HttpDirection.OUTBOUND_RESPONSE,
                        remoteEndpoint.getUri().getHost(), remoteEndpoint.getUri().getPort(), inputStream);
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
                LOG.trace("Unmarshalling of a message failed", e);
                throw new TransportBindingException(
                        String.format("Receiving of a response failed due to unmarshalling problem: %s",
                                e.getMessage()));
            } catch (IOException e) {
                LOG.debug("IOException: {}", e.getMessage());
            }

            return soapUtil.createMessage();
        }

        @Override
        public void close() {
            // no action on HTTP
        }
    }
}
