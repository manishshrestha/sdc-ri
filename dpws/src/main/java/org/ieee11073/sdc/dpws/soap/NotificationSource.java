package org.ieee11073.sdc.dpws.soap;

import org.ieee11073.sdc.dpws.soap.exception.MarshallingException;
import org.ieee11073.sdc.dpws.soap.exception.TransportException;
import org.ieee11073.sdc.dpws.soap.interception.InterceptorHandler;
import org.ieee11073.sdc.dpws.soap.interception.InterceptorResult;

/**
 * Interface for client APIs to invoke execution of notification message exchanges.
 */
public interface NotificationSource extends InterceptorHandler {
    /**
     * Send a SOAP notification message.
     *
     * This method returns as soon as the message left the application process.
     *
     * @param notification Outgoing notification message.
     * @return current progress of interceptor chain processing
     * @throws TransportException   Any transport-related exception during processing. This will hinder the response from
     *                              being sent.
     * @throws MarshallingException Any exception that occurs during marshalling or unmarshalling of SOAP messages.
     */
    InterceptorResult sendNotification(SoapMessage notification) throws MarshallingException, TransportException;
}
