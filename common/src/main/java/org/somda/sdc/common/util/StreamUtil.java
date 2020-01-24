package org.somda.sdc.common.util;

import com.google.common.io.ByteStreams;

import java.io.IOException;
import java.io.InputStream;

/**
 * Convert arbitrary input streams to byte arrays.
 */
public class StreamUtil {

    /**
     * Gets bytes from input stream as array.
     *
     * @param inputStream the input stream to convert.
     * @return the converted byte array.
     * @throws IOException if reading from the input stream fails.
     * @see ByteStreams#toByteArray(InputStream)
     * @deprecated This function is a shortcut for a Guava utility ({@link ByteStreams}). Please use the Guava utility
     * instead.
     */
    @Deprecated
    public byte[] getByteArrayFromInputStream(InputStream inputStream) throws IOException {
        return ByteStreams.toByteArray(inputStream);
    }
}
