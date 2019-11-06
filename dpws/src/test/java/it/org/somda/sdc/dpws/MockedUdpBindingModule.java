package it.org.ieee11073.sdc.dpws;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.ieee11073.sdc.dpws.udp.UdpBindingService;
import org.ieee11073.sdc.dpws.udp.factory.UdpBindingServiceFactory;

public class MockedUdpBindingModule extends AbstractModule {
    @Override
    protected void configure() {
        install(new FactoryModuleBuilder()
                .implement(UdpBindingService.class, UdpBindingServiceMock.class)
                .build(UdpBindingServiceFactory.class));
    }
}
