package org.ieee11073.sdc.glue.guice;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.ieee11073.sdc.biceps.common.MdibEntity;
import org.ieee11073.sdc.biceps.common.MdibEntityImpl;
import org.ieee11073.sdc.biceps.common.access.ReadTransaction;
import org.ieee11073.sdc.biceps.common.access.ReadTransactionImpl;
import org.ieee11073.sdc.biceps.common.access.factory.ReadTransactionFactory;
import org.ieee11073.sdc.biceps.common.factory.MdibEntityGuiceAssistedFactory;
import org.ieee11073.sdc.biceps.common.storage.MdibStorage;
import org.ieee11073.sdc.biceps.common.storage.MdibStorageImpl;
import org.ieee11073.sdc.biceps.common.storage.MdibStoragePreprocessingChain;
import org.ieee11073.sdc.biceps.common.storage.factory.MdibStorageFactory;
import org.ieee11073.sdc.biceps.common.storage.factory.MdibStoragePreprocessingChainFactory;
import org.ieee11073.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.ieee11073.sdc.biceps.consumer.access.RemoteMdibAccessImpl;
import org.ieee11073.sdc.biceps.consumer.access.factory.RemoteMdibAccessFactory;
import org.ieee11073.sdc.biceps.provider.access.LocalMdibAccess;
import org.ieee11073.sdc.biceps.provider.access.LocalMdibAccessImpl;
import org.ieee11073.sdc.biceps.provider.access.factory.LocalMdibAccessFactory;
import org.ieee11073.sdc.glue.provider.sco.factory.ContextFactory;
import org.ieee11073.sdc.glue.provider.sco.factory.ScoControllerFactory;
import org.ieee11073.sdc.glue.provider.services.helper.ReportGenerator;
import org.ieee11073.sdc.glue.provider.services.helper.factory.MdibMapperFactory;
import org.ieee11073.sdc.glue.provider.services.helper.factory.ReportGeneratorFactory;

/**
 * Default Glue module.
 */
public class DefaultGlueModule extends AbstractModule {
    @Override
    protected void configure() {
        install(new FactoryModuleBuilder()
                .build(ReportGeneratorFactory.class));

        install(new FactoryModuleBuilder()
                .build(MdibMapperFactory.class));

        install(new FactoryModuleBuilder()
                .build(ContextFactory.class));

        install(new FactoryModuleBuilder()
                .build(ScoControllerFactory.class));
//
//        install(new FactoryModuleBuilder()
//                .implement(MdibStorage.class, MdibStorageImpl.class)
//                .build(MdibStorageFactory.class));
    }
}
