package org.ieee11073.sdc.dpws.guice;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.ieee11073.sdc.dpws.DpwsFramework;
import org.ieee11073.sdc.dpws.DpwsFrameworkImpl;
import org.ieee11073.sdc.dpws.client.Client;
import org.ieee11073.sdc.dpws.client.ClientImpl;
import org.ieee11073.sdc.dpws.client.helper.*;
import org.ieee11073.sdc.dpws.client.helper.factory.ClientHelperFactory;
import org.ieee11073.sdc.dpws.device.Device;
import org.ieee11073.sdc.dpws.device.DeviceImpl;
import org.ieee11073.sdc.dpws.device.helper.DiscoveryDeviceUdpMessageProcessor;
import org.ieee11073.sdc.dpws.device.helper.factory.DeviceHelperFactory;
import org.ieee11073.sdc.dpws.factory.DpwsFrameworkFactory;
import org.ieee11073.sdc.dpws.factory.TransportBindingFactory;
import org.ieee11073.sdc.dpws.factory.TransportBindingFactoryImpl;
import org.ieee11073.sdc.dpws.helper.NotificationSourceUdpCallback;
import org.ieee11073.sdc.dpws.helper.factory.DpwsHelperFactory;
import org.ieee11073.sdc.dpws.http.HttpServerRegistry;
import org.ieee11073.sdc.dpws.http.grizzly.GrizzlyHttpServerRegistry;
import org.ieee11073.sdc.dpws.ni.LocalAddressResolver;
import org.ieee11073.sdc.dpws.ni.LocalAddressResolverImpl;
import org.ieee11073.sdc.dpws.service.*;
import org.ieee11073.sdc.dpws.service.factory.HostedServiceFactory;
import org.ieee11073.sdc.dpws.service.factory.HostedServiceInterceptorFactory;
import org.ieee11073.sdc.dpws.service.factory.HostedServiceTransportBindingFactory;
import org.ieee11073.sdc.dpws.service.factory.HostingServiceFactory;
import org.ieee11073.sdc.dpws.soap.*;
import org.ieee11073.sdc.dpws.soap.factory.NotificationSourceFactory;
import org.ieee11073.sdc.dpws.soap.factory.RequestResponseClientFactory;
import org.ieee11073.sdc.dpws.soap.factory.SoapMessageFactory;
import org.ieee11073.sdc.dpws.soap.wsaddressing.WsAddressingClientInterceptor;
import org.ieee11073.sdc.dpws.soap.wsaddressing.WsAddressingServerInterceptor;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.*;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.factory.WsDiscoveryClientFactory;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.factory.WsDiscoveryTargetServiceFactory;
import org.ieee11073.sdc.dpws.soap.wseventing.*;
import org.ieee11073.sdc.dpws.soap.wseventing.factory.NotificationWorkerFactory;
import org.ieee11073.sdc.dpws.soap.wseventing.factory.SubscriptionManagerFactory;
import org.ieee11073.sdc.dpws.soap.wseventing.factory.WsEventingEventSinkFactory;
import org.ieee11073.sdc.dpws.soap.wseventing.helper.NotificationWorker;
import org.ieee11073.sdc.dpws.soap.wsmetadataexchange.GetMetadataClient;
import org.ieee11073.sdc.dpws.soap.wsmetadataexchange.GetMetadataClientImpl;
import org.ieee11073.sdc.dpws.soap.wstransfer.TransferGetClient;
import org.ieee11073.sdc.dpws.soap.wstransfer.TransferGetClientImpl;
import org.ieee11073.sdc.dpws.udp.UdpBindingService;
import org.ieee11073.sdc.dpws.udp.UdpBindingServiceImpl;
import org.ieee11073.sdc.dpws.udp.UdpMessageQueueService;
import org.ieee11073.sdc.dpws.udp.UdpMessageQueueServiceImpl;
import org.ieee11073.sdc.dpws.udp.factory.UdpBindingServiceFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Default Guice module to bind all interfaces and factories used by the DPWS implementation.
 */
public class DefaultDpwsModule extends AbstractModule {
    @Override
    protected void configure() {
        configureMarshalling();
        configureUdp();
        configureThreadPools();
        configureHttpServer();
        configureSoapEngine();
        configureWsAddressing();
        configureWsDiscovery();
        configureWsEventing();
        configureWsMetadataExchange();
        configureWsTransfer();

        configureDevice();
        configureClient();

        configureService();

        install(new FactoryModuleBuilder()
                .implement(NotificationSourceUdpCallback.class, NotificationSourceUdpCallback.class)
                .build(DpwsHelperFactory.class));

        install(new FactoryModuleBuilder()
                .implement(DpwsFramework.class, DpwsFrameworkImpl.class)
                .build(DpwsFrameworkFactory.class));

        bind(TransportBindingFactory.class)
                .to(TransportBindingFactoryImpl.class);


        install(new FactoryModuleBuilder()
                .implement(UdpBindingService.class, UdpBindingServiceImpl.class)
                .build(UdpBindingServiceFactory.class));
    }

