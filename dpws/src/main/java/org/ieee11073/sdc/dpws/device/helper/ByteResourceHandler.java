package org.ieee11073.sdc.dpws.device.helper;

import org.ieee11073.sdc.dpws.http.HttpHandler;
import org.ieee11073.sdc.dpws.soap.TransportInfo;
import org.ieee11073.sdc.dpws.soap.exception.MarshallingException;
import org.ieee11073.sdc.dpws.soap.exception.TransportException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * HTTP handler to allow for requesting arbitrary bytes as resource.
 */
public class ByteResourceHandler implements HttpHandler {
    private final byte[] resourceBytes;

    public ByteResourceHandler(byte[] resourceBytes) {
        this.resourceBytes = resourceBytes;
    }

    @Override
    public void process(InputStream inStream, OutputStream outStream, TransportInfo transportInfo)
            throws TransportException {
        try {
            outStream.write(resourceBytes);
        } catch (IOException e) {
            throw new TransportException(e);
        }
    }
}