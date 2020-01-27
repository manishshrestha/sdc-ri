package test.org.somda.common;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;

/**
 * Utility class with static logging configuration for tests.
 */
public class TestLogging {
    /**
     * Configures a default logging valid for all tests.
     */

    public static void configure() {
        Configurator.initialize(new DefaultConfiguration());
        Configurator.setRootLevel(Level.DEBUG);
        // silence the apache wire log output, it's too much to handle in CI
        Configurator.setAllLevels("org.apache.http.wire", Level.INFO);
    }
}
