package com.example.consumer1;

import com.example.CustomCryptoSettings;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.biceps.guice.DefaultBicepsConfigModule;
import org.somda.sdc.biceps.guice.DefaultBicepsModule;
import org.somda.sdc.common.guice.DefaultHelperModule;
import org.somda.sdc.dpws.DpwsConfig;
import org.somda.sdc.dpws.crypto.CryptoConfig;
import org.somda.sdc.dpws.crypto.CryptoSettings;
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
 * This class provides the configuration used for the consumer instance.
 * <p>
 * Overwriting configuration steps allows customizing the behavior of the framework through
 * injection.
 */
public class ConsumerUtil {
    private static final Logger LOG = LoggerFactory.getLogger(ConsumerUtil.class);
    private static final String OPT_EPR = "epr";
    private static final String OPT_ADDRESS = "address";
    private static final String OPT_IFACE = "iface";
    private static final String OPT_NO_TLS = "no_tls";
    private static final String OPT_KEYSTORE_PATH = "keystore";
    private static final String OPT_TRUSTSTORE_PATH = "truststore";
    private static final String OPT_KEYSTORE_PASSWORD = "keystore_password";
    private static final String OPT_TRUSTSTORE_PASSWORD = "truststore_password";


    private final Injector injector;
    private final CommandLine parsedArgs;
    private final String epr;
    private final String iface;
    private final boolean useTls;
    private final String address;

    /**
     * Parse command line arguments for epr address and network interface
     *
     * @param args array of arguments, as passed to main
     * @return instance of parsed command line arguments
     */
    public static CommandLine parseCommandLineArgs(String[] args) {
        Options options = new Options();

        {
            Option epr = new Option("e", OPT_EPR, true, "epr address of target provider");
            epr.setRequired(false);
            options.addOption(epr);
        }
        {
            Option networkInterface = new Option("i", OPT_IFACE, true, "network interface to use");
            networkInterface.setRequired(false);
            options.addOption(networkInterface);
        }
//        {
//            Option ipAddress = new Option("a", OPT_ADDRESS, true, "ip address to use");
//            ipAddress.setRequired(false);
//            options.addOption(ipAddress);
//        }
        {
            Option tls = new Option("u", OPT_NO_TLS, false, "disable tls");
            tls.setRequired(false);
            options.addOption(tls);
        }
        {
            Option keyStorePath = new Option("ks", OPT_KEYSTORE_PATH, true, "keystore path");
            keyStorePath.setRequired(false);
            options.addOption(keyStorePath);
        }
        {
            Option trustStorePath = new Option("ts", OPT_TRUSTSTORE_PATH, true, "truststore path");
            trustStorePath.setRequired(false);
            options.addOption(trustStorePath);
        }
        {
            Option keyStorePassword = new Option("ksp", OPT_KEYSTORE_PASSWORD, true, "keystore password");
            keyStorePassword.setRequired(false);
            options.addOption(keyStorePassword);
        }
        {
            Option keystorePath = new Option("tsp", OPT_TRUSTSTORE_PASSWORD, true, "truststore password");
            keystorePath.setRequired(false);
            options.addOption(keystorePath);
        }

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);

            System.exit(1);
        }

        return cmd;
    }

    CryptoSettings createCustomCryptoSettings(CommandLine arguments) {
        var keyPath = arguments.getOptionValue(OPT_KEYSTORE_PATH);
        var trustPath = arguments.getOptionValue(OPT_KEYSTORE_PATH);
        var keyPass = arguments.getOptionValue(OPT_KEYSTORE_PASSWORD);
        var trustPass = arguments.getOptionValue(OPT_TRUSTSTORE_PASSWORD);

        if (keyPath != null && trustPath != null && keyPass != null && trustPass != null) {
            return new CustomCryptoSettings(keyPath, trustPath, keyPass, trustPass);
        }
        return new CustomCryptoSettings();
    }

    public ConsumerUtil(String[] args) {
        this.parsedArgs = parseCommandLineArgs(args);
        this.epr = parsedArgs.getOptionValue(OPT_EPR);
        this.iface = parsedArgs.getOptionValue(OPT_IFACE);
        this.useTls = !parsedArgs.hasOption(OPT_NO_TLS);
        this.address = parsedArgs.getOptionValue(OPT_ADDRESS);

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
                                createCustomCryptoSettings(parsedArgs)
                        );
                        bind(CryptoConfig.CRYPTO_CLIENT_HOSTNAME_VERIFIER,
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
                                                if (keyUri.equals(URI.create(GlueConstants.OID_KEY_PURPOSE_SDC_SERVICE_PROVIDER))) {
                                                    LOG.debug("SDC Service Provider PKP found");
                                                    return true;
                                                }
                                            } catch (IllegalArgumentException e) {
                                                // don't care, was no uri
                                            }
                                        }
                                        return true;
                                    } catch (Exception e) {
                                        LOG.error("Error while validating provider certificate: {}", e.getMessage());
                                        LOG.trace("Error while validating provider certificate", e);
                                    }
                                    return false;
                                });
                        bind(DpwsConfig.HTTPS_SUPPORT, Boolean.class, !isUseTls());
                        bind(DpwsConfig.HTTP_SUPPORT, Boolean.class, isUseTls());
                    }
                });
    }

    public Injector getInjector() {
        return injector;
    }

    public String getEpr() {
        return epr;
    }

    public String getIface() {
        return iface;
    }

    public boolean isUseTls() {
        return useTls;
    }
}
