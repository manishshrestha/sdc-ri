package org.somda.sdc.dpws.guice;

import org.somda.sdc.common.guice.AbstractConfigurationModule;
import org.somda.sdc.dpws.DpwsConfig;
import org.somda.sdc.dpws.DpwsConstants;
import org.somda.sdc.dpws.client.ClientConfig;
import org.somda.sdc.dpws.crypto.CryptoConfig;
import org.somda.sdc.dpws.crypto.CryptoSettings;
import org.somda.sdc.dpws.device.DeviceConfig;
import org.somda.sdc.dpws.soap.SoapConfig;
import org.somda.sdc.dpws.soap.wsaddressing.WsAddressingConfig;
import org.somda.sdc.dpws.soap.wsdiscovery.WsDiscoveryConfig;
import org.somda.sdc.dpws.soap.wseventing.WsEventingConfig;

import java.io.File;
import java.time.Duration;

/**
 * Default configuration module to configure {@link DefaultDpwsModule}.
 * <p>
 * Derive from this class to override default configuration values.
 * Use {@link #bind(String, Class, Object)} to set your default values.
 */
public class DefaultDpwsConfigModule extends AbstractConfigurationModule {
    @Override
    public void defaultConfigure() {
        configureWsAddressingConfig();
        configureWsDiscoveryConfig();
        configureWsEventingConfig();
        configureClientConfig();
        configureDeviceConfig();
        configureCryptoConfig();
        configureSoapConfig();
        configureDpws();
    }

    private void configureDeviceConfig() {
        bind(DeviceConfig.UNSECURED_ENDPOINT,
                Boolean.class,
                true);

        bind(DeviceConfig.SECURED_ENDPOINT,
                Boolean.class,
                false);
    }

    private void configureDpws() {
        bind(DpwsConfig.MAX_WAIT_FOR_FUTURES,
                Duration.class,
                Duration.ofSeconds(10));

        bind(DpwsConfig.MAX_ENVELOPE_SIZE,
                Integer.class,
                DpwsConstants.MAX_ENVELOPE_SIZE);

        bind(DpwsConfig.COMMUNICATION_LOG_DIRECTORY,
                File.class,
                new File("commlog"));
    }

    private void configureCryptoConfig() {
        bind(CryptoConfig.CRYPTO_SETTINGS,
                CryptoSettings.class,
                null);
    }

    private void configureClientConfig() {
        bind(ClientConfig.MAX_WAIT_FOR_RESOLVE_MATCHES,
                Duration.class,
                Duration.ofSeconds(10));

        bind(ClientConfig.AUTO_RESOLVE,
                Boolean.class,
                false);
    }

    private void configureWsDiscoveryConfig() {
        bind(WsDiscoveryConfig.MAX_WAIT_FOR_PROBE_MATCHES,
                Duration.class,
                Duration.ofSeconds(10));
        bind(WsDiscoveryConfig.MAX_WAIT_FOR_RESOLVE_MATCHES,
                Duration.class,
                Duration.ofSeconds(10));
        bind(WsDiscoveryConfig.PROBE_MATCHES_BUFFER_SIZE,
                Integer.class,
                50);
        bind(WsDiscoveryConfig.RESOLVE_MATCHES_BUFFER_SIZE,
                Integer.class,
                50);
    }

    private void configureWsAddressingConfig() {
        bind(WsAddressingConfig.IGNORE_MESSAGE_IDS,
                Boolean.class,
                false);
        bind(WsAddressingConfig.MESSAGE_ID_CACHE_SIZE,
                Integer.class,
                50);
    }

    private void configureWsEventingConfig() {
        bind(WsEventingConfig.NOTIFICATION_STALE_DURATION,
                Duration.class,
                Duration.ofSeconds(10));
        bind(WsEventingConfig.SOURCE_MAX_EXPIRES,
                Duration.class,
                Duration.ofHours(1));
        bind(WsEventingConfig.SOURCE_MAX_RETRIES_ON_DELIVERY_FAILURE,
                Integer.class,
                3);
        bind(WsEventingConfig.SOURCE_SUBSCRIPTION_MANAGER_PATH,
                String.class,
                "SubscriptionManager");
        bind(WsEventingConfig.NOTIFICATION_QUEUE_CAPACITY,
                Integer.class,
                Integer.valueOf(500));
    }

    private void configureSoapConfig() {
        bind(SoapConfig.JAXB_CONTEXT_PATH,
                String.class,
                "");
        bind(SoapConfig.NAMESPACE_MAPPINGS,
                String.class,
                "");
    }
}
