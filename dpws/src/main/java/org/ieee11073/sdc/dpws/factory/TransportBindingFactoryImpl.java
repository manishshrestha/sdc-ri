package org.ieee11073.sdc.dpws.factory;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.glassfish.jersey.SslConfigurator;
import org.ieee11073.sdc.dpws.TransportBinding;
import org.ieee11073.sdc.dpws.TransportBindingException;
import org.ieee11073.sdc.dpws.crypto.CryptoConfig;
import org.ieee11073.sdc.dpws.crypto.CryptoSettings;
import org.ieee11073.sdc.dpws.crypto.CryptoConfigurator;
import org.ieee11073.sdc.dpws.soap.SoapConstants;
import org.ieee11073.sdc.dpws.soap.SoapMarshalling;
import org.ieee11073.sdc.dpws.soap.SoapMessage;
import org.ieee11073.sdc.dpws.soap.SoapUtil;
import org.ieee11073.sdc.dpws.soap.exception.SoapFaultException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

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

    private final Client client;
    private Client securedClient; // if null => no cryptography configured/enabled

    @Inject
    TransportBindingFactoryImpl(SoapMarshalling marshalling,
                                SoapUtil soapUtil,
                                CryptoConfigurator cryptoConfigurator,
                                @Nullable @Named(CryptoConfig.CRYPTO_SETTINGS) CryptoSettings cryptoSettings) {
        this.marshalling = marshalling;
        this.soapUtil = soapUtil;
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
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            try {
                marshalling.marshal(request.getEnvelopeWithMappedHeaders(), os);
            } catch (JAXBException e) {
                LOG.warn("Marshalling of a message failed: {}", e.getMessage());
                e.printStackTrace();
                throw new TransportBindingException(
                        String.format("Sending of a request failed due to marshalling problem: %s", e.getMessage()));
            }

            Response response = remoteEndpoint
                    .request(SoapConstants.MEDIA_TYPE_SOAP)
                    .accept(SoapConstants.MEDIA_TYPE_SOAP)
                    .post(Entity.entity(os.toString(), SoapConstants.MEDIA_TYPE_SOAP));

            if (response.getStatus() >= 300) {
                throw new TransportBindingException(
                        String.format("Endpoint was not able to process request. HTTP status code: %s",
                                response.getStatus()));
            }

            InputStream is = response.readEntity(InputStream.class);
            try {
                if (is.available() > 0) {
                    SoapMessage msg = soapUtil.createMessage(marshalling.unmarshal(is));
                    if (msg.isFault()) {
                        throw new SoapFaultException(msg);
                    }

                    return msg;
                }
            } catch (JAXBException e) {
                LOG.debug("Unmarshalling of a message failed: {}", e.getMessage());
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
