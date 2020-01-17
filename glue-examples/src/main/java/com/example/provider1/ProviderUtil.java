package com.example.provider1;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.somda.sdc.biceps.guice.DefaultBicepsConfigModule;
import org.somda.sdc.biceps.guice.DefaultBicepsModule;
import org.somda.sdc.common.guice.DefaultHelperModule;
import org.somda.sdc.dpws.crypto.CryptoConfig;
import org.somda.sdc.dpws.crypto.CryptoSettings;
import org.somda.sdc.dpws.device.DeviceConfig;
import org.somda.sdc.dpws.guice.DefaultDpwsModule;
import org.somda.sdc.glue.guice.DefaultGlueConfigModule;
import org.somda.sdc.glue.guice.DefaultGlueModule;
import org.somda.sdc.glue.guice.GlueDpwsConfigModule;

public class ProviderUtil {
    private final Injector injector;

    public ProviderUtil() {
        Configurator.initialize(new DefaultConfiguration());
        Configurator.setRootLevel(Level.TRACE);

//        var settings = new ProviderCryptoSettings();
        injector = Guice.createInjector(
                new DefaultGlueModule(),
                new DefaultGlueConfigModule(),
                new DefaultBicepsModule(),
                new DefaultBicepsConfigModule(),
                new DefaultHelperModule(),
                new DefaultDpwsModule(),
                new GlueDpwsConfigModule() {
                    @Override
                    protected void customConfigure() {
                        super.customConfigure();
//                        bind(CryptoConfig.CRYPTO_SETTINGS,
//                                CryptoSettings.class,
//                                settings
//                        );
//                        bind(DeviceConfig.UNSECURED_ENDPOINT, Boolean.class, false);
//                        bind(DeviceConfig.SECURED_ENDPOINT, Boolean.class, true);
                    }
                });
    }

    public Injector getInjector() {
        return injector;
    }
}
