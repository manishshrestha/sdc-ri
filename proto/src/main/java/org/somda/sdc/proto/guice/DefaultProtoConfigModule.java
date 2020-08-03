package org.somda.sdc.proto.guice;

import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.somda.sdc.common.guice.AbstractConfigurationModule;
import org.somda.sdc.glue.common.ModificationsBuilder;
import org.somda.sdc.glue.common.factory.ModificationsBuilderFactory;
import org.somda.sdc.glue.consumer.ConsumerConfig;

import java.time.Duration;

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

        bind(ConsumerConfig.WATCHDOG_PERIOD,
                Duration.class,
                Duration.ofMillis(5000));
        bind(ConsumerConfig.AWAITING_TRANSACTION_TIMEOUT,
                Duration.class,
                Duration.ofSeconds(5));
        bind(ConsumerConfig.REQUESTED_EXPIRES,
                Duration.class,
                Duration.ofSeconds(60));
    }
}
