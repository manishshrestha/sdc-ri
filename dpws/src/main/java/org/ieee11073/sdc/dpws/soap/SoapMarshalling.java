package org.ieee11073.sdc.dpws.soap;

import com.google.common.util.concurrent.Service;
import org.ieee11073.sdc.dpws.soap.model.Envelope;

import javax.xml.bind.JAXBException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * JAXB SOAP marshalling service.
 */
public interface SoapMarshalling extends Service {
    /**
     * Marshal a SOAP message.
     *
     * @param envelope     The envelope of the SOAP message.
     * @param outputStream The output stream where to write marshalled data to.
     * @throws JAXBException If marshalling to output stream failed
     */
    void marshal(Envelope envelope, OutputStream outputStream) throws JAXBException;

    /**
     * Unmarshal a SOAP message.
     *
     * @param inputStream The input message to unmarshal a SOAP message from.
     * @return The unmarshalled SOAP envelope.
     * @throws JAXBException If unmarshalling fails.
     * @throws ClassCastException If the cast to {@link Envelope} fails.
     */
    @SuppressWarnings("unchecked")
    Envelope unmarshal(InputStream inputStream) throws JAXBException, ClassCastException;
}
