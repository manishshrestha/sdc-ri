package org.somda.sdc.proto.guice;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.util.ExecutorWrapperService;
import org.somda.sdc.dpws.guice.DiscoveryUdpQueue;
import org.somda.sdc.dpws.udp.UdpBindingService;
import org.somda.sdc.dpws.udp.UdpBindingServiceImpl;
import org.somda.sdc.dpws.udp.UdpMessageQueueService;
import org.somda.sdc.dpws.udp.UdpMessageQueueServiceImpl;
import org.somda.sdc.dpws.udp.factory.UdpBindingServiceFactory;
import org.somda.sdc.glue.provider.services.helper.ReportGenerator;
import org.somda.sdc.glue.provider.services.helper.factory.ReportGeneratorFactory;
import org.somda.sdc.proto.addressing.AddressingValidator;
import org.somda.sdc.proto.addressing.factory.AddressingValidatorFactory;
import org.somda.sdc.proto.consumer.Consumer;
import org.somda.sdc.proto.consumer.ConsumerImpl;
import org.somda.sdc.proto.consumer.SdcRemoteDevice;
import org.somda.sdc.proto.consumer.SdcRemoteDeviceImpl;
import org.somda.sdc.proto.consumer.SdcRemoteDeviceWatchdog;
import org.somda.sdc.proto.consumer.SdcRemoteDevicesConnector;
import org.somda.sdc.proto.consumer.SdcRemoteDevicesConnectorImpl;
import org.somda.sdc.proto.consumer.factory.SdcRemoteDeviceFactory;
import org.somda.sdc.proto.consumer.factory.SdcRemoteDeviceWatchdogFactory;
import org.somda.sdc.proto.discovery.common.UdpUtil;
import org.somda.sdc.proto.discovery.provider.TargetService;
import org.somda.sdc.proto.discovery.provider.TargetServiceImpl;
import org.somda.sdc.proto.discovery.provider.factory.TargetServiceFactory;
import org.somda.sdc.proto.mapping.participant.PojoToProtoTreeMapper;
import org.somda.sdc.proto.mapping.participant.ProtoToPojoModificationsBuilder;
import org.somda.sdc.proto.mapping.participant.factory.PojoToProtoTreeMapperFactory;
import org.somda.sdc.proto.mapping.participant.factory.ProtoToPojoModificationsBuilderFactory;
import org.somda.sdc.proto.provider.Provider;
import org.somda.sdc.proto.provider.ProviderImpl;
import org.somda.sdc.proto.provider.SdcDevice;
import org.somda.sdc.proto.provider.SdcDeviceImpl;
import org.somda.sdc.proto.provider.factory.ProviderFactory;
import org.somda.sdc.proto.provider.factory.SdcDeviceFactory;
import org.somda.sdc.proto.provider.sco.Context;
import org.somda.sdc.proto.provider.sco.ScoController;
import org.somda.sdc.proto.provider.sco.SetService;
import org.somda.sdc.proto.provider.sco.factory.ContextFactory;
import org.somda.sdc.proto.provider.sco.factory.ScoControllerFactory;
import org.somda.sdc.proto.provider.sco.factory.SetServiceFactory;
import org.somda.sdc.proto.provider.service.HighPriorityServices;
import org.somda.sdc.proto.provider.service.guice.ServiceFactory;
import org.somda.sdc.proto.server.Server;
import org.somda.sdc.proto.server.ServerImpl;
import org.somda.sdc.proto.server.guice.ServerFactory;

import javax.inject.Named;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

/**
 * Default protoSDC module.
 */
public class DefaultProtoModule extends AbstractModule {

    private ExecutorWrapperService<ListeningExecutorService> discoveryExecutor;
    private ExecutorWrapperService<ListeningExecutorService> consumerExecutor;
//    private ExecutorWrapperService<ScheduledExecutorService> watchdogScheduledExecutor;

//    public DefaultProtoDiscoveryModule() {
//        consumerExecutor = null;
//        watchdogScheduledExecutor = null;
//    }

    @Override
    protected void configure() {
        install(new FactoryModuleBuilder()
                .implement(AddressingValidator.class, AddressingValidator.class)
                .build(AddressingValidatorFactory.class));
        install(new FactoryModuleBuilder()
                .implement(TargetService.class, TargetServiceImpl.class)
                .build(TargetServiceFactory.class));
        install(new FactoryModuleBuilder()
                .implement(Provider.class, ProviderImpl.class)
                .build(ProviderFactory.class));
        install(new FactoryModuleBuilder()
                .implement(Server.class, ServerImpl.class)
                .build(ServerFactory.class));
        install(new FactoryModuleBuilder()
                .implement(PojoToProtoTreeMapper.class, PojoToProtoTreeMapper.class)
                .build(PojoToProtoTreeMapperFactory.class));
        install(new FactoryModuleBuilder()
                .implement(ProtoToPojoModificationsBuilder.class, ProtoToPojoModificationsBuilder.class)
                .build(ProtoToPojoModificationsBuilderFactory.class));
        install(new FactoryModuleBuilder()
                .implement(HighPriorityServices.class, HighPriorityServices.class)
                .build(ServiceFactory.class));
        install(new FactoryModuleBuilder()
                .implement(Context.class, Context.class)
                .build(ContextFactory.class));
        install(new FactoryModuleBuilder()
                .implement(ScoController.class, ScoController.class)
                .build(ScoControllerFactory.class));
        install(new FactoryModuleBuilder()
                .implement(org.somda.sdc.proto.consumer.sco.ScoController.class, org.somda.sdc.proto.consumer.sco.ScoController.class)
                .build(org.somda.sdc.proto.consumer.sco.factory.ScoControllerFactory.class));
        install(new FactoryModuleBuilder()
                .implement(SetService.class, SetService.class)
                .build(SetServiceFactory.class));
        install(new FactoryModuleBuilder()
                .implement(SdcDevice.class, SdcDeviceImpl.class)
                .build(SdcDeviceFactory.class));
        install(new FactoryModuleBuilder()
                .implement(SdcRemoteDevice.class, SdcRemoteDeviceImpl.class)
                .build(SdcRemoteDeviceFactory.class));
        install(new FactoryModuleBuilder()
                .implement(UdpBindingService.class, UdpBindingServiceImpl.class)
                .build(UdpBindingServiceFactory.class));
        install(new FactoryModuleBuilder()
                .implement(ReportGenerator.class, ReportGenerator.class)
                .build(ReportGeneratorFactory.class));
        install(new FactoryModuleBuilder()
                .implement(SdcRemoteDeviceWatchdog.class, SdcRemoteDeviceWatchdog.class)
                .build(SdcRemoteDeviceWatchdogFactory.class));
        bind(Consumer.class).to(ConsumerImpl.class);
        bind(SdcRemoteDevicesConnector.class).to(SdcRemoteDevicesConnectorImpl.class);
        bind(UdpUtil.class).annotatedWith(ProtoDiscovery.class).to(UdpUtil.class).asEagerSingleton();
        bind(UdpMessageQueueService.class).annotatedWith(DiscoveryUdpQueue.class).to(UdpMessageQueueServiceImpl.class).asEagerSingleton();
    }

