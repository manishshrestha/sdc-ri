package it.org.somda.glue;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.somda.sdc.biceps.guice.DefaultBicepsConfigModule;
import org.somda.sdc.biceps.guice.DefaultBicepsModule;
import org.somda.sdc.common.guice.DefaultHelperModule;
import org.somda.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.somda.sdc.dpws.guice.DefaultDpwsModule;
import org.somda.sdc.dpws.soap.SoapConfig;
import org.somda.sdc.glue.GlueConstants;
import org.somda.sdc.glue.guice.DefaultGlueConfigModule;
import org.somda.sdc.glue.guice.DefaultGlueModule;
import test.org.somda.common.TestLogging;

public class IntegrationTestUtil {
    private final Injector injector;

    public IntegrationTestUtil() {
        TestLogging.configure();
        injector = Guice.createInjector(
                new DefaultGlueModule(),
                new DefaultGlueConfigModule(),
                new DefaultBicepsModule(),
                new DefaultBicepsConfigModule(),
                new DefaultHelperModule(),
                new DefaultDpwsModule(),
                new DefaultDpwsConfigModule() {
                    @Override
                    protected void customConfigure() {
                        bind(SoapConfig.JAXB_CONTEXT_PATH,
                                String.class,
                                GlueConstants.JAXB_CONTEXT_PATH);
                    }
                });
    }

    public Injector getInjector() {
        return injector;
    }
}
