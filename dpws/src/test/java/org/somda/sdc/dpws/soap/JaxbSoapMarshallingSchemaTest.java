package org.somda.sdc.dpws.soap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.somda.sdc.common.guice.AbstractConfigurationModule;
import org.somda.sdc.dpws.DpwsTest;
import org.somda.sdc.dpws.NetworkSinkMock;
import org.somda.sdc.dpws.soap.factory.EnvelopeFactory;
import org.somda.sdc.dpws.soap.factory.SoapFaultFactory;
import org.somda.sdc.dpws.soap.model.Envelope;
import org.somda.sdc.dpws.soap.wsaddressing.model.EndpointReferenceType;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryConstants;
import org.somda.sdc.dpws.soap.wsdiscovery.model.HelloType;
import org.somda.sdc.dpws.soap.wsdiscovery.model.ObjectFactory;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.MarshalException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JaxbSoapMarshallingSchemaTest extends DpwsTest {
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        overrideBindings(new AbstractConfigurationModule() {
            @Override
            protected void defaultConfigure() {
                bind(SoapConfig.VALIDATE_SOAP_MESSAGES,
                        Boolean.class,
                        true);
            }
        });
        super.setUp();
        getInjector().getInstance(SoapMarshalling.class).startAsync().awaitRunning();
    }

    @Test
    public void marshallValidMessage() throws Exception {
        ObjectFactory wsdFactory = getInjector().getInstance(ObjectFactory.class);
        // create a hello message with EndpointReference present
        HelloType helloType = wsdFactory.createHelloType();

        var wsaFactory = getInjector().getInstance(org.somda.sdc.dpws.soap.wsaddressing.model.ObjectFactory.class);
        var expectedEpr = wsaFactory.createEndpointReferenceType();
        var eprAddress = wsaFactory.createAttributedURIType();
        eprAddress.setValue("http://test-xAddr1");
        expectedEpr.setAddress(eprAddress);
        helloType.setEndpointReference(expectedEpr);

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
    }


    @Test
    public void marshallInvalidMessage() throws Exception {
        ObjectFactory wsdFactory = getInjector().getInstance(ObjectFactory.class);

        // create a hello message with missing EndpointReference, which should trigger a marshalling error
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

        assertThrows(MarshalException.class, () -> {
            try (OutputStream os = mockNetworkSink.createOutputStream()) {
                marshalling.marshal(expectedEnvelope, os);
            }
        });
    }
    
}
