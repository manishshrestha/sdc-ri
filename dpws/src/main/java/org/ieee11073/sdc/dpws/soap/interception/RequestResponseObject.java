package org.ieee11073.sdc.dpws.soap.interception;

import org.ieee11073.sdc.dpws.soap.SoapMessage;
import org.ieee11073.sdc.dpws.soap.TransportInfo;

import java.util.Optional;

/**
 * Object pushed from interceptor processors to provide both a SOAP request and response message.
 */
public class RequestResponseObject implements InterceptorCallbackType {
    private final SoapMessage request;
    private final SoapMessage response;
    private final TransportInfo transportInfo;

    public RequestResponseObject(SoapMessage request, SoapMessage response, TransportInfo transportInfo) {
        this.request = request;
        this.response = response;
        this.transportInfo = transportInfo;
    }

    public RequestResponseObject(SoapMessage request, SoapMessage response) {
        this.request = request;
        this.response = response;
        this.transportInfo = null;
    }

    public SoapMessage getRequest() {
        return request;
    }

    public SoapMessage getResponse() {
        return response;
    }

    public Optional<TransportInfo> getTransportInfo() {
        return Optional.ofNullable(transportInfo);
    }

}
