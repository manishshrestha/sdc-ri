package org.ieee11073.sdc.dpws.soap.interception;

import org.ieee11073.sdc.dpws.soap.SoapMessage;

/**
 * Object pushed from interceptor processors to provide a SOAP request message.
 */
public class RequestObject implements InterceptorCallbackType {
    private final SoapMessage request;

    public RequestObject(SoapMessage request) {
        this.request = request;
    }

    public SoapMessage getRequest() {
        return request;
    }
}
