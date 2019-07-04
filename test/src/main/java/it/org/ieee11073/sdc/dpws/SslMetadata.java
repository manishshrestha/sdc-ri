package it.org.ieee11073.sdc.dpws;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * SSL metadata used for crypto related tests.
 */
public class SslMetadata {
    final File trustStoreFile;
    final File keyStoreFile;
    final String trustStorePassword;
    final String keyStorePassword;

    public SslMetadata() throws IOException {
        final ClassLoader classLoader = getClass().getClassLoader();

        trustStoreFile = new File("truststore.jks");
        keyStoreFile = new File("keystore.jks");
        String test = trustStoreFile.getAbsolutePath();
        copy(classLoader.getResourceAsStream("it/org/ieee11073/sdc/dpws/truststore.jks"), trustStoreFile);
        copy(classLoader.getResourceAsStream("it/org/ieee11073/sdc/dpws/keystore.jks"), keyStoreFile);

        trustStorePassword = "whatever";
        keyStorePassword = "whatever";
    }

    public String getTrustStoreFile() {
        return trustStoreFile.getAbsolutePath();
    }

    public String getKeyStoreFile() {
        return keyStoreFile.getAbsolutePath();
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    private static void copy(final InputStream is, final File f) throws IOException {
        final byte[] buf = new byte[4096];
        FileOutputStream os = new FileOutputStream(f);
        int len = 0;
        while ((len = is.read(buf)) > 0) {
            os.write(buf, 0, len);
        }
        is.close();
        os.close();
    }
}
