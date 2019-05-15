package it.org.ieee11073.sdc.dpws;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.ieee11073.sdc.common.guice.DefaultHelperModule;
import org.ieee11073.sdc.dpws.device.Device;
import org.ieee11073.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.ieee11073.sdc.dpws.guice.DefaultDpwsModule;
import org.ieee11073.sdc.dpws.soap.SoapConfig;
import org.ieee11073.sdc.dpws.soap.wsaddressing.WsAddressingConfig;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class IntegrationTestPeer extends AbstractExecutionThreadService {
    private Injector injector;
    private Set<Service> services;
    private ServiceManager manager;

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
        this.services = new HashSet<>();
    }

    public Injector getInjector() {
        return injector;
    }

    public void addService(Service service) {
        this.services.add(service);
    }

    @Override
    protected void startUp() {

    }

    @Override
    protected void run() throws Exception {
        this.manager = new ServiceManager(services);
        this.manager.startAsync().awaitHealthy();
        while (!Thread.interrupted()) {
            Thread.sleep(1000);
        }
    }

    @Override
    protected void shutDown() {
        if (this.manager != null) {
            this.manager.stopAsync().awaitStopped();
        }
    }
}
