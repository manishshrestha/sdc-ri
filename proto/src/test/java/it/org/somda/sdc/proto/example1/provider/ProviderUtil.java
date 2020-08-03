package it.org.somda.sdc.proto.example1.provider;

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
import org.somda.sdc.biceps.model.participant.AbstractMetricValue;
import org.somda.sdc.biceps.model.participant.GenerationMode;
import org.somda.sdc.biceps.model.participant.MeasurementValidity;
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

import java.time.Duration;

public class ProviderUtil extends BaseUtil {

    public static final String OPT_REPORT_INTERVAL = "report_interval";
    public static final String OPT_WAVEFORMS_INTERVAL = "waveform_interval";

    private static final Logger LOG = LogManager.getLogger(ProviderUtil.class);

    private static final String DEFAULT_REPORT_INTERVAL = "5000"; // millis
    private static final String DEFAULT_WAVEFORM_INTERVAL = "100"; // millis

    private final Injector injector;
    private final Duration reportInterval;
    private final Duration waveformInterval;

    public ProviderUtil(String[] args) {
        Configurator.reconfigure(localLoggerConfig(Level.INFO));

        reportInterval = Duration.ofMillis(Long.parseLong(DEFAULT_REPORT_INTERVAL));
        waveformInterval = Duration.ofMillis(Long.parseLong(DEFAULT_WAVEFORM_INTERVAL));

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
                        new DefaultProtoConfigModule(),
                        new DefaultBicepsModule()
                ).with(
                        new AbstractConfigurationModule() {
                            @Override
                            protected void defaultConfigure() {
                                super.customConfigure();
                                bind(CryptoConfig.CRYPTO_SETTINGS,
                                        CryptoSettings.class,
                                        Ssl.setupServer());
                            }
                        }
                ));
    }

    public static void addMetricQualityDemo(AbstractMetricValue val) {
        if (val.getMetricQuality() == null) {
            var qual = new AbstractMetricValue.MetricQuality();
            qual.setMode(GenerationMode.DEMO);
            qual.setValidity(MeasurementValidity.VLD);
            val.setMetricQuality(qual);
        }
    }

    public Injector getInjector() {
        return injector;
    }

    public Duration getReportInterval() {
        return reportInterval;
    }

    public Duration getWaveformInterval() {
        return waveformInterval;
    }
}
