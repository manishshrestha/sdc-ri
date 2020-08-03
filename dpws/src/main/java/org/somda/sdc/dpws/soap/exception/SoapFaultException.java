package org.somda.sdc.dpws.soap.exception;

import org.somda.sdc.dpws.soap.SoapMessage;
import org.somda.sdc.dpws.soap.model.Fault;
import org.somda.sdc.dpws.soap.model.Reasontext;
import org.somda.sdc.dpws.soap.wsaddressing.model.AttributedURIType;

import javax.xml.bind.JAXBElement;
import java.util.List;
import java.util.Optional;

/**
 * Defines an exception that is supposed to be used to convey SOAP fault information.
 *
 * @see <a href="https://www.w3.org/TR/2007/REC-soap12-part1-20070427/#soapfault">SOAP Fault</a>
 */
public class SoapFaultException extends Exception {
    private static final String UNSPECIFIED_MESSAGE = "http://www.w3.org/2005/08/addressing/unspecified";
    private final SoapMessage faultMessage;
    private final Fault fault;

    /**
     * Constructor that requires an wrapped SOAP fault message.
     *
     * @param faultMessage SOAP message that shall include a {@linkplain JAXBElement} with {@link Fault} body.
     *                     Otherwise, a {@linkplain ClassCastException} is thrown.
     * @param messageId    of the request, to properly set the relatesTo field.
     */
    @SuppressWarnings("unchecked")
    public SoapFaultException(SoapMessage faultMessage, Optional<AttributedURIType> messageId) {
        this.faultMessage = faultMessage;
        this.fault = ((JAXBElement<Fault>) faultMessage.getOriginalEnvelope().getBody().getAny().get(0)).getValue();
        setRelatesTo(messageId);
    }

    /**
     * Constructor that requires a wrapped SOAP fault message plus a nested cause.
     *
     * @param faultMessage SOAP message that shall include a {@linkplain JAXBElement} with {@link Fault} body.
     *                     Otherwise, a {@linkplain ClassCastException} is thrown.
     * @param throwable    extended information, e.g. transport layer info.
     * @param messageId    of the request, to properly set the relatesTo field.
     */
    public SoapFaultException(SoapMessage faultMessage, Throwable throwable, Optional<AttributedURIType> messageId) {
        super(throwable);
        this.faultMessage = faultMessage;
        this.fault = ((JAXBElement<Fault>) faultMessage.getOriginalEnvelope().getBody().getAny().get(0)).getValue();
        setRelatesTo(messageId);
    }

    private void setRelatesTo(Optional<AttributedURIType> messageId) {
        final AttributedURIType unspecifiedMessageUri = new AttributedURIType();
        unspecifiedMessageUri.setValue(UNSPECIFIED_MESSAGE);
        if (faultMessage.getWsAddressingHeader().getRelatesTo().isEmpty()) {
            faultMessage.getWsAddressingHeader().setRelatesTo(messageId.orElse(unspecifiedMessageUri));
        }
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
