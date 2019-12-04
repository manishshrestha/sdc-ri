package org.somda.sdc.glue.guice;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.somda.sdc.glue.common.MdibMapper;
import org.somda.sdc.glue.common.factory.MdibMapperFactory;
import org.somda.sdc.glue.consumer.SdcRemoteDevice;
import org.somda.sdc.glue.consumer.SdcRemoteDeviceImpl;
import org.somda.sdc.glue.consumer.factory.SdcRemoteDeviceFactory;
import org.somda.sdc.glue.consumer.sco.helper.OperationInvocationDispatcher;
import org.somda.sdc.glue.consumer.sco.factory.OperationInvocationDispatcherFactory;
import org.somda.sdc.glue.provider.SdcDevice;
import org.somda.sdc.glue.provider.factory.SdcDeviceFactory;
import org.somda.sdc.glue.provider.sco.Context;
import org.somda.sdc.glue.provider.sco.ScoController;
import org.somda.sdc.glue.provider.sco.factory.ContextFactory;
import org.somda.sdc.glue.provider.sco.factory.ScoControllerFactory;
import org.somda.sdc.glue.provider.services.HighPriorityServices;
import org.somda.sdc.glue.provider.services.factory.ServicesFactory;
import org.somda.sdc.glue.provider.services.helper.ReportGenerator;
import org.somda.sdc.glue.provider.services.helper.factory.ReportGeneratorFactory;

import java.util.concurrent.Executors;

/**
 * Default Glue module.
 */
public class DefaultGlueModule extends AbstractModule {
    @Override
    protected void configure() {
        configureThreadPools();

        install(new FactoryModuleBuilder()
                .implement(ReportGenerator.class, ReportGenerator.class)
                .build(ReportGeneratorFactory.class));

        install(new FactoryModuleBuilder()
                .implement(MdibMapper.class, MdibMapper.class)
                .build(MdibMapperFactory.class));

        install(new FactoryModuleBuilder()
                .implement(Context.class, Context.class)
                .build(ContextFactory.class));

        install(new FactoryModuleBuilder()
                .implement(ScoController.class, ScoController.class)
                .build(ScoControllerFactory.class));

        install(new FactoryModuleBuilder()
                .implement(HighPriorityServices.class, HighPriorityServices.class)
                .build(ServicesFactory.class));

        install(new FactoryModuleBuilder()
                .implement(SdcDevice.class, SdcDevice.class)
                .build(SdcDeviceFactory.class));

        install(new FactoryModuleBuilder()
                .implement(OperationInvocationDispatcher.class, OperationInvocationDispatcher.class)
                .build(OperationInvocationDispatcherFactory.class));

        install(new FactoryModuleBuilder()
                .implement(SdcRemoteDevice.class, SdcRemoteDeviceImpl.class)
                .build(SdcRemoteDeviceFactory.class));
    }

    private void configureThreadPools() {
        bind(ListeningExecutorService.class)
                .annotatedWith(Consumer.class)
                .toInstance(MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10)));
    }
}