    //    private void configureCommon() {
//        install(new FactoryModuleBuilder()
//                .implement(MdibMapper.class, MdibMapper.class)
//                .build(MdibMapperFactory.class));
//        install(new FactoryModuleBuilder()
//                .implement(ModificationsBuilder.class, ModificationsBuilder.class)
//                .build(ModificationsBuilderFactory.class));
//    }
//
//    private void configureConsumer() {
//        bind(SdcRemoteDevicesConnector.class).to(SdcRemoteDevicesConnectorImpl.class);
//
//        install(new FactoryModuleBuilder()
//                .implement(OperationInvocationDispatcher.class, OperationInvocationDispatcher.class)
//                .build(OperationInvocationDispatcherFactory.class));
//
//        install(new FactoryModuleBuilder()
//                .implement(SdcRemoteDevice.class, SdcRemoteDeviceImpl.class)
//                .build(SdcRemoteDeviceFactory.class));
//
//        install(new FactoryModuleBuilder()
//                .implement(org.somda.sdc.glue.consumer.sco.ScoController.class, org.somda.sdc.glue.consumer.sco.ScoController.class)
//                .build(org.somda.sdc.glue.consumer.sco.factory.ScoControllerFactory.class));
//
//        install(new FactoryModuleBuilder()
//                .implement(SdcRemoteDeviceWatchdog.class, SdcRemoteDeviceWatchdog.class)
//                .build(SdcRemoteDeviceWatchdogFactory.class));
//    }
//
//    private void configureProvider() {
//        install(new FactoryModuleBuilder()
//                .implement(ScoController.class, ScoController.class)
//                .build(ScoControllerFactory.class));
//
//        install(new FactoryModuleBuilder()
//                .implement(HighPriorityServices.class, HighPriorityServices.class)
//                .build(ServicesFactory.class));
//
//        install(new FactoryModuleBuilder()
//                .implement(SdcDevice.class, SdcDevice.class)
//                .build(SdcDeviceFactory.class));
//
//        install(new FactoryModuleBuilder()
//                .implement(Context.class, Context.class)
//                .build(ContextFactory.class));
//
//        install(new FactoryModuleBuilder()
//                .implement(ReportGenerator.class, ReportGenerator.class)
//                .build(ReportGeneratorFactory.class));
//    }
//
    @Provides
    @ProtoDiscovery
    ExecutorWrapperService<ListeningExecutorService> getDiscoveryExecutor(@Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        if (discoveryExecutor == null) {
            Callable<ListeningExecutorService> executor = () -> MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(
                    10,
                    new ThreadFactoryBuilder()
                            .setNameFormat("ProtoDiscovery-thread-%d")
                            .setDaemon(true)
                            .build()
            ));
            discoveryExecutor = new ExecutorWrapperService<>(executor, "ProtoDiscovery", frameworkIdentifier);
        }
        return discoveryExecutor;
    }

    @Provides
    @ProtoConsumer
    ExecutorWrapperService<ListeningExecutorService> getConsumerExecutor(
            @com.google.inject.name.Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        if (consumerExecutor == null) {
            Callable<ListeningExecutorService> executor =
                    () -> MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(
                            10,
                            new ThreadFactoryBuilder()
                                    .setNameFormat("Consumer-thread-%d")
                                    .setDaemon(true)
                                    .build()
                    ));
            consumerExecutor = new ExecutorWrapperService<>(executor, "Consumer", frameworkIdentifier);
        }
        return consumerExecutor;
    }
//
//    @Provides
//    @WatchdogScheduledExecutor
//    ExecutorWrapperService<ScheduledExecutorService> getWatchdogScheduledExecutor(@Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
//        if (watchdogScheduledExecutor == null) {
//            Callable<ScheduledExecutorService> executor = () -> Executors.newScheduledThreadPool(
//                    10,
//                    new ThreadFactoryBuilder()
//                            .setNameFormat("WatchdogScheduledExecutor-thread-%d")
//                            .setDaemon(true)
//                            .build()
//            );
//
//            watchdogScheduledExecutor = new ExecutorWrapperService<>(executor, "WatchdogScheduledExecutor", frameworkIdentifier);
//        }
//
//        return watchdogScheduledExecutor;
//    }
}
