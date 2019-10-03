package org.ieee11073.sdc.dpws.soap;

import org.ieee11073.sdc.dpws.soap.exception.MarshallingException;
import org.ieee11073.sdc.dpws.soap.exception.TransportException;
import org.ieee11073.sdc.dpws.soap.interception.InterceptorHandler;
import org.ieee11073.sdc.dpws.soap.interception.InterceptorResult;

/**
 * Interface to send notifications to event sinks.
 */
public interface NotificationSource extends InterceptorHandler {
    /**
     * Sends a SOAP notification message.
     * <p>
     * This method returns as soon as the message left the application process.
     *
     * @param notification outgoing notification message.
     * @return the interceptor chain state.
     * @throws TransportException   if any transport-related exception comes up during processing. This will hinder the
     *                              response from being sent.
     * @throws MarshallingException if any exception occurs during marshalling or unmarshalling of SOAP messages.
     */
    InterceptorResult sendNotification(SoapMessage notification) throws MarshallingException, TransportException;
}
