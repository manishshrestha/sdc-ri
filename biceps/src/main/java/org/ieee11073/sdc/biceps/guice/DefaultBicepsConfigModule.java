package org.ieee11073.sdc.biceps.guice;

import org.ieee11073.sdc.biceps.common.CommonConfig;
import org.ieee11073.sdc.common.guice.AbstractConfigurationModule;

/**
 * Guice module that determines default values for BICEPS package configuration.
 */
public class DefaultBicepsConfigModule extends AbstractConfigurationModule {
    @Override
    public void defaultConfigure() {
        configureCommon();
    }

    private void configureCommon() {
        bind(CommonConfig.MDIB_QUEUE_SIZE,
                Integer.class,
                Integer.MAX_VALUE);

        bind(CommonConfig.COPY_MDIB_INPUT,
                Boolean.class,
                true);

        bind(CommonConfig.COPY_MDIB_OUTPUT,
                Boolean.class,
                true);
    }
}