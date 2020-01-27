package test.org.somda.common;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;

import java.io.File;

/**
 * Utility class with static logging configuration for tests.
 */
public class TestLogging {
    /**
     * Configures a default logging valid for all tests.
     */

    public static void configure() {
        var cfg = new DefaultConfiguration();

        // this will create a logfile called ConsoleOutput.txt in every module being tested
        var file = new File(System.getProperty("user.dir"), "ConsoleOutput.txt");
        FileAppender fa = FileAppender.newBuilder()
                .setName("fileAppender")
                .withAppend(true)
                .withFileName(file.toString())
                .setConfiguration(cfg)
                .build();
        cfg.addAppender(fa);

        Configurator.initialize(cfg);
        Configurator.setRootLevel(Level.DEBUG);
    }
}
