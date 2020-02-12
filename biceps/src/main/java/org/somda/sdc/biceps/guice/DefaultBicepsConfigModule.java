package org.somda.sdc.biceps.guice;

import org.somda.sdc.biceps.common.CommonConfig;
import org.somda.sdc.common.guice.AbstractConfigurationModule;

/**
 * Guice module that determines default values for the BICEPS package configuration.
 */
public class DefaultBicepsConfigModule extends AbstractConfigurationModule {
    @Override
    public void defaultConfigure() {
        configureCommon();
    }

    private void configureCommon() {
        bind(CommonConfig.COPY_MDIB_INPUT,
                Boolean.class,
                true);

        bind(CommonConfig.COPY_MDIB_OUTPUT,
                Boolean.class,
                true);
    }
}