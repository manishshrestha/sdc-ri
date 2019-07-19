package org.ieee11073.sdc.biceps.guice;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.ieee11073.sdc.biceps.common.*;
import org.ieee11073.sdc.biceps.common.factory.MdibEntityFactory;
import org.ieee11073.sdc.biceps.common.factory.MdibQueueFactory;

/**
 * Default BICEPS module.
 */
public class DefaultBicepsModule extends AbstractModule {
    @Override
    protected void configure() {
        install(new FactoryModuleBuilder()
                .implement(WritableMdibEntity.class, MdibEntityImpl.class)
                .implement(MdibEntity.class, MdibEntityImpl.class)
                .build(MdibEntityFactory.class));

        install(new FactoryModuleBuilder()
                .implement(MdibQueue.class, MdibQueueImpl.class)
                .build(MdibQueueFactory.class));
    }
}
