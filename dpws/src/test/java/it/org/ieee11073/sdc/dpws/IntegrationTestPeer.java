package it.org.ieee11073.sdc.dpws;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.ieee11073.sdc.common.guice.DefaultHelperModule;
import org.ieee11073.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.ieee11073.sdc.dpws.guice.DefaultDpwsModule;
import org.ieee11073.sdc.dpws.soap.SoapConfig;

public abstract class IntegrationTestPeer extends AbstractIdleService {
    private Injector injector;

    public IntegrationTestPeer(DefaultDpwsConfigModule configModule) {
        this.injector = Guice.createInjector(
                new DefaultDpwsModule(),
                new DefaultHelperModule(),
                configModule);
    }

    public Injector getInjector() {
        return injector;
    }
}
