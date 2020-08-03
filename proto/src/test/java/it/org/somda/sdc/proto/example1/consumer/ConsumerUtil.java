package it.org.somda.sdc.proto.example1.consumer;

import com.example.CustomCryptoSettings;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import it.org.somda.sdc.dpws.soap.Ssl;
import it.org.somda.sdc.proto.example1.BaseUtil;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
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
        Configurator.reconfigure(localLoggerConfig(Level.DEBUG));

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
                                        null);
//                                        new CustomCryptoSettings());
                            }
                        }
                ));
    }

    public Injector getInjector() {
        return injector;
    }
}
