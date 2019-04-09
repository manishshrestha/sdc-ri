package org.ieee11073.sdc.dpws.soap;

import org.ieee11073.sdc.dpws.soap.exception.MarshallingException;
import org.ieee11073.sdc.dpws.soap.exception.SoapFaultException;
import org.ieee11073.sdc.dpws.soap.exception.TransportException;
import org.ieee11073.sdc.dpws.soap.interception.InterceptorHandler;

/**
 * Interface for client APIs to invoke execution of request-response message exchanges.
 */
public interface RequestResponseClient extends InterceptorHandler {
    /**
     * Send a SOAP request message and wait for response to be received from recipient.
     *
     * @param request Outgoint request message.
     * @return Incoming response message.
     * @throws SoapFaultException SOAP fault that may be thrown on processing.
     */
    SoapMessage sendRequestResponse(SoapMessage request) throws SoapFaultException, MarshallingException, TransportException;
}
