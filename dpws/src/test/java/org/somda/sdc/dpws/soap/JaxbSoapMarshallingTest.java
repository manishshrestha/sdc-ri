package org.somda.sdc.dpws.soap;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.somda.sdc.common.guice.AbstractConfigurationModule;
import org.somda.sdc.common.guice.DefaultCommonConfigModule;
import org.somda.sdc.common.guice.DefaultHelperModule;
import org.somda.sdc.dpws.DpwsTest;
import org.somda.sdc.dpws.NetworkSinkMock;
import org.somda.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.somda.sdc.dpws.guice.DefaultDpwsModule;
import org.somda.sdc.dpws.helper.JaxbMarshalling;
import org.somda.sdc.dpws.soap.factory.ContentHandlerProxyFactory;
import org.somda.sdc.dpws.soap.factory.EnvelopeFactory;
import org.somda.sdc.dpws.soap.factory.SoapFaultFactory;
import org.somda.sdc.dpws.soap.model.Envelope;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryConstants;
import org.somda.sdc.dpws.soap.wsdiscovery.model.HelloType;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ObjectFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JaxbSoapMarshallingTest extends DpwsTest {
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        overrideBindings(new AbstractConfigurationModule() {
            @Override
            protected void defaultConfigure() {
                // no need for validation as tests to test general marshalling
                bind(SoapConfig.VALIDATE_SOAP_MESSAGES,
                        Boolean.class,
                        false);
            }
        });
        super.setUp();
        getInjector().getInstance(JaxbMarshalling.class).startAsync().awaitRunning();
        getInjector().getInstance(SoapMarshalling.class).startAsync().awaitRunning();
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Round trip test for marshalling and unmarshalling a Hello message")
    void marshalAndUnmatshal() throws JAXBException, IOException {
        ObjectFactory wsdFactory = getInjector().getInstance(ObjectFactory.class);
        HelloType helloType = wsdFactory.createHelloType();
        List<String> xAddrs = new ArrayList<>();
        xAddrs.add("http://test-xAddr1");
        xAddrs.add("http://test-xAddr2");
        helloType.setXAddrs(xAddrs);
        JAXBElement<HelloType> expectedHello = wsdFactory.createHello(helloType);

        EnvelopeFactory envelopeFactory = getInjector().getInstance(EnvelopeFactory.class);
        Envelope expectedEnvelope = envelopeFactory.createEnvelope(WsDiscoveryConstants.WSA_ACTION_HELLO,
                WsDiscoveryConstants.WSA_UDP_TO, expectedHello);

        SoapMarshalling marshalling = getInjector().getInstance(SoapMarshalling.class);
        NetworkSinkMock mockNetworkSink = getInjector().getInstance(NetworkSinkMock.class);

        try (OutputStream os = mockNetworkSink.createOutputStream()) {
            marshalling.marshal(expectedEnvelope, os);
        }

        try (InputStream inputStreams = mockNetworkSink.getLatest()) {
            Envelope actualEnvelope = marshalling.unmarshal(inputStreams);
            assertEquals(1, actualEnvelope.getBody().getAny().size());

            JAXBElement<HelloType> actualHello = (JAXBElement<HelloType>) actualEnvelope.getBody().getAny().get(0);
            assertThat(expectedHello.getValue().getXAddrs(), is(actualHello.getValue().getXAddrs()));
        }
    }

    @Test
    @DisplayName("SOAP fault marshalling shall not throw an exception")
    void marshalFault() {
        assertDoesNotThrow(() -> {
            SoapFaultFactory sff = getInjector().getInstance(SoapFaultFactory.class);
            SoapMessage faultMsg = sff.createReceiverFault("Test");
            SoapMarshalling marshalling = getInjector().getInstance(SoapMarshalling.class);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            Envelope envelopeWithMappedHeaders = faultMsg.getEnvelopeWithMappedHeaders();
            marshalling.marshal(envelopeWithMappedHeaders, bos);
        });
    }

    @Test
    @DisplayName("A marshalled message created using SoapUtil shall contain the Action element only once")
    void testDuplicateHeaders() throws Exception {
        var action = "ftp://somda.org/upload";
        var soapUtil = getInjector().getInstance(SoapUtil.class);
        SoapMarshalling marshalling = getInjector().getInstance(SoapMarshalling.class);

        var message = soapUtil.createMessage(action);

        var messageBaos = new ByteArrayOutputStream();
        marshalling.marshal(message.getEnvelopeWithMappedHeaders(), messageBaos);

        var messageString = messageBaos.toString(StandardCharsets.UTF_8);

        // one opening and one closing tag
        assertEquals(2, StringUtils.countMatches(messageString, ":Action>"));
    }

    @Test
    @DisplayName("A ContentHandlerProxyFactory shall be applied by JaxbSoapMarshalling if Guice binding exists")
    void testContentHandlerProxyFactoryApplicability() throws Exception {
        var documentStarted = new AtomicBoolean(false);
        var proxyFactory = new MyContentHandlerProxyFactory(documentStarted);
        var injector = Guice.createInjector(
                new DefaultCommonConfigModule(), new DefaultDpwsModule(),
                new DefaultHelperModule(), new DefaultDpwsConfigModule(), new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(ContentHandlerProxyFactory.class).toInstance(proxyFactory);
                    }
                }
        );

        var jaxbMarshalling = injector.getInstance(JaxbMarshalling.class);
        jaxbMarshalling.startAsync().awaitRunning();

        try (InputStream inputStream = getClass().getResourceAsStream("soap-envelope.xml")) {
            assertFalse(documentStarted.get());
            jaxbMarshalling.unmarshal(inputStream);
            assertTrue(documentStarted.get());
        }
    }

    private class MyContentHandlerProxyFactory implements ContentHandlerProxyFactory {
        private final AtomicBoolean documentStarted;

        MyContentHandlerProxyFactory(AtomicBoolean documentStarted) {
            this.documentStarted = documentStarted;
        }

        @Override
        public ContentHandler createContentHandlerProxy(ContentHandler targetHandler) {
            return new MyContentHandlerProxy(targetHandler, documentStarted);
        }
    }

    private class MyContentHandlerProxy extends ContentHandlerAdapter {
        private AtomicBoolean documentStarted;

        MyContentHandlerProxy(ContentHandler targetHandler, AtomicBoolean documentStarted) {
            super(targetHandler);
            this.documentStarted = documentStarted;
            this.documentStarted.set(false);
        }

        @Override
        public void startDocument() throws SAXException {
            // It is assumed once the startDocument method has been called, that other SAX methods are also being called
            this.documentStarted.set(true);
            getTargetHandler().startDocument();
        }
    }
}
