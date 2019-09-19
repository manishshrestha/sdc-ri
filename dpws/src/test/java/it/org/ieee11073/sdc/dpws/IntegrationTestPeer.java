package it.org.ieee11073.sdc.dpws;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import org.ieee11073.sdc.common.guice.DefaultHelperModule;
import org.ieee11073.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.ieee11073.sdc.dpws.guice.DefaultDpwsModule;

import javax.annotation.Nullable;

public abstract class IntegrationTestPeer extends AbstractIdleService {
    private Injector injector;

    protected void setupInjector(DefaultDpwsConfigModule configModule, @Nullable AbstractModule overridingModule) {
        if (overridingModule != null) {
            injector = Guice.createInjector(Modules.override(new DefaultDpwsModule(), new DefaultHelperModule(),
                    configModule).with(overridingModule));
        } else {
            injector = Guice.createInjector(new DefaultDpwsModule(), new DefaultHelperModule(), configModule);
        }
    }

    protected void setupInjector(DefaultDpwsConfigModule configModule) {
        setupInjector(configModule, null);
    }

    public Injector getInjector() {
        if (injector == null) {
            throw new RuntimeException("Call setupInjector() before getting injector");
        }
        return injector;
    }
}
