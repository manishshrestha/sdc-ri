package org.somda.sdc.dpws.http;

import org.somda.sdc.dpws.soap.TransportInfo;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.TransportException;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Simple HTTP handler for {@link HttpServerRegistry} callbacks.
 * <p>
 * This HTTP handler is designed to be used for SOAP bindings only.
 * Hence, any modifications to HTTP headers is handled by the underlying {@link HttpServerRegistry} implementation
 * according to the SOAP HTTP binding specification.
 *
 * @see <a href="https://www.w3.org/TR/2007/REC-soap12-part2-20070427/#soapinhttp">SOAP HTTP Binding</a>
 */
public interface HttpHandler {
    /**
     * Callback that is invoked by {@link HttpServerRegistry} whenever a respective context path is matched.
     *
     * @param inStream      stream of the incoming SOAP request. Do not forget to call {@linkplain
     *                      InputStream#close()} when the messages was read.
     * @param outStream     stream of the outgoing SOAP response. Do not forget to call {@linkplain
     *                      OutputStream#close()} when the message is ready to be sent back.
     * @param transportInfo information from the transport layer, e.g., local address, local port, certificate data etc.
     * @throws TransportException   if any transport-related exception occurs during processing. This will hinder the
     *                              response from being sent.
     * @throws MarshallingException if any exception occurs during marshalling or unmarshalling of SOAP messages.
     */
    void process(InputStream inStream, OutputStream outStream, TransportInfo transportInfo)
            throws TransportException, MarshallingException;
}
