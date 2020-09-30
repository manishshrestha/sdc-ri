package org.somda.sdc.biceps.guice;

import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import org.somda.sdc.biceps.common.CommonConfig;
import org.somda.sdc.biceps.common.storage.StatePreprocessingSegment;
import org.somda.sdc.biceps.consumer.preprocessing.DuplicateContextStateHandleHandler;
import org.somda.sdc.biceps.consumer.preprocessing.VersionDuplicateHandler;
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

        bind(CommonConfig.STORE_NOT_ASSOCIATED_CONTEXT_STATES,
                Boolean.class,
                false);

        bind(CommonConfig.ALLOW_STATES_WITHOUT_DESCRIPTORS,
                Boolean.class,
                true);

        Multibinder<StatePreprocessingSegment> consumerPreProcessingSegments = Multibinder.newSetBinder(
                binder(), StatePreprocessingSegment.class, Names.named(CommonConfig.CONSUMER_PREPROCESSING_SEGMENTS));
        consumerPreProcessingSegments.addBinding().to(DuplicateContextStateHandleHandler.class);
        consumerPreProcessingSegments.addBinding().to(VersionDuplicateHandler.class);
    }
}