package org.ieee11073.sdc.dpws.device.helper;

import org.ieee11073.sdc.dpws.http.HttpHandler;
import org.ieee11073.sdc.dpws.soap.TransportInfo;
import org.ieee11073.sdc.dpws.soap.exception.MarshallingException;
import org.ieee11073.sdc.dpws.soap.exception.TransportException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * HTTP handler that facilitates responding with arbitrary byte sequences.
 */
public class ByteResourceHandler implements HttpHandler {
    private final byte[] resourceBytes;

    /**
     * Constructor.
     *
     * @param resourceBytes the bytes that are supposed to be returned on any incoming network request.
     */
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