package org.somda.sdc.proto.guice;

import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.somda.sdc.common.guice.AbstractConfigurationModule;
import org.somda.sdc.glue.common.ModificationsBuilder;
import org.somda.sdc.glue.common.factory.ModificationsBuilderFactory;

public class DefaultProtoConfigModule extends AbstractConfigurationModule {
    @Override
    protected void defaultConfigure() {
        glueWorkarounds();
    }

    void glueWorkarounds() {
        install(new FactoryModuleBuilder()
                .implement(ModificationsBuilder.class, ModificationsBuilder.class)
                .build(ModificationsBuilderFactory.class));
        bind(org.somda.sdc.glue.common.CommonConfig.NAMESPACE_MAPPINGS,
                String.class,
                "");
    }
}
