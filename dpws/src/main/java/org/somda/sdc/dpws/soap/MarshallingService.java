package org.somda.sdc.dpws.soap;

import com.google.inject.Inject;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.model.Envelope;

import javax.xml.bind.JAXBException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Utility class to marshal/unmarshal SOAP messages.
 */
public class MarshallingService {
    private final SoapMarshalling soapMarshalling;
    private final SoapUtil soapUtil;

    @Inject
    MarshallingService(SoapUtil soapUtil,
                       SoapMarshalling soapMarshalling) {
        this.soapUtil = soapUtil;
        this.soapMarshalling = soapMarshalling;
    }

    /**
     * Marshals a SOAP message.
     *
     * @param msg the message to marshal.
     * @param os  the output stream where to write the XML message to.
     * @throws MarshallingException if any exception occurs during marshalling.
     */
    public void marshal(SoapMessage msg, OutputStream os) throws MarshallingException {
        try {
            soapMarshalling.marshal(msg.getEnvelopeWithMappedHeaders(), os);
        } catch (JAXBException e) {
            throw new MarshallingException(e);
        }
    }

    /**
     * Unmarshals a SOAP message from an input stream.
     *
     * @param is the input stream where to unmarshal from.
     * @return the unmarshalled object.
     * @throws MarshallingException if any exception occurs during unmarshalling.
     */
    public SoapMessage unmarshal(InputStream is) throws MarshallingException {
        try {
            Envelope env = soapMarshalling.unmarshal(is);
            return soapUtil.createMessage(env);
        } catch (Exception e) {
            throw new MarshallingException(e);
        }
    }

    /**
     * Uses the given {@link RequestResponseServer} object to accept unmarshalled request data and marshalled response data.
     *
     * @param srv                  the request-response server where to call
     *                             {@link RequestResponseServer#receiveRequestResponse(SoapMessage, SoapMessage, CommunicationContext)}
     * @param is                   input stream that provides SOAP request message.
     * @param os                   output stream where to write SOAP response message to.
     * @param communicationContext transport and application layer information.
     * @throws MarshallingException if any exception occurs during marshalling or unmarshalling of SOAP messages.
     */
    public void handleRequestResponse(RequestResponseServer srv,
                                      InputStream is,
                                      OutputStream os,
                                      CommunicationContext communicationContext) throws MarshallingException {
        SoapMessage responseMessage = soapUtil.createMessage();
        try {
            srv.receiveRequestResponse(unmarshal(is), responseMessage, communicationContext);
        } catch (SoapFaultException e) {
            marshal(e.getFaultMessage(), os);
            return;
        }
        marshal(responseMessage, os);
    }
}
