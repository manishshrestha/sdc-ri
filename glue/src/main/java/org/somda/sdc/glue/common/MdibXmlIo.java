package org.somda.sdc.glue.common;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.sun.xml.bind.marshaller.NamespacePrefixMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.biceps.model.message.GetMdibResponse;
import org.somda.sdc.biceps.model.message.ObjectFactory;
import org.somda.sdc.biceps.model.participant.Mdib;
import org.somda.sdc.biceps.model.participant.MdibVersion;
import org.somda.sdc.common.util.NamespacePrefixMapperConverter;
import org.somda.sdc.common.util.PrefixNamespaceMappingParser;
import org.somda.sdc.dpws.soap.SoapConstants;
import org.somda.sdc.glue.GlueConstants;
import org.somda.sdc.glue.provider.services.helper.MdibVersionUtil;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.*;

/**
 * Utility class to read an {@linkplain Mdib} from an input stream (or file).
 */
public class MdibXmlIo {
    private static final Logger LOG = LoggerFactory.getLogger(MdibXmlIo.class);
    private final ObjectFactory messageModelFactory;
    private final MdibVersionUtil mdibVersionUtil;
    private final NamespacePrefixMapper namespacePrefixMapper;

    private JAXBContext jaxbContext;

    @Inject
    MdibXmlIo(ObjectFactory messageModelFactory,
              MdibVersionUtil mdibVersionUtil,
              PrefixNamespaceMappingParser prefixNamespaceMappingParser,
              NamespacePrefixMapperConverter namespacePrefixMapperConverter,
              @Named(CommonConfig.NAMESPACE_MAPPINGS) String namespaceMappings) {
        this.messageModelFactory = messageModelFactory;
        this.mdibVersionUtil = mdibVersionUtil;

        // Append internal namespace prefix mappings
        namespaceMappings += CommonConstants.NAMESPACE_PREFIX_MAPPINGS_MDPWS +
                CommonConstants.NAMESPACE_PREFIX_MAPPINGS_BICEPS + CommonConstants.NAMESPACE_PREFIX_MAPPINGS_GLUE +
                "{xsi:" + SoapConstants.NAMESPACE_XSI + "}";

        this.namespacePrefixMapper = namespacePrefixMapperConverter.convert(
                prefixNamespaceMappingParser.parse(namespaceMappings));

        initJaxb();
    }

    /**
     * Reads the MDIB from an input stream.
     *
     * @return the parsed {@link Mdib}.
     * @throws JAXBException      in case JAXB cannot parse the input stream.
     * @throws ClassCastException if something unexpected was read in.
     */
    public Mdib readMdib(InputStream getMdibResponseStream) throws JAXBException, ClassCastException {
        return ((GetMdibResponse) (jaxbContext.createUnmarshaller().unmarshal(getMdibResponseStream))).getMdib();
    }

    /**
     * Reads an MDIB from a file input.
     *
     * @return the parsed {@link Mdib}.
     * @throws JAXBException         in case JAXB cannot parse the input stream.
     * @throws ClassCastException    if something unexpected was read in.
     * @throws FileNotFoundException if the object tried to access a file that does not exist.
     */
    public Mdib readMdib(File getMdibResponseFile) throws JAXBException, ClassCastException, FileNotFoundException {
        return readMdib(new FileInputStream(getMdibResponseFile));
    }

    /**
     * Writes an MDIB to an output stream.
     *
     * @param mdib         the MDIB to write.
     * @param outputStream the output stream where to write the marshalled XML to.
     * @throws JAXBException in case the marshaller throws an exception.
     */
    public void writeMdib(Mdib mdib, OutputStream outputStream) throws JAXBException {
        final GetMdibResponse getMdibResponse = messageModelFactory.createGetMdibResponse();

        try {
            // Set a random UUID; no real purpose, but required for validity
            mdibVersionUtil.setMdibVersion(MdibVersion.create(), getMdibResponse);
        } catch (Exception e) {
            LOG.warn("Unexpected error during setMdibVersion on a GetMdibResponseObject. " +
                    "Nothing was written to the output stream", e);
            return;
        }

        getMdibResponse.setMdib(mdib);

        final Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(NamespacePrefixMapperConverter.JAXB_MARSHALLER_PROPERTY_KEY, namespacePrefixMapper);
        marshaller.marshal(getMdibResponse, outputStream);
    }

    /**
     * Writes an MDIB to an file.
     *
     * @param mdib       the MDIB to write.
     * @param outputFile the output file where to write the marshalled XML to.
     * @throws JAXBException         in case the marshaller throws an exception.
     * @throws FileNotFoundException in case the file cannot be created.
     */
    public void writeMdib(Mdib mdib, File outputFile) throws JAXBException, FileNotFoundException {
        writeMdib(mdib, new FileOutputStream(outputFile));
    }

    private void initJaxb() {
        LOG.info("Setup an MDIB XML reader with JAXB contexts: {}", GlueConstants.JAXB_CONTEXT_PATH);

        try {
            jaxbContext = JAXBContext.newInstance(GlueConstants.JAXB_CONTEXT_PATH);
        } catch (JAXBException e) {
            throw new RuntimeException(String.format("JAXB context for '%s' could not be set up",
                    GlueConstants.JAXB_CONTEXT_PATH), e);
        }
    }
}
