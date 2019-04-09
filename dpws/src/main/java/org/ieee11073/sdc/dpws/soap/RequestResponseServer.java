package org.ieee11073.sdc.dpws.soap;

import org.ieee11073.sdc.dpws.soap.exception.SoapFaultException;
import org.ieee11073.sdc.dpws.soap.interception.InterceptorHandler;
import org.ieee11073.sdc.dpws.soap.interception.InterceptorResult;

/**
 * Interface for network bindings to invoke execution of incoming SOAP request messages.
 */
public interface RequestResponseServer extends InterceptorHandler {
    /**
     * Start processing of an incoming SOAP request message.
     *
     * @param request Incoming request message.
     * @param response Outhgoing response message.
     * @param transportInfo Transport information from transport layer (scheme, host, port).
     * @throws SoapFaultException SOAP fault that may be thrown on processing.
     */
    InterceptorResult receiveRequestResponse(SoapMessage request, SoapMessage response, TransportInfo transportInfo)
            throws SoapFaultException;
}
