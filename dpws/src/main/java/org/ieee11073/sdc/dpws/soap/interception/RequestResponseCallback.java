package org.ieee11073.sdc.dpws.soap.interception;

import org.ieee11073.sdc.dpws.soap.exception.MarshallingException;
import org.ieee11073.sdc.dpws.soap.exception.SoapFaultException;
import org.ieee11073.sdc.dpws.soap.SoapMessage;
import org.ieee11073.sdc.dpws.soap.exception.TransportException;

/**
 * Callback for network bindings to invoke request-response message exchange.
 */
public interface RequestResponseCallback {
    /**
     * Client is synchronously requested to invoke a request-response message exchange on the network.
     *
     * @param request The request to send.
     * @return A SOAP message with the received response.
     * @throws SoapFaultException Any SOAP fault a server answers.
     * @throws TransportException   Any transport-related exception during processing. This will hinder the response from
     *                              being sent.
     * @throws MarshallingException Any exception that occurs during marshalling or unmarshalling of SOAP messages.
     */
    SoapMessage onRequestResponse(SoapMessage request) throws SoapFaultException, MarshallingException, TransportException;
}
