package org.somda.sdc.proto.guice;

import org.somda.sdc.common.guice.AbstractConfigurationModule;

public class DefaultGrpcConfigModule extends AbstractConfigurationModule {
    @Override
    protected void defaultConfigure() {
        configureSecurity();
    }

    void configureSecurity() {
        bind(GrpcConfig.GRPC_SERVER_INSECURE,
            Boolean.class,
            false);
    }
}
