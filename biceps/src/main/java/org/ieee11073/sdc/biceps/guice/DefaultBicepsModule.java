package org.ieee11073.sdc.biceps.guice;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.ieee11073.sdc.biceps.common.*;
import org.ieee11073.sdc.biceps.common.factory.MdibEntityGuiceAssistedFactory;

/**
 * Default BICEPS module.
 */
public class DefaultBicepsModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(MdibStorage.class).to(MdibStorageImpl.class);
        install(new FactoryModuleBuilder()
                .implement(MdibEntity.class, MdibEntityImpl.class)
                .build(MdibEntityGuiceAssistedFactory.class));
//
//        install(new FactoryModuleBuilder()
//                .implement(MdibAccess.class, MdibStorageNotificationAccess.class)
//                .build(MdibAccessFactory.class));

//        install(new FactoryModuleBuilder()
//                .implement(MdibQueue.class, MdibQueueImpl.class)
//                .build(MdibQueueFactory.class));
    }
}