    private void configureService() {
        install(new FactoryModuleBuilder()
                .implement(HostingService.class, HostingServiceInterceptor.class)
                .implement(HostingServiceProxy.class, HostingServiceProxyImpl.class)
                .build(HostingServiceFactory.class));

        install(new FactoryModuleBuilder()
                .implement(HostedService.class, HostedServiceImpl.class)
                .implement(HostedServiceProxy.class, HostedServiceProxyImpl.class)
                .build(HostedServiceFactory.class));

        install(new FactoryModuleBuilder()
                .implement(HostedServiceInterceptor.class, HostedServiceInterceptor.class)
                .build(HostedServiceInterceptorFactory.class));

        install(new FactoryModuleBuilder()
                .implement(HostedServiceTransportBinding.class, HostedServiceTransportBinding.class)
                .build(HostedServiceTransportBindingFactory.class));
    }

    private void configureClient() {
        bind(Client.class)
                .to(ClientImpl.class);

        bind(ScheduledExecutorService.class)
                .annotatedWith(WatchDogScheduler.class)
                .toProvider(() -> Executors.newScheduledThreadPool(20));

        install(new FactoryModuleBuilder()
                .implement(DiscoveryClientUdpProcessor.class, DiscoveryClientUdpProcessor.class)
                .implement(DiscoveredDeviceResolver.class, DiscoveredDeviceResolver.class)
                .implement(HelloByeAndProbeMatchesObserverImpl.class, HelloByeAndProbeMatchesObserverImpl.class)
                .implement(WatchDog.class, WatchDogImpl.class)
                .build(ClientHelperFactory.class));
    }

    private void configureDevice() {
        bind(Device.class)
                .to(DeviceImpl.class);

        bind(ScheduledExecutorService.class)
                .annotatedWith(AppDelayExecutor.class)
                .toInstance(Executors.newScheduledThreadPool(10));

        install(new FactoryModuleBuilder()
                .implement(DiscoveryDeviceUdpMessageProcessor.class, DiscoveryDeviceUdpMessageProcessor.class)
                .build(DeviceHelperFactory.class));
    }

    private void configureWsMetadataExchange() {
        bind(GetMetadataClient.class).to(GetMetadataClientImpl.class);
    }

    private void configureWsTransfer() {
        bind(TransferGetClient.class).to(TransferGetClientImpl.class);
    }

    private void configureThreadPools() {
        bind(ListeningExecutorService.class)
                .annotatedWith(NetworkJobThreadPool.class)
                .toInstance(MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10)));
    }

    private void configureMarshalling() {
        bind(SoapMarshalling.class).to(JaxbSoapMarshalling.class).asEagerSingleton();
    }

    private void configureUdp() {
        install(new FactoryModuleBuilder()
                .implement(UdpBindingService.class, UdpBindingServiceImpl.class)
                .build(UdpBindingServiceFactory.class));
    }

    private void configureWsDiscovery() {
        bind(ExecutorService.class)
                .annotatedWith(WsDiscovery.class)
                .toInstance(Executors.newFixedThreadPool(10));

        bind(UdpMessageQueueService.class)
                .annotatedWith(DiscoveryUdpQueue.class)
                .to(UdpMessageQueueServiceImpl.class)
                .asEagerSingleton();

        install(new FactoryModuleBuilder()
                .implement(WsDiscoveryTargetService.class, WsDiscoveryTargetServiceInterceptor.class)
                .build(WsDiscoveryTargetServiceFactory.class));
        install(new FactoryModuleBuilder()
                .implement(WsDiscoveryClient.class, WsDiscoveryClientInterceptor.class)
                .build(WsDiscoveryClientFactory.class));
    }

    private void configureSoapEngine() {
        bind(RequestResponseServer.class)
                .to(RequestResponseServerImpl.class);
        bind(NotificationSink.class)
                .to(NotificationSinkImpl.class);

        install(new FactoryModuleBuilder()
                .implement(SoapMessage.class, SoapMessage.class)
                .build(SoapMessageFactory.class));
        install(new FactoryModuleBuilder()
                .implement(RequestResponseClient.class, RequestResponseClientImpl.class)
                .build(RequestResponseClientFactory.class));
        install(new FactoryModuleBuilder()
                .implement(NotificationSource.class, NotificationSourceImpl.class)
                .build(NotificationSourceFactory.class));
    }

    private void configureWsAddressing() {
        bind(WsAddressingClientInterceptor.class).asEagerSingleton();
        bind(WsAddressingServerInterceptor.class).asEagerSingleton();
    }

    private void configureWsEventing() {
        bind(EventSource.class)
                .to(EventSourceInterceptor.class);
        bind(LocalAddressResolver.class)
                .to(LocalAddressResolverImpl.class);
        bind(ScheduledExecutorService.class)
                .annotatedWith(AutoRenewExecutor.class)
                .toInstance(Executors.newScheduledThreadPool(10));

        install(new FactoryModuleBuilder()
                .implement(EventSink.class, EventSinkImpl.class)
                .build(WsEventingEventSinkFactory.class));

        install(new FactoryModuleBuilder()
                .implement(NotificationWorker.class, NotificationWorker.class)
                .build(NotificationWorkerFactory.class));

        install(new FactoryModuleBuilder()
                .implement(SourceSubscriptionManager.class, SourceSubscriptionManagerImpl.class)
                .implement(SinkSubscriptionManager.class, SinkSubscriptionManagerImpl.class)
                .build(SubscriptionManagerFactory.class));
    }

    private void configureHttpServer() {
        bind(HttpServerRegistry.class).to(GrizzlyHttpServerRegistry.class).asEagerSingleton();
    }
}
