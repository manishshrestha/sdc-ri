package org.somda.sdc.proto.server.guice;

import org.somda.sdc.common.guice.AbstractConfigurationModule;

public class DefaultGrpcServerConfigModule extends AbstractConfigurationModule {
    @Override
    protected void defaultConfigure() {
        configureSecurity();
    }

    void configureSecurity() {
        bind(GrpcServerConfig.GRPC_INSECURE,
            Boolean.class,
            false);
    }
}
