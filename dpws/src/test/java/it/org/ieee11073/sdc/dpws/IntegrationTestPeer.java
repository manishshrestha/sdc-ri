package it.org.ieee11073.sdc.dpws;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.ieee11073.sdc.common.guice.DefaultHelperModule;
import org.ieee11073.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.ieee11073.sdc.dpws.guice.DefaultDpwsModule;

public abstract class IntegrationTestPeer extends AbstractIdleService {
    private Injector injector;

    protected void setupInjector(DefaultDpwsConfigModule configModule) {
        this.injector = Guice.createInjector(
                new DefaultDpwsModule(),
                new DefaultHelperModule(),
                configModule);
    }

    public Injector getInjector() {
        if (injector == null) {
            throw new RuntimeException("Call setupInjector() before getting injector");
        }
        return injector;
    }
}
