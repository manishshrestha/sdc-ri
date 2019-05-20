package it.org.ieee11073.sdc.dpws;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.ieee11073.sdc.common.guice.DefaultHelperModule;
import org.ieee11073.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.ieee11073.sdc.dpws.guice.DefaultDpwsModule;
import org.ieee11073.sdc.dpws.soap.SoapConfig;

public abstract class IntegrationTestPeer extends AbstractExecutionThreadService {
    private Injector injector;

    public IntegrationTestPeer() {
        this.injector = Guice.createInjector(
                new DefaultDpwsModule(),
                new DefaultHelperModule(),
                new DefaultDpwsConfigModule() {
                    @Override
                    protected void customConfigure() {
                        bind(SoapConfig.JAXB_CONTEXT_PATH, String.class,
                                TestServiceMetadata.JAXB_CONTEXT_PATH);
                    }
                });
    }

    public Injector getInjector() {
        return injector;
    }
}
