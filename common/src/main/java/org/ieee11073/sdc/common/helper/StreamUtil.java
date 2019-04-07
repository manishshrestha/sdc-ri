package org.ieee11073.sdc.common.helper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility class to convert an arbitrary input stream to a byte array.
 */
public class StreamUtil {

    /**
     * Get bytes from input stream as byte array.
     *
     * @throws IOException If reading from the input stream fails.
     */
    public byte[] getByteArrayFromInputStream(InputStream inputStream) throws IOException {
        byte[] buf = new byte[4096];
        int bytesRead;
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        while ((bytesRead = inputStream.read(buf)) != -1) {
            bao.write(buf, 0, bytesRead);
        }
        return bao.toByteArray();
    }
}
