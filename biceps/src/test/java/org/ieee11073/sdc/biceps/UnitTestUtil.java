package org.ieee11073.sdc.biceps;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.ieee11073.sdc.biceps.guice.DefaultBicepsConfigModule;
import org.ieee11073.sdc.biceps.guice.DefaultBicepsModule;
import org.ieee11073.sdc.common.guice.DefaultHelperModule;

public class UnitTestUtil {
    private final Injector injector;

    public UnitTestUtil() {
        injector = Guice.createInjector(
                new DefaultBicepsModule(),
                new DefaultBicepsConfigModule(),
                new DefaultHelperModule());
    }

    public Injector getInjector() {
        return injector;
    }
}
