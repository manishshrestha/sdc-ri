package com.example;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.somda.sdc.dpws.crypto.CryptoSettings;

/**
 * Base utility which provides parsing of command line flags and certain environment variables.
 */
public class BaseUtil {
    private static final String OPT_EPR = "epr";
    private static final String OPT_ADDRESS = "address";
    private static final String OPT_IFACE = "iface";
    private static final String OPT_NO_TLS = "no_tls";
    private static final String OPT_KEYSTORE_PATH = "keystore";
    private static final String OPT_TRUSTSTORE_PATH = "truststore";
    private static final String OPT_KEYSTORE_PASSWORD = "keystore_password";
    private static final String OPT_TRUSTSTORE_PASSWORD = "truststore_password";

    private final CommandLine parsedArgs;
    private String epr;
    private String iface;
    private boolean useTls;
    private String address;

    /**
     * Creates a base utility instance.
     *
     * @param args array of arguments, as passed to main
     */
    public BaseUtil(String[] args) {
        this.parsedArgs = parseCommandLineArgs(args, configureOptions());
        this.epr = parsedArgs.getOptionValue(OPT_EPR);
        this.iface = parsedArgs.getOptionValue(OPT_IFACE);
        this.useTls = !parsedArgs.hasOption(OPT_NO_TLS);
        this.address = parsedArgs.getOptionValue(OPT_ADDRESS);
    }

    /**
     * Configures the available command line flags.
     *
     * @return configured command line flags
     */
    protected Options configureOptions() {
        Options options = new Options();

        {
            Option epr = new Option("e", OPT_EPR, true, "epr address of provider");
            epr.setRequired(false);
            options.addOption(epr);
        }
        {
            Option networkInterface = new Option("i", OPT_IFACE, true, "network interface to use");
            networkInterface.setRequired(false);
            options.addOption(networkInterface);
        }
        {
            Option ipAddress = new Option(
                    "a", OPT_ADDRESS, true,
                    "ip address to bind to. if an adapter has been selected, this will be ignored"
            );
            ipAddress.setRequired(false);
            options.addOption(ipAddress);
        }
        {
            Option tls = new Option("u", OPT_NO_TLS, false, "disable tls");
            tls.setRequired(false);
            options.addOption(tls);
        }
        {
            Option keyStorePath = new Option(null, OPT_KEYSTORE_PATH, true, "keystore path");
            keyStorePath.setRequired(false);
            options.addOption(keyStorePath);
        }
        {
            Option trustStorePath = new Option(null, OPT_TRUSTSTORE_PATH, true, "truststore path");
            trustStorePath.setRequired(false);
            options.addOption(trustStorePath);
        }
        {
            Option keyStorePassword = new Option(null, OPT_KEYSTORE_PASSWORD, true, "keystore password");
            keyStorePassword.setRequired(false);
            options.addOption(keyStorePassword);
        }
        {
            Option keystorePath = new Option(null, OPT_TRUSTSTORE_PASSWORD, true, "truststore password");
            keystorePath.setRequired(false);
            options.addOption(keystorePath);
        }

        return options;
    }

    /**
     * Parses command line arguments, prints a help text and exits on error.
     *
     * @param args array of arguments, as passed to main
     * @return instance of parsed command line arguments
     */
    private CommandLine parseCommandLineArgs(String[] args, Options options) {
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("COMMAND", options);

            System.exit(1);
        }

        return cmd;
    }

    public CryptoSettings createCustomCryptoSettings() {
        var keyPath = this.parsedArgs.getOptionValue(OPT_KEYSTORE_PATH);
        var trustPath = this.parsedArgs.getOptionValue(OPT_KEYSTORE_PATH);
        var keyPass = this.parsedArgs.getOptionValue(OPT_KEYSTORE_PASSWORD);
        var trustPass = this.parsedArgs.getOptionValue(OPT_TRUSTSTORE_PASSWORD);

        if (keyPath != null && trustPath != null && keyPass != null && trustPass != null) {
            return new CustomCryptoSettings(keyPath, trustPath, keyPass, trustPass);
        }
        return new CustomCryptoSettings();
    }

    public CommandLine getParsedArgs() {
        return parsedArgs;
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

    public String getAddress() {
        if (address == null || address.isBlank()) {
            // while command line has priority, we have an env var as a fallback
            return System.getenv().getOrDefault("ref_ip", Constants.DEFAULT_IP);
        }
        return address;
    }

    public void setEpr(String epr) {
        this.epr = epr;
    }

    public void setIface(String iface) {
        this.iface = iface;
    }

    public void setUseTls(boolean useTls) {
        this.useTls = useTls;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
