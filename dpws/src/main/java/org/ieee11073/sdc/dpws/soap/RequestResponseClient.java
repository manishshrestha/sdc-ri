package org.ieee11073.sdc.dpws.soap;

import org.ieee11073.sdc.dpws.soap.exception.MarshallingException;
import org.ieee11073.sdc.dpws.soap.exception.SoapFaultException;
import org.ieee11073.sdc.dpws.soap.exception.TransportException;
import org.ieee11073.sdc.dpws.soap.interception.InterceptorHandler;

/**
 * Interface for client APIs to conduct request-response message exchanges.
 */
public interface RequestResponseClient extends InterceptorHandler {
    /**
     * Sends a SOAP request message and waits for the response to be received from the recipient.
     *
     * @param request outgoing request message.
     * @return incoming response message.
     * @throws SoapFaultException   if a SOAP fault comes up during processing.
     * @throws TransportException   if  transport-related exceptions come up during processing.
     *                              This will hinder the response from being sent.
     * @throws MarshallingException if any exception occurs during marshalling or unmarshalling of SOAP messages.
     */
    SoapMessage sendRequestResponse(SoapMessage request) throws SoapFaultException, MarshallingException, TransportException;
}
