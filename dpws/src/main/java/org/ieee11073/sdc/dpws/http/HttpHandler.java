package org.ieee11073.sdc.dpws.http;

import org.ieee11073.sdc.dpws.soap.TransportInfo;
import org.ieee11073.sdc.dpws.soap.exception.MarshallingException;
import org.ieee11073.sdc.dpws.soap.exception.TransportException;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Simple HTTP handler for {@link HttpServerRegistry} callbacks.
 *
 * This HTTP handler is designated to be used for SOAP bindings only. Hence, any modifications to HTTP headers is
 * handled by the underlying {@link HttpServerRegistry} implementation according to the SOAP HTTP binding.
 *
 * @see <a href="https://www.w3.org/TR/2007/REC-soap12-part2-20070427/#soapinhttp">SOAP HTTP Binding</a>
 */
public interface HttpHandler {
    /**
     * Callback that is invoked by {@link HttpServerRegistry} whenever a respective context path is matched.
     *
     * @param inStream      Stream of the incoming SOAP request. Do not forget to call {@linkplain
     *                      InputStream#close()} when the messages was read.
     * @param outStream     Stream of the outgoing SOAP response. Do not forget to call {@linkplain
     *                      OutputStream#close()} when the message is ready to be send back.
     * @param transportInfo Information from transport layer.
     * @throws TransportException   Any transport-related exception during processing. This will hinder the response from
     *                              being sent.
     * @throws MarshallingException Any exception that occurs during mashalling or unmarshalling of SOAP messages.
     */
    void process(InputStream inStream, OutputStream outStream, TransportInfo transportInfo)
            throws TransportException, MarshallingException;
}
