package test.org.somda.common;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

import java.io.File;
import java.util.List;

/**
 * Utility class with static logging configuration for tests.
 */
public class TestLogging {

    private static List<String> chattyLoggers = List.of(
            "org.apache.http.wire",
            "org.apache.http.headers",
            "org.eclipse.jetty"
    );

    private static BuiltConfiguration testConfiguration() {
        ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
        builder.setStatusLevel(Level.ERROR);
        builder.setConfigurationName("TestLogging");

        var layoutBuilder = builder
                .newLayout("PatternLayout")
                .addAttribute("pattern", DefaultConfiguration.DEFAULT_PATTERN);

        var rootLogger = builder.newRootLogger(Level.DEBUG);

        {
            // create a console appender
            var appenderBuilder = builder
                    .newAppender("console_logger", ConsoleAppender.PLUGIN_NAME)
                    .addAttribute("target", ConsoleAppender.Target.SYSTEM_OUT);
            appenderBuilder.add(layoutBuilder);
            // only log WARN or worse to console
            appenderBuilder.addComponent(
                    builder.newFilter("ThresholdFilter", Filter.Result.ACCEPT, Filter.Result.DENY)
                            .addAttribute("level", Level.WARN)
            );
            builder.add(appenderBuilder);

            rootLogger.add(builder.newAppenderRef(appenderBuilder.getName()));
        }
        {
            // create a file appender
            var appenderBuilder = builder.newAppender("file", "File")
                    .addAttribute("fileName", "target/test.log")
                    .addAttribute("append", true)
                    .add(layoutBuilder);
            builder.add(appenderBuilder);

            rootLogger.add(builder.newAppenderRef(appenderBuilder.getName()));
        }
        {
            // quiet down chatty loggers
            chattyLoggers.forEach(logger -> {
                builder.add(builder.newLogger(logger, Level.INFO)
                        .addAttribute("additivity", true));
            });
        }

        builder.add(rootLogger);
        return builder.build();
    }

    /**
     * Configures a default logging valid for all tests.
     */
    public static void configure() {
        if (CIDetector.isRunningInCi()) {
            Configurator.initialize(testConfiguration());
        } else {
            Configurator.initialize(new DefaultConfiguration());
            Configurator.setRootLevel(Level.DEBUG);
            // silence the apache httpclient and jetty log output a little
            Configurator.setAllLevels("org.apache.http.wire", Level.INFO);
            Configurator.setAllLevels("org.apache.http.headers", Level.INFO);
            Configurator.setAllLevels("org.eclipse.jetty", Level.INFO);
        }
    }
}
