package org.somda.sdc.dpws.soap;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.sun.xml.bind.marshaller.NamespacePrefixMapper;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.somda.sdc.common.util.NamespacePrefixMapperConverter;
import org.somda.sdc.common.util.PrefixNamespaceMappingParser;
import org.somda.sdc.dpws.DpwsConstants;
import org.somda.sdc.dpws.FrameworkMetadata;
import org.somda.sdc.dpws.soap.model.Envelope;
import org.somda.sdc.dpws.soap.model.ObjectFactory;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingConstants;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryConstants;
import org.somda.sdc.dpws.soap.wseventing.WsEventingConstants;
import org.somda.sdc.dpws.soap.wsmetadataexchange.WsMetadataExchangeConstants;
import org.somda.sdc.dpws.soap.wstransfer.WsTransferConstants;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Creates XML input and output streams from {@link Envelope} instances by using JAXB.
 */
public class JaxbSoapMarshalling extends AbstractIdleService implements SoapMarshalling {
    private static final Logger LOG = LogManager.getLogger(JaxbSoapMarshalling.class);

    private static final String PKG_DELIM = ":";
    private static final String SCHEMA_DELIM = ":";
    private static final String XML_PROLOG = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";

    private final NamespacePrefixMapper namespacePrefixMapper;
    private final String schemaPath;
    private final Boolean validateSoapMessages;
    private final ObjectFactory soapFactory;
    private final FrameworkMetadata metadata;
    private final Boolean metadataComment;


    private String contextPackages;
    private JAXBContext jaxbContext;
    private Schema schema;

    @Inject
    JaxbSoapMarshalling(@Named(SoapConfig.JAXB_CONTEXT_PATH) String contextPackages,
                        @Named(SoapConfig.NAMESPACE_MAPPINGS) String namespaceMappings,
                        @Named(SoapConfig.JAXB_SCHEMA_PATH) String schemaPath,
                        @Named(SoapConfig.VALIDATE_SOAP_MESSAGES) Boolean validateSoapMessages,
                        @Named(SoapConfig.METADATA_COMMENT) Boolean metadataComment,
                        PrefixNamespaceMappingParser namespaceMappingParser,
                        NamespacePrefixMapperConverter namespacePrefixMapperConverter,
                        ObjectFactory soapFactory,
                        FrameworkMetadata metadata) {
        this.contextPackages = contextPackages;
        this.schemaPath = schemaPath;
        this.validateSoapMessages = validateSoapMessages;
        this.metadataComment = metadataComment;
        this.soapFactory = soapFactory;
        this.metadata = metadata;

        // Append internal mappings
        namespaceMappings += SoapConstants.NAMESPACE_PREFIX_MAPPINGS;

        this.namespacePrefixMapper = namespacePrefixMapperConverter.convert(
                namespaceMappingParser.parse(namespaceMappings));

    }

    @Override
    protected void startUp() throws Exception {
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
        if (schema != null) {
            marshaller.setSchema(schema);
        }
        if (metadataComment) {
            // don't generate document level events (i.e. the XML prolog)
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);

            var version = metadata.getFrameworkVersion();
            var versionString = "<!-- Generated with SDCri " + version + " -->";
            LOG.debug("Attaching metadata comment: {}", versionString);
            try {
                outputStream.write(XML_PROLOG.getBytes(StandardCharsets.UTF_8));
                outputStream.write(versionString.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                LOG.error("Error while writing SDCri metadata to message");
            }
        }
        marshaller.marshal(soapFactory.createEnvelope(envelope), outputStream);
    }

    private void checkRunning() {
        if (!isRunning()) {
            throw new RuntimeException("Try to marshal, but marshalling service is not running. " +
                    "Please check if the DPWS framework is up and running.");
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

        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        if (schema != null) {
            unmarshaller.setSchema(schema);
        }
        return ((JAXBElement<Envelope>) (unmarshaller.unmarshal(inputStream))).getValue();
    }

    private void initializeJaxb() throws SAXException, IOException, ParserConfigurationException {
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
            LOG.error("JAXB context for SOAP model(s) could not be created", e);
            throw new RuntimeException("JAXB context for SOAP model(s) could not be created");
        }

        if (validateSoapMessages) {
            var extendedSchemaPath = SoapConstants.SCHEMA_PATH +
                    SCHEMA_DELIM + WsAddressingConstants.SCHEMA_PATH +
                    SCHEMA_DELIM + WsDiscoveryConstants.SCHEMA_PATH +
                    SCHEMA_DELIM + WsEventingConstants.SCHEMA_PATH +
                    SCHEMA_DELIM + WsMetadataExchangeConstants.SCHEMA_PATH +
                    SCHEMA_DELIM + WsTransferConstants.SCHEMA_PATH +
                    SCHEMA_DELIM + DpwsConstants.SCHEMA_PATH +
                    SCHEMA_DELIM + schemaPath;
            LOG.info("SOAP message validation enabled with schemas (order matters!): {}", extendedSchemaPath);
            schema = generateTopLevelSchema(extendedSchemaPath);
        } else {
            LOG.info("SOAP message validation disabled");
            schema = null;
        }
    }

    private Schema generateTopLevelSchema(String schemaPath) throws SAXException, IOException, ParserConfigurationException {
        final var topLevelSchemaBeginning =
                "<xsd:schema xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" elementFormDefault=\"qualified\">";
        final var importPattern = "<xsd:import namespace=\"%s\" schemaLocation=\"%s\"/>";
        final var topLevelSchemaEnd = "</xsd:schema>";

        var stringBuilder = new StringBuilder();
        stringBuilder.append(topLevelSchemaBeginning);
        for (String path : schemaPath.split(SCHEMA_DELIM)) {
            var classLoader = getClass().getClassLoader();
            var schemaUrl = classLoader.getResource(path);
            if (schemaUrl == null) {
                LOG.error("Could not find schema for resource: {}", path);
                throw new IOException(String.format("Could not find schema for resource while loading in %s: %s",
                        JaxbSoapMarshalling.class.getSimpleName(), path));
            }
            var targetNamespace = resolveTargetNamespace(schemaUrl);
            LOG.info("Register namespace for validation: {}, read from {}", targetNamespace, schemaUrl.toString());
            stringBuilder.append(String.format(importPattern, targetNamespace, schemaUrl.toString()));
        }
        stringBuilder.append(topLevelSchemaEnd);
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        return schemaFactory.newSchema(new StreamSource(new ByteArrayInputStream(stringBuilder.toString()
                .getBytes(StandardCharsets.UTF_8))));
    }

    private String resolveTargetNamespace(URL url) throws IOException, ParserConfigurationException, SAXException {
        try (InputStream inputStream = url.openStream()) {
            var factory = DocumentBuilderFactory.newInstance();
            var builder = factory.newDocumentBuilder();
            var document = builder.parse(inputStream);
            return document.getDocumentElement().getAttribute("targetNamespace");
        }
    }
}
