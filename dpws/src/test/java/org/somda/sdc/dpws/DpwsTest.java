package org.somda.sdc.dpws;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import org.somda.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.somda.sdc.dpws.guice.DefaultDpwsModule;
import org.somda.sdc.common.guice.DefaultHelperModule;
import test.org.somda.common.TestLogging;

import java.util.List;

/**
 * Test base class to provide common test functionality.
 */
public class DpwsTest {
    private Injector injector;
    private List<AbstractModule> overridingModules;

    protected void overrideBindings(AbstractModule module) {
        this.overrideBindings(List.of(module));
    }

    protected void overrideBindings(List<AbstractModule> module) {
        this.overridingModules = module;
    }

    protected void setUp() throws Exception {
        TestLogging.configure();
        if (overridingModules != null) {
            injector = Guice.createInjector(Modules.override(new DefaultDpwsModule(), new DefaultHelperModule(),
                    new DefaultDpwsConfigModule()).with(overridingModules));
        } else {
            injector = Guice.createInjector(new DefaultDpwsModule(), new DefaultHelperModule(), new DefaultDpwsConfigModule());
        }
    }

    protected Injector getInjector() {
        return injector;
    }
}
