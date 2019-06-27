package it.org.ieee11073.sdc.dpws.soap;

import it.org.ieee11073.sdc.dpws.SslMetadata;
import org.glassfish.jersey.SslConfigurator;

import javax.net.ssl.HttpsURLConnection;

public class SslSetup {
    public SslSetup() {
        final SslMetadata sslMetadata = new SslMetadata();
        final ClassLoader classLoader = getClass().getClassLoader();
        HttpsURLConnection.setDefaultHostnameVerifier ((hostname, session) -> true);
        classLoader.getResource("it/org/ieee11073/sdc/dpws/TestService1.wsdl").getFile();
        System.setProperty(SslConfigurator.TRUST_STORE_FILE, sslMetadata.getTrustStoreFile());
        System.setProperty(SslConfigurator.KEY_STORE_FILE, sslMetadata.getKeyStoreFile());
        System.setProperty(SslConfigurator.TRUST_STORE_PASSWORD, sslMetadata.getTrustStorePassword());
        System.setProperty(SslConfigurator.KEY_STORE_PASSWORD, sslMetadata.getKeyStorePassword());
    }
}
