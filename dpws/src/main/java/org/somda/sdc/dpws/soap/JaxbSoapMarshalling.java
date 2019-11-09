package org.somda.sdc.dpws.soap;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.sun.xml.bind.marshaller.NamespacePrefixMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.common.util.NamespacePrefixMapperConverter;
import org.somda.sdc.common.util.PrefixNamespaceMappingParser;
import org.somda.sdc.dpws.DpwsConstants;
import org.somda.sdc.dpws.soap.model.Envelope;
import org.somda.sdc.dpws.soap.model.ObjectFactory;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingConstants;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryConstants;
import org.somda.sdc.dpws.soap.wseventing.WsEventingConstants;
import org.somda.sdc.dpws.soap.wsmetadataexchange.WsMetadataExchangeConstants;
import org.somda.sdc.dpws.soap.wstransfer.WsTransferConstants;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Creates XML input and output streams from {@link Envelope} instances by using JAXB.
 */
public class JaxbSoapMarshalling extends AbstractIdleService implements SoapMarshalling {
    private static final Logger LOG = LoggerFactory.getLogger(JaxbSoapMarshalling.class);

    private static final String PKG_DELIM = ":";

    private final NamespacePrefixMapper namespacePrefixMapper;
    private final ObjectFactory soapFactory;


    private String contextPackages;
    private JAXBContext jaxbContext;

    @Inject
    JaxbSoapMarshalling(@Named(SoapConfig.JAXB_CONTEXT_PATH) String contextPackages,
                        @Named(SoapConfig.NAMESPACE_MAPPINGS) String namespaceMappings,
                        PrefixNamespaceMappingParser namespaceMappingParser,
                        NamespacePrefixMapperConverter namespacePrefixMapperConverter,
                        ObjectFactory soapFactory) {
        this.contextPackages = contextPackages;
        this.soapFactory = soapFactory;
        namespaceMappings +=
                "{xsi:http://www.w3.org/2001/XMLSchema-instance}" +
                        "{wsa:http://www.w3.org/2005/08/addressing}" +
                        "{wse:http://schemas.xmlsoap.org/ws/2004/08/eventing}" +
                        "{wsd:http://docs.oasis-open.org/ws-dd/ns/discovery/2009/01}" +
                        "{wsm:http://schemas.xmlsoap.org/ws/2004/09/mex}" +
                        "{wst:http://schemas.xmlsoap.org/ws/2004/09/transfer}" +
                        "{dpws:http://docs.oasis-open.org/ws-dd/ns/dpws/2009/01}" +
                        "{s12:http://www.w3.org/2003/05/soap-envelope}";

        namespacePrefixMapper = namespacePrefixMapperConverter.convert(
                namespaceMappingParser.parse(namespaceMappings));
    }

    @Override
    protected void startUp() {
        LOG.info("Start SOAP marshalling. Initialize JAXB.");
        initializeJaxb();
        LOG.info("JAXB initialization finished");
    }

    @Override
    protected void shutDown() {
        LOG.info("SOAP marshalling stopped");
    }

    /**
     * Takes a SOAP envelope and marshals it.
     *
     * @param envelope     the source envelope to marshal.
     * @param outputStream the destination of the marshalled data.
     * @throws JAXBException if marshalling fails.
     */
    @Override
    public void marshal(Envelope envelope, OutputStream outputStream) throws JAXBException {
        checkRunning();
        final Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(NamespacePrefixMapperConverter.JAXB_MARSHALLER_PROPERTY_KEY, namespacePrefixMapper);
        marshaller.marshal(soapFactory.createEnvelope(envelope), outputStream);
    }

    private void checkRunning() {
        if (!isRunning()) {
            throw new RuntimeException("Try to marshal, but marshalling service is not running");
        }
    }

    /**
     * Takes an input stream and unmarshals it.
     *
     * @param inputStream the input stream to unmarshal.
     * @return the unmarshalled SOAP envelope.
     * @throws JAXBException      if unmarshalling fails.
     * @throws ClassCastException in case unmarshalled data could not be cast to a JAXB element.
     */
    @Override
    @SuppressWarnings("unchecked")
    public Envelope unmarshal(InputStream inputStream) throws JAXBException, ClassCastException {
        checkRunning();
        return ((JAXBElement<Envelope>) (jaxbContext.createUnmarshaller().unmarshal(inputStream))).getValue();
    }

    private void initializeJaxb() {
        if (!contextPackages.isEmpty()) {
            contextPackages += PKG_DELIM;
        }
        contextPackages += SoapConstants.JAXB_CONTEXT_PACKAGE + PKG_DELIM +
                DpwsConstants.JAXB_CONTEXT_PACKAGE + PKG_DELIM +
                WsAddressingConstants.JAXB_CONTEXT_PACKAGE + PKG_DELIM +
                WsDiscoveryConstants.JAXB_CONTEXT_PACKAGE + PKG_DELIM +
                WsEventingConstants.JAXB_CONTEXT_PACKAGE + PKG_DELIM +
                WsTransferConstants.JAXB_CONTEXT_PACKAGE + PKG_DELIM +
                WsMetadataExchangeConstants.JAXB_CONTEXT_PACKAGE;

        LOG.info("Configure JAXB with contexts: {}", contextPackages);

        try {
            jaxbContext = JAXBContext.newInstance(contextPackages);
        } catch (JAXBException e) {
            throw new RuntimeException("JAXB context for SOAP model(s) could not be created");
        }
    }
}
