package org.somda.sdc.dpws.soap;

import org.somda.sdc.dpws.soap.exception.SoapFaultException;
import org.somda.sdc.dpws.soap.interception.InterceptorHandler;

/**
 * Interface for network bindings to process incoming SOAP request messages and their responses.
 */
public interface RequestResponseServer extends InterceptorHandler {
    /**
     * Starts processing of an incoming SOAP request message.
     *
     * @param request       incoming request message.
     * @param response      outgoing response message.
     * @param transportInfo transport information from transport layer (scheme, host, port).
     * @throws SoapFaultException if a SOAP fault comes up during processing.
     */
    void receiveRequestResponse(SoapMessage request, SoapMessage response, TransportInfo transportInfo)
            throws SoapFaultException;
}
