package it.org.somda.sdc.proto.example1;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.glue.consumer.helper.HostingServiceLogger;

import java.util.List;

public class BaseUtil {
    private static final List<String> CHATTY_LOGGERS = List.of(
            "org.apache.http.wire",
            "org.apache.http.headers",
            "org.eclipse.jetty"
    );

    private static final String CUSTOM_PATTERN = "%d{HH:mm:ss.SSS}"
            + " [%thread]"
            // only include the space if we have a variable for these
            + " %notEmpty{[%X{" + InstanceLogger.INSTANCE_ID + "}] }"
            + " %notEmpty{[%X{" + HostingServiceLogger.HOSTING_SERVICE_INFO + "}] }"
            + "%-5level"
            + " %logger{36}"
            + " - %msg%n";


    protected static BuiltConfiguration localLoggerConfig(Level consoleLevel) {
        ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
        builder.setStatusLevel(Level.ERROR);
        builder.setConfigurationName("LocalLogging");

        var layoutBuilder = builder
                .newLayout("PatternLayout")
                .addAttribute("pattern", CUSTOM_PATTERN);

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
                            .addAttribute("level", consoleLevel)
            );
            builder.add(appenderBuilder);

            rootLogger.add(builder.newAppenderRef(appenderBuilder.getName()));
        }
        {
            // quiet down chatty loggers
            CHATTY_LOGGERS.forEach(logger -> {
                builder.add(builder.newLogger(logger, Level.INFO)
                        .addAttribute("additivity", true));
            });
        }

        builder.add(rootLogger);
        return builder.build();
    }
}
