package org.somda.sdc.dpws.soap;

import com.google.inject.AbstractModule;
import org.somda.sdc.common.guice.AbstractConfigurationModule;
import org.somda.sdc.dpws.DpwsTest;
import org.somda.sdc.dpws.NetworkSinkMock;
import org.somda.sdc.dpws.soap.factory.EnvelopeFactory;
import org.somda.sdc.dpws.soap.factory.SoapFaultFactory;
import org.somda.sdc.dpws.soap.model.Envelope;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryConstants;
import org.somda.sdc.dpws.soap.wsdiscovery.model.HelloType;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ObjectFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class JaxbSoapMarshallingTest extends DpwsTest {
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        overrideBindings(new AbstractConfigurationModule() {
            @Override
            protected void defaultConfigure() {
                // no need for valiation as tests to test general marshalling
                bind(SoapConfig.VALIDATE_SOAP_MESSAGES,
                        Boolean.class,
                        false);
            }
        });
        super.setUp();
        getInjector().getInstance(SoapMarshalling.class).startAsync().awaitRunning();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void marshallCompositeSoapMessage() throws JAXBException, IOException {
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
    public void marshalFault() throws Exception {
        SoapFaultFactory sff = getInjector().getInstance(SoapFaultFactory.class);
        SoapMessage faultMsg = sff.createReceiverFault("Test");
        SoapMarshalling marshalling = getInjector().getInstance(SoapMarshalling.class);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Envelope envelopeWithMappedHeaders = faultMsg.getEnvelopeWithMappedHeaders();
        marshalling.marshal(envelopeWithMappedHeaders, bos);
        System.out.println(bos.toString());
        assertTrue(true);
    }
}
