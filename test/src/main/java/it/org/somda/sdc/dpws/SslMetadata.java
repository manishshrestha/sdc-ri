package it.org.ieee11073.sdc.dpws;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.Service;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V3CertificateGenerator;

import javax.annotation.Nullable;
import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * SSL metadata used for crypto related tests.
 * <p>
 * Provides key stores and trust stores for client and device side in-memory.
 *
 * Code derived from http://www.bouncycastle.org/documentation.html and https://www.baeldung.com/java-keystore
 */
public class SslMetadata extends AbstractIdleService implements Service {
    @Nullable
    private KeySet serverKeySet;
    @Nullable
    private KeySet clientKeySet;

    public SslMetadata() {
        serverKeySet = null;
        clientKeySet = null;
    }

    @Override
    protected void startUp() throws
            InvalidKeyException,
            NoSuchProviderException,
            SignatureException,
            NoSuchAlgorithmException,
            CertificateException,
            KeyStoreException,
            IOException {

        Security.addProvider(new BouncyCastleProvider());

        final String commonPassword = "secret";

        final KeyPurposeId[] ekuId = {KeyPurposeId.id_kp_serverAuth, KeyPurposeId.id_kp_clientAuth};
        final ExtendedKeyUsage extendedKeyUsage = new ExtendedKeyUsage(ekuId);

        final String serverAlias = "test-device";
        final String clientAlias = "test-client";

        final KeyPair serverKeyPair = generateKeyPair();
        final KeyPair clientKeyPair = generateKeyPair();

        final X509Certificate serverCert = generateCertificate("sdc-lite-server.org", serverKeyPair, extendedKeyUsage);
        final X509Certificate clientCert = generateCertificate("sdc-lite-client.org", clientKeyPair, extendedKeyUsage);

        final KeyStore serverKeyStore = createKeyStore(serverAlias, serverKeyPair.getPrivate(), commonPassword, Collections.singletonList(serverCert));
        final KeyStore clientKeyStore = createKeyStore(clientAlias, clientKeyPair.getPrivate(), commonPassword, Collections.singletonList(clientCert));

        final KeyStore serverTrustStore = createTrustStore(serverAlias, commonPassword, clientCert);
        final KeyStore clientTrustStore = createTrustStore(clientAlias, commonPassword, serverCert);

        serverKeySet = new KeySet(serverKeyStore, commonPassword, serverTrustStore, commonPassword);
        clientKeySet = new KeySet(clientKeyStore, commonPassword, clientTrustStore, commonPassword);
    }

    @Override
    protected void shutDown() {

    }

    public KeySet getServerKeySet() {
        return serverKeySet;
    }

    public KeySet getClientKeySet() {
        return clientKeySet;
    }

    private static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(1024);
        return keyPairGenerator.generateKeyPair();
    }

    private static KeyStore createKeyStore(String alias,
                                           PrivateKey privateKey,
                                           String password,
                                           List<X509Certificate> certificateChain)
            throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {

        final KeyStore keyStore = KeyStore.getInstance("jks");
        keyStore.load(null, password.toCharArray());

        final X509Certificate[] certificates = certificateChain.toArray(new X509Certificate[0]);
        keyStore.setKeyEntry(alias, privateKey, password.toCharArray(), certificates);
        return keyStore;
    }

    private static KeyStore createTrustStore(String alias,
                                             String password,
                                             X509Certificate trustedCertificate)
            throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {

        final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, password.toCharArray());
        keyStore.setCertificateEntry(alias, trustedCertificate);
        return keyStore;
    }

    private static X509Certificate generateCertificate(String issuer, KeyPair keyPair, ExtendedKeyUsage extendedKeyUsage)
            throws InvalidKeyException, NoSuchProviderException, SignatureException {

        /*
         * DGr 2019-07-06
         * At the time implementing the certificate generation, there was no reasonable implementation found online that
         * didn't use deprecated functionality.
         */

        // generate the certificate
        X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();

        certGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
        certGen.setIssuerDN(new X500Principal("CN=" + issuer));
        certGen.setNotBefore(new Date(System.currentTimeMillis() - 500000));
        certGen.setNotAfter(new Date(System.currentTimeMillis() + 500000));
        certGen.setSubjectDN(new X500Principal("CN=" + issuer));
        certGen.setPublicKey(keyPair.getPublic());
        certGen.setSignatureAlgorithm("SHA256WithRSAEncryption");

        certGen.addExtension(X509Extensions.BasicConstraints, true, new BasicConstraints(false));
        certGen.addExtension(X509Extensions.KeyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
        certGen.addExtension(X509Extensions.SubjectAlternativeName, false, new GeneralNames(new GeneralName(GeneralName.rfc822Name, "david.gregorczyk@web.de")));
        certGen.addExtension(X509Extensions.ExtendedKeyUsage, true, extendedKeyUsage);

        return certGen.generateX509Certificate(keyPair.getPrivate(), "BC");
    }

    public class KeySet {
        final KeyStore keyStore;
        final String keyStorePassword;

        final KeyStore trustStore;
        final String trustStorePassword;

        public KeySet(KeyStore keyStore, String keyStorePassword, KeyStore trustStore, String trustStorePassword) {
            this.keyStore = keyStore;
            this.keyStorePassword = keyStorePassword;
            this.trustStore = trustStore;
            this.trustStorePassword = trustStorePassword;
        }

        public KeyStore getKeyStore() {
            return keyStore;
        }

        public String getKeyStorePassword() {
            return keyStorePassword;
        }

        public KeyStore getTrustStore() {
            return trustStore;
        }

        public String getTrustStorePassword() {
            return trustStorePassword;
        }
    }
}

/*
    Snippets for file based key store and trust store loading
 */
//        final ClassLoader classLoader = getClass().getClassLoader();
//        trustStoreFile = new File("truststore.jks");
//        keyStoreFile = new File("keystore.jks");
//        String test = trustStoreFile.getAbsolutePath();
//        copy(classLoader.getResourceAsStream("it/org/ieee11073/sdc/dpws/truststore.jks"), trustStoreFile);
//        copy(classLoader.getResourceAsStream("it/org/ieee11073/sdc/dpws/keystore.jks"), keyStoreFile);
//
//        trustStorePassword = "whatever";
//        keyStorePassword = "whatever";

//    private static void copy(final InputStream is, final File f) throws IOException {
//        final byte[] buf = new byte[4096];
//        FileOutputStream os = new FileOutputStream(f);
//        int len = 0;
//        while ((len = is.read(buf)) > 0) {
//            os.write(buf, 0, len);
//        }
//        is.close();
//        os.close();
//    }