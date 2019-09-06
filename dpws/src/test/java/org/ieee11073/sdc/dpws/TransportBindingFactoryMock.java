package org.ieee11073.sdc.dpws;

import com.google.inject.Inject;
import org.ieee11073.sdc.dpws.factory.TransportBindingFactory;
import org.ieee11073.sdc.dpws.http.HttpHandler;
import org.ieee11073.sdc.dpws.soap.SoapMarshalling;
import org.ieee11073.sdc.dpws.soap.SoapMessage;
import org.ieee11073.sdc.dpws.soap.TransportInfo;
import org.ieee11073.sdc.dpws.soap.exception.MarshallingException;
import org.ieee11073.sdc.dpws.soap.exception.TransportException;
import org.ieee11073.sdc.dpws.soap.factory.SoapMessageFactory;
import org.ieee11073.sdc.dpws.soap.model.Envelope;

import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TransportBindingFactoryMock implements TransportBindingFactory {
    private static Map<URI, HttpHandler> handlerRegistry;
    private final SoapMarshalling soapMarshalling;
    private final SoapMessageFactory soapMessageFactory;
    private final TransportInfo mockTransportInfo;

    @Inject
    TransportBindingFactoryMock(SoapMarshalling soapMarshalling,
                                SoapMessageFactory soapMessageFactory) {
        this.soapMarshalling = soapMarshalling;
        this.soapMessageFactory = soapMessageFactory;
        this.mockTransportInfo = new TransportInfo("mock.scheme", "localhost", 123);
    }

    public static void setHandlerRegistry(Map<URI, HttpHandler> handlerRegistry) {
        TransportBindingFactoryMock.handlerRegistry = handlerRegistry;
    }

    @Override
    public TransportBinding createTransportBinding(URI endpointUri) {
        if (handlerRegistry == null) {
            handlerRegistry = new HashMap<>();
        }
        Optional<HttpHandler> httpHandler = Optional.ofNullable(handlerRegistry.get(endpointUri));

        return new TransportBinding() {
            @Override
            public void close() throws IOException {
            }

            @Override
            public void onNotification(SoapMessage notification) throws MarshallingException {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                try {
                    soapMarshalling.marshal(notification.getEnvelopeWithMappedHeaders(), bos);
                    if (httpHandler.isPresent()) {
                        httpHandler.get().process(new ByteArrayInputStream(bos.toByteArray()),
                                new ByteArrayOutputStream(), mockTransportInfo);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public SoapMessage onRequestResponse(SoapMessage request) throws MarshallingException, TransportException {
                ByteArrayOutputStream bosRequest = new ByteArrayOutputStream();

                try {
                    soapMarshalling.marshal(request.getEnvelopeWithMappedHeaders(), bosRequest);
                } catch (JAXBException e) {
                    throw new MarshallingException(e);
                }

                ByteArrayOutputStream bosResponse = new ByteArrayOutputStream();
                HttpHandler theHttpHandler = httpHandler.orElseThrow(() -> new TransportException("HTTP handler not set."));
                theHttpHandler.process(new ByteArrayInputStream(bosRequest.toByteArray()), bosResponse, mockTransportInfo);

                try {
                    Envelope env = soapMarshalling.unmarshal(new ByteArrayInputStream(bosResponse.toByteArray()));
                    return soapMessageFactory.createSoapMessage(env);
                } catch (JAXBException e) {
                    throw new MarshallingException(e);
                }
            }
        };
    }

    @Override
    public TransportBinding createHttpBinding(URI endpointUri) {
        return createTransportBinding(endpointUri);
    }
}
