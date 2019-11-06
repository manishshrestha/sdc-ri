package org.ieee11073.sdc.glue;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import org.ieee11073.sdc.biceps.guice.DefaultBicepsConfigModule;
import org.ieee11073.sdc.biceps.guice.DefaultBicepsModule;
import org.ieee11073.sdc.common.guice.AbstractConfigurationModule;
import org.ieee11073.sdc.common.guice.DefaultHelperModule;
import org.ieee11073.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.ieee11073.sdc.dpws.guice.DefaultDpwsModule;
import org.ieee11073.sdc.glue.guice.DefaultGlueConfigModule;
import org.ieee11073.sdc.glue.guice.DefaultGlueModule;

public class UnitTestUtil {
    private final Injector injector;

    public UnitTestUtil() {
        injector = Guice.createInjector(
                new DefaultGlueModule(),
                new DefaultGlueConfigModule(),
                new DefaultBicepsModule(),
                new DefaultBicepsConfigModule(),
                new DefaultHelperModule(),
                new DefaultDpwsModule(),
                new DefaultDpwsConfigModule());
    }

    public Injector getInjector() {
        return injector;
    }

    public Injector createInjectorWithOverrides(AbstractModule overridingModule) {
        return Guice.createInjector(Modules.override(
                new DefaultBicepsModule(),
                new DefaultBicepsConfigModule(),
                new DefaultHelperModule()).with(overridingModule));
    }
}
