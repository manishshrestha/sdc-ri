package org.somda.sdc.glue.guice;

import org.somda.sdc.common.guice.AbstractConfigurationModule;
import org.somda.sdc.glue.common.CommonConfig;
import org.somda.sdc.glue.consumer.ConsumerConfig;

import java.time.Duration;

/**
 * Guice module that determines default values for the Glue package configuration.
 */
public class DefaultGlueConfigModule extends AbstractConfigurationModule {
    @Override
    public void defaultConfigure() {
        configureCommon();
        configureConsumer();
    }

    private void configureCommon() {
        bind(CommonConfig.NAMESPACE_MAPPINGS,
                String.class,
                "");
    }

    private void configureConsumer() {
        bind(ConsumerConfig.WATCHDOG_PERIOD,
                Duration.class,
                Duration.ofMillis(5000));
    }
}