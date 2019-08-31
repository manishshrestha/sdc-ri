package org.ieee11073.sdc.dpws.soap;

import com.google.inject.Inject;
import org.ieee11073.sdc.dpws.soap.exception.MarshallingException;
import org.ieee11073.sdc.dpws.soap.exception.SoapFaultException;
import org.ieee11073.sdc.dpws.soap.model.Envelope;

import javax.xml.bind.JAXBException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Utility class to marshal/unmarshal SOAP messages.
 *
 * Moreover, this class possesses
 * {@link #handleRequestResponse(RequestResponseServer, InputStream, OutputStream, TransportInfo)} to execute some
 * marshalling boilerplate code with {@link RequestResponseServer} instances.
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
     * Marshal a SOAP message.
     *
     * @param msg The message to marshal.
     * @param os  The output stream where to write the XML message to.
     * @throws MarshallingException Any exception that occurs during marshalling or unmarshalling of SOAP messages.
     */
    public void marshal(SoapMessage msg, OutputStream os) throws MarshallingException {
        try {
            soapMarshalling.marshal(msg.getEnvelopeWithMappedHeaders(), os);
        } catch (JAXBException e) {
            throw new MarshallingException(e);
        }
    }

    /**
     * Unmarshal a SOAP message from an input stream.
     *
     * @param is The input stream where to unmarshal from.
     * @return The unmarshalled object.
     * @throws MarshallingException Any exception that occurs during marshalling or unmarshalling of SOAP messages.
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
     * Use the given {@link RequestResponseServer} object to pass unmarshalled request data and marshaled response data.
     *
     * @param srv           The request-response server where to call
     *                      {@link RequestResponseServer#receiveRequestResponse(SoapMessage, SoapMessage, TransportInfo)}
     * @param is            Input stream that provides SOAP request message.
     * @param os            Output stream where to write SOAP response message to.
     * @param transportInfo Transport layer information.
     * @throws MarshallingException Any exception that occurs during marshalling or unmarshalling of SOAP messages.
     */
    public void handleRequestResponse(RequestResponseServer srv,
                                      InputStream is,
                                      OutputStream os,
                                      TransportInfo transportInfo) throws MarshallingException {
        SoapMessage responseMessage = soapUtil.createMessage();
        try {
            srv.receiveRequestResponse(unmarshal(is), responseMessage, transportInfo);
        } catch (SoapFaultException e) {
            marshal(e.getFaultMessage(), os);
            return;
        }
        marshal(responseMessage, os);
    }
}
