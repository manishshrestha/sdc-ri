package com.example.provider1;

import com.example.CustomCryptoSettings;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.biceps.guice.DefaultBicepsConfigModule;
import org.somda.sdc.biceps.guice.DefaultBicepsModule;
import org.somda.sdc.biceps.model.participant.AbstractMetricValue;
import org.somda.sdc.biceps.model.participant.GenerationMode;
import org.somda.sdc.biceps.model.participant.MeasurementValidity;
import org.somda.sdc.common.guice.DefaultHelperModule;
import org.somda.sdc.dpws.crypto.CryptoConfig;
import org.somda.sdc.dpws.crypto.CryptoSettings;
import org.somda.sdc.dpws.device.DeviceConfig;
import org.somda.sdc.dpws.guice.DefaultDpwsModule;
import org.somda.sdc.glue.GlueConstants;
import org.somda.sdc.glue.guice.DefaultGlueConfigModule;
import org.somda.sdc.glue.guice.DefaultGlueModule;
import org.somda.sdc.glue.guice.GlueDpwsConfigModule;

import javax.net.ssl.HostnameVerifier;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * This class provides the configuration used for the provider instance.
 * <p>
 * Overwriting configuration steps allows customizing the behavior of the framework through
 * injection.
 */
public class ProviderUtil {
    private static final Logger LOG = LoggerFactory.getLogger(ProviderUtil.class);
    private final Injector injector;

    public ProviderUtil() {
        Configurator.initialize(new DefaultConfiguration());
        Configurator.setRootLevel(Level.INFO);

        injector = Guice.createInjector(
                new DefaultGlueModule(),
                new DefaultGlueConfigModule(),
                new DefaultBicepsModule(),
                new DefaultBicepsConfigModule(),
                new DefaultHelperModule(),
                new DefaultDpwsModule(),
                new GlueDpwsConfigModule() {
                    @Override
                    protected void customConfigure() {
                        super.customConfigure();
                        bind(CryptoConfig.CRYPTO_SETTINGS,
                                CryptoSettings.class,
                                new CustomCryptoSettings()
                        );
                        bind(DeviceConfig.UNSECURED_ENDPOINT, Boolean.class, false);
                        bind(DeviceConfig.SECURED_ENDPOINT, Boolean.class, true);
                        bind(CryptoConfig.CRYPTO_DEVICE_HOSTNAME_VERIFIER,
                                HostnameVerifier.class,
                                (hostname, session) -> {
                                    try {
                                        // since this is not a real implementation, we still want to allow all peers
                                        // which is why this doesn't really filter anything
                                        // returning false in this filter would reject an incoming request
                                        var peerCerts = session.getPeerCertificates();
                                        final X509Certificate x509 = (X509Certificate) peerCerts[0];
                                        List<String> extendedKeyUsage = x509.getExtendedKeyUsage();
                                        if (extendedKeyUsage == null || extendedKeyUsage.isEmpty()) {
                                            LOG.warn("No EKU in peer certificate");
                                            return true;
                                        }

                                        // find matching provider key purpose
                                        for (String key : extendedKeyUsage) {
                                            try {
                                                URI keyUri = URI.create(key);
                                                if (keyUri.equals(GlueConstants.URI_KEY_PURPOSE_SDC_SERVICE_CONSUMER)) {
                                                    LOG.debug("SDC Service Consumer PKP found");
                                                    return true;
                                                }
                                            } catch (IllegalArgumentException e) {
                                                // don't care, was no uri
                                            }
                                        }
                                        return true;
                                    } catch (Exception e) {
                                        LOG.error("Error while validating client certificate: {}", e.getMessage());
                                        LOG.trace("Error while validating client certificate", e);
                                    }
                                    return false;
                                });

                    }
                });
    }

    public Injector getInjector() {
        return injector;
    }

    public static void addMetricQualityDemo(AbstractMetricValue val) {
        if (val.getMetricQuality() == null) {
            var qual = new AbstractMetricValue.MetricQuality();
            qual.setMode(GenerationMode.DEMO);
            qual.setValidity(MeasurementValidity.VLD);
            val.setMetricQuality(qual);
        }
    }
}
