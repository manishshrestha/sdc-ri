package org.somda.sdc.glue.guice;

import org.somda.sdc.common.guice.AbstractConfigurationModule;

/**
 * Guice module that determines default values for the Glue package configuration.
 */
public class DefaultGlueConfigModule extends AbstractConfigurationModule {
    @Override
    public void defaultConfigure() {
        configureCommon();
    }

    private void configureCommon() {
//        bind(CommonConfig.COPY_MDIB_INPUT,
//                Boolean.class,
//                true);
//
//        bind(CommonConfig.COPY_MDIB_OUTPUT,
//                Boolean.class,
//                true);
    }
}