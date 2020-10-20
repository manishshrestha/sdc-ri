package org.somda.sdc.biceps.common.preprocessing;

import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * Wrapper class to pass the injector in PreprocessingUtil without the warning.
 */
public class PreprocessingInjectorWrapper {

    private final Injector injector;

    @Inject
    public PreprocessingInjectorWrapper(Injector injector) {
        this.injector = injector;
    }

    public Injector getInjector() {
        return injector;
    }
}
