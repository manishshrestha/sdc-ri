package org.ieee11073.sdc.dpws;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.ieee11073.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.ieee11073.sdc.dpws.guice.DefaultDpwsModule;
import org.ieee11073.sdc.common.guice.DefaultHelperModule;

/**
 * Test base class to provide common test functionality.
 */
public class DpwsTest {
    private Injector injector;
    private AbstractModule overridingModule;

    protected void overrideBindings(AbstractModule module) {
        this.overridingModule = module;
    }

    protected void setUp() throws Exception {
        Configurator.initialize(new DefaultConfiguration());
        Configurator.setRootLevel(Level.DEBUG);
        if (overridingModule != null) {
            injector = Guice.createInjector(Modules.override(new DefaultDpwsModule(), new DefaultHelperModule(),
                    new DefaultDpwsConfigModule()).with(overridingModule));
        } else {
            injector = Guice.createInjector(new DefaultDpwsModule(), new DefaultHelperModule(), new DefaultDpwsConfigModule());
        }
    }

    protected Injector getInjector() {
        return injector;
    }
}
