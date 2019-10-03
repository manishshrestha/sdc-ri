package org.ieee11073.sdc.dpws.soap;

import org.ieee11073.sdc.dpws.soap.exception.SoapFaultException;
import org.ieee11073.sdc.dpws.soap.interception.InterceptorHandler;
import org.ieee11073.sdc.dpws.soap.interception.InterceptorResult;

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
     * @return current progress of interceptor chain processing
     * @throws SoapFaultException if a SOAP fault comes up during processing.
     */
    InterceptorResult receiveRequestResponse(SoapMessage request, SoapMessage response, TransportInfo transportInfo)
            throws SoapFaultException;
}
