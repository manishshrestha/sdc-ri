package org.ieee11073.sdc.dpws;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import org.apache.log4j.BasicConfigurator;
import org.ieee11073.sdc.dpws.guice.DpwsConfigModule;
import org.ieee11073.sdc.dpws.guice.DpwsModule;
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
        BasicConfigurator.configure();
        if (overridingModule != null) {
            injector = Guice.createInjector(Modules.override(new DpwsModule(), new DefaultHelperModule(),
                    new DpwsConfigModule()).with(overridingModule));
        } else {
            injector = Guice.createInjector(new DpwsModule(), new DefaultHelperModule(), new DpwsConfigModule());
        }
    }

    protected Injector getInjector() {
        return injector;
    }
}
