package org.ieee11073.sdc.dpws.soap.exception;

import org.ieee11073.sdc.dpws.soap.SoapMessage;
import org.ieee11073.sdc.dpws.soap.model.Fault;
import org.ieee11073.sdc.dpws.soap.model.Reasontext;

import javax.xml.bind.JAXBElement;
import java.util.List;

/**
 * Defines an exception that is supposed to be used to convey SOAP fault information.
 *
 * @see <a href="https://www.w3.org/TR/2007/REC-soap12-part1-20070427/#soapfault">SOAP Fault</a>
 */
public class SoapFaultException extends Exception {
    private final SoapMessage faultMessage;
    private final Fault fault;


    /**
     * Constructor that requires an wrapped SOAP fault message.
     * <p>
     * todo DGr design flaw: implementation requires knowledge of JAXB.
     * @param faultMessage SOAP message that shall include a {@linkplain JAXBElement} with {@link Fault} body.
     *                     Otherwise, a {@linkplain ClassCastException} is thrown.
     */
    @SuppressWarnings("unchecked")
    public SoapFaultException(SoapMessage faultMessage) {
        this.faultMessage = faultMessage;
        this.fault = ((JAXBElement<Fault>)faultMessage.getOriginalEnvelope().getBody().getAny().get(0)).getValue();
    }

    public SoapMessage getFaultMessage() {
        return faultMessage;
    }

    /**
     * Gets the SOAP fault information.
     *
     * @return {@link Fault} that is encapsulated in the {@link SoapMessage} wrapped by this exception.
     */
    public Fault getFault() {
        return fault;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append(getFault().getCode().getValue().toString());
        List<Reasontext> text = getFault().getReason().getText();
        if (text.size() > 0) {
            sb.append(": ");
            sb.append(text.get(0).getValue());
        }
        return sb.toString();
    }
}
