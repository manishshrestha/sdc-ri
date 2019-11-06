package org.somda.sdc.dpws.soap;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.somda.sdc.dpws.DpwsConstants;
import org.somda.sdc.dpws.soap.model.Envelope;
import org.somda.sdc.dpws.soap.model.ObjectFactory;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingConstants;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryConstants;
import org.somda.sdc.dpws.soap.wseventing.WsEventingConstants;
import org.somda.sdc.dpws.soap.wsmetadataexchange.WsMetadataExchangeConstants;
import org.somda.sdc.dpws.soap.wstransfer.WsTransferConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Creates XML input and output streams from {@link Envelope} instances by using JAXB.
 */
public class JaxbSoapMarshalling extends AbstractIdleService implements SoapMarshalling {
    private static final Logger LOG = LoggerFactory.getLogger(JaxbSoapMarshalling.class);

    private static final String pkgDelim = ":";
    private final ObjectFactory soapFactory;

    private String contextPackages;
    private JAXBContext jaxbContext;

    @Inject
    JaxbSoapMarshalling(@Named(SoapConfig.JAXB_CONTEXT_PATH) String contextPackages,
                        ObjectFactory soapFactory) {
        this.contextPackages = contextPackages;
        this.soapFactory = soapFactory;
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
        jaxbContext.createMarshaller().marshal(soapFactory.createEnvelope(envelope), outputStream);
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
            contextPackages += pkgDelim;
        }
        contextPackages += SoapConstants.JAXB_CONTEXT_PACKAGE + pkgDelim +
                DpwsConstants.JAXB_CONTEXT_PACKAGE + pkgDelim +
                WsAddressingConstants.JAXB_CONTEXT_PACKAGE + pkgDelim +
                WsDiscoveryConstants.JAXB_CONTEXT_PACKAGE + pkgDelim +
                WsEventingConstants.JAXB_CONTEXT_PACKAGE + pkgDelim +
                WsTransferConstants.JAXB_CONTEXT_PACKAGE + pkgDelim +
                WsMetadataExchangeConstants.JAXB_CONTEXT_PACKAGE;

        LOG.info("Configure JAXB with contexts: {}", contextPackages);

        try {
            jaxbContext = JAXBContext.newInstance(contextPackages);
        } catch (JAXBException e) {
            throw new RuntimeException("JAXB context for SOAP model(s) could not be created");
        }
    }
}
