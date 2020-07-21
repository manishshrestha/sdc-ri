package it.org.somda.sdc.proto;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import org.somda.sdc.common.guice.DefaultCommonConfigModule;
import org.somda.sdc.common.guice.DefaultHelperModule;
import org.somda.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.somda.sdc.dpws.guice.DefaultDpwsModule;
import org.somda.sdc.proto.guice.DefaultProtoModule;
import test.org.somda.common.TestLogging;

public class IntegrationTestUtil {
    private final Injector injector;

    public IntegrationTestUtil() {
        TestLogging.configure();
        injector = Guice.createInjector(
                Modules.override(
                        new DefaultCommonConfigModule(),
                        new DefaultHelperModule(),
                        new DefaultDpwsModule(),
                        new DefaultDpwsConfigModule(),
                        new DefaultProtoModule()
                ).with(new MockedUdpBindingModule()));
    }

    public Injector getInjector() {
        return injector;
    }
}
