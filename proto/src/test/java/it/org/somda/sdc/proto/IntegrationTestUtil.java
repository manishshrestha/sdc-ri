package it.org.somda.sdc.proto;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import org.somda.sdc.biceps.guice.DefaultBicepsConfigModule;
import org.somda.sdc.biceps.guice.DefaultBicepsModule;
import org.somda.sdc.common.guice.DefaultCommonConfigModule;
import org.somda.sdc.common.guice.DefaultCommonModule;
import org.somda.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.somda.sdc.dpws.guice.DefaultDpwsModule;
import org.somda.sdc.proto.guice.DefaultGrpcConfigModule;
import org.somda.sdc.proto.guice.DefaultProtoConfigModule;
import org.somda.sdc.proto.guice.DefaultProtoModule;
import test.org.somda.common.TestLogging;

import java.util.ArrayList;
import java.util.Arrays;

public class IntegrationTestUtil {
    private final Injector injector;

    public IntegrationTestUtil(AbstractModule... overrides) {
        TestLogging.configure();

        var overrideList = new ArrayList<AbstractModule>();
        // always add udp binding module
        overrideList.add(new MockedUdpBindingModule());
        overrideList.addAll(Arrays.asList(overrides));
        injector = Guice.createInjector(
                Modules.override(
                        new DefaultCommonConfigModule(),
                        new DefaultCommonModule(),
                        new DefaultDpwsModule(),
                        new DefaultDpwsConfigModule(),
                        new DefaultProtoModule(),
                        new DefaultProtoConfigModule(),
                        new DefaultGrpcConfigModule(),
                        new DefaultBicepsConfigModule(),
                        new DefaultBicepsModule()
                ).with(
                        overrideList
                ));
    }

    public Injector getInjector() {
        return injector;
    }
}
