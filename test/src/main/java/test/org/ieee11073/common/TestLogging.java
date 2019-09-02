package test.org.ieee11073.common;

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
    }
}