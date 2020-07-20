package org.somda.sdc.dpws.http;

import org.eclipse.jetty.http.HttpStatus;
import org.somda.sdc.dpws.soap.CommunicationContext;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.TransportException;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Simple HTTP handler for {@link HttpServerRegistry} callbacks.
 * <p>
 * This HTTP handler is designed to be used for SOAP bindings only.
 * Hence, any modification to HTTP headers is handled by the underlying {@link HttpServerRegistry} implementation
 * according to the SOAP HTTP binding specification.
 *
 * @see <a href="https://www.w3.org/TR/2007/REC-soap12-part2-20070427/#soapinhttp">SOAP HTTP Binding</a>
 */
public interface HttpHandler {
    /**
     * Callback that is invoked by {@link HttpServerRegistry} whenever a respective context path is matched.
     *
     * @param inStream             stream of the incoming SOAP request. Do not forget to call {@linkplain
     *                             InputStream#close()} when the messages was read.
     * @param outStream            stream of the outgoing SOAP response. Do not forget to call {@linkplain
     *                             OutputStream#close()} when the message is ready to be sent back.
     * @param communicationContext information from the transport and application layer, e.g., local address, local
     *                             port, certificate data etc.
     * @throws TransportException   if any transport-related exception occurs during processing. This will hinder the
     *                              response from being sent.
     * @throws MarshallingException if any exception occurs during marshalling or unmarshalling of SOAP messages.
     * @deprecated this function requires the user to close the streams. Moreover, it throws layer-unspecific
     * exceptions which it shouldn't ({@link MarshallingException}, {@link TransportException}) in transport-agnostic
     * callbacks. Use {@link #handle(InputStream, OutputStream, CommunicationContext)} instead.
     */
    @Deprecated(since = "1.1.0", forRemoval = true)
    default void process(InputStream inStream, OutputStream outStream, CommunicationContext communicationContext)
            throws TransportException, MarshallingException
    {
        throw new UnsupportedOperationException("The process() callback is not implemented. As process() is " +
                "deprecated, implement handle() instead");
    }

    /**
     * Callback that is invoked by {@link HttpServerRegistry} whenever a respective context path is matched.
     *
     * @param inStream             stream of the incoming SOAP request.
     *                             The caller is responsible for closing the stream.
     * @param outStream            stream of the outgoing SOAP response.
     *                             The caller is responsible for closing the stream.
     * @param communicationContext information from the transport and application layer, e.g., local address, local
     *                             port, certificate data etc.
     * @throws HttpException if there is a SOAP fault thrown that is supposed to cause the HTTP layer to attach a
     *                       status code other than 200.
     *                       If the exception includes a message, then the caller is supposed to put the text to
     *                       the HTTP response. If the exception does not include a message (length is 0), then
     *                       the caller is supposed to put the outStream content to the HTTP response.
     */
    default void handle(InputStream inStream, OutputStream outStream, CommunicationContext communicationContext)
            throws HttpException
    {
        try {
            process(inStream, outStream, communicationContext);
            // CHECKSTYLE.OFF: IllegalCatch
        } catch (Exception e) {
            // CHECKSTYLE.ON: IllegalCatch
            throw new HttpException(HttpStatus.INTERNAL_SERVER_ERROR_500, e.getMessage());
        }
    }
}
