package it.org.somda.sdc.proto.performance.vdb_mdib.consumer;

import com.example.CustomCryptoSettings;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import it.org.somda.sdc.proto.performance.vdb_mdib.BaseUtil;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.somda.sdc.biceps.common.CommonConfig;
import org.somda.sdc.biceps.guice.DefaultBicepsConfigModule;
import org.somda.sdc.biceps.guice.DefaultBicepsModule;
import org.somda.sdc.common.guice.AbstractConfigurationModule;
import org.somda.sdc.common.guice.DefaultCommonConfigModule;
import org.somda.sdc.common.guice.DefaultHelperModule;
import org.somda.sdc.dpws.crypto.CryptoConfig;
import org.somda.sdc.dpws.crypto.CryptoSettings;
import org.somda.sdc.dpws.guice.DefaultDpwsConfigModule;
import org.somda.sdc.dpws.guice.DefaultDpwsModule;
import org.somda.sdc.proto.guice.DefaultGrpcConfigModule;
import org.somda.sdc.proto.guice.DefaultProtoConfigModule;
import org.somda.sdc.proto.guice.DefaultProtoModule;

public class ConsumerUtil extends BaseUtil {

    private static final Logger LOG = LogManager.getLogger(ConsumerUtil.class);

    private final Injector injector;

    public ConsumerUtil() {
        Configurator.reconfigure(localLoggerConfig(Level.INFO));

        injector = Guice.createInjector(
                Modules.override(
                        new DefaultCommonConfigModule(),
                        new DefaultHelperModule(),
                        new DefaultDpwsModule(),
                        new DefaultDpwsConfigModule(),
                        new DefaultProtoModule(),
                        new DefaultProtoConfigModule(),
                        new DefaultGrpcConfigModule(),
                        new DefaultBicepsConfigModule(),
                        new DefaultBicepsModule()
                ).with(
                        new AbstractConfigurationModule() {
                            @Override
                            protected void defaultConfigure() {
                                super.customConfigure();
                                bind(CryptoConfig.CRYPTO_SETTINGS,
                                        CryptoSettings.class,
                                        new CustomCryptoSettings());
                                bind(CommonConfig.COPY_MDIB_INPUT,
                                        Boolean.class,
                                        false);

                                bind(CommonConfig.COPY_MDIB_OUTPUT,
                                        Boolean.class,
                                        false);
                            }
                        }
                ));
    }

    public Injector getInjector() {
        return injector;
    }
}
