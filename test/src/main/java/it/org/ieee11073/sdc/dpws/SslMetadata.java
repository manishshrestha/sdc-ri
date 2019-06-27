package it.org.ieee11073.sdc.dpws;

public class SslMetadata {
    final String trustStoreFile;
    final String keyStoreFile;
    final String trustStorePassword;
    final String keyStorePassword;

    public SslMetadata() {
        final ClassLoader classLoader = getClass().getClassLoader();
        classLoader.getResource("it/org/ieee11073/sdc/dpws/TestService1.wsdl").getFile();

        trustStoreFile =  classLoader.getResource("it/org/ieee11073/sdc/dpws/truststore.jks").getFile();
        keyStoreFile =  classLoader.getResource("it/org/ieee11073/sdc/dpws/keystore.jks").getFile();
        trustStorePassword = "whatever";
        keyStorePassword = "whatever";
    }

    public String getTrustStoreFile() {
        return trustStoreFile;
    }

    public String getKeyStoreFile() {
        return keyStoreFile;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }
}
