package org.somda.sdc.proto.crypto;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.Socket;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * TrustManager which delegates all interactions to the default Java X509ExtendedTrustManager, except it disables
 * the verification of hostnames and ips in certificates.
 */
public class ClientTrustManager extends X509ExtendedTrustManager implements X509TrustManager {

    private X509ExtendedTrustManager trustManager = null;

    public ClientTrustManager(KeyStore trustStore) throws NoSuchAlgorithmException, KeyStoreException {
        var trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        for (final TrustManager manager : trustManagers) {
            if (manager instanceof X509ExtendedTrustManager) {
                trustManager = (X509ExtendedTrustManager) manager;
                return;
            }
        }
        if (trustManager == null) {
            throw new NoSuchAlgorithmException(
                "Could not initialize TrustManager, no X509ExtendedTrustManager base instance found."
            );
        }
    }


    @Override
    public void checkClientTrusted(final X509Certificate[] chain, final String authType, final Socket socket) throws CertificateException {
        trustManager.checkClientTrusted(chain, authType, socket);
    }

    @Override
    public void checkServerTrusted(final X509Certificate[] chain, final String authType, final Socket socket) throws CertificateException {
        trustManager.checkServerTrusted(chain, authType, socket);
    }

    @Override
    public void checkClientTrusted(final X509Certificate[] chain, final String authType, final SSLEngine engine) throws CertificateException {
        trustManager.checkClientTrusted(chain, authType, engine);
    }

    @Override
    public void checkServerTrusted(final X509Certificate[] chain, final String authType, final SSLEngine engine) throws CertificateException {
        // this SHOULD only prevent hostname/ip checks for the certificate
        // TODO LDe: Evaluate the consequences of doing this
        //  It overrides the endpoint identification algorithm inside the ssl engine,
        //  which is the behavior we want for sdc. I cannot in good conscious say that I've
        //  evaluated all the side effects this can have, so take it with a huge grain of salt.
        var param = engine.getSSLParameters();
        param.setEndpointIdentificationAlgorithm(null);
        engine.setSSLParameters(param);
        trustManager.checkServerTrusted(chain, authType, engine);
    }

    @Override
    public void checkClientTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
        trustManager.checkClientTrusted(chain, authType);
    }

    @Override
    public void checkServerTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
        trustManager.checkServerTrusted(chain, authType);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return trustManager.getAcceptedIssuers();
    }
}
