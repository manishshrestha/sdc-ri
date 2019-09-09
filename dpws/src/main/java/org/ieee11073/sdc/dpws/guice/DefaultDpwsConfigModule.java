package org.ieee11073.sdc.dpws.guice;

import org.ieee11073.sdc.common.guice.AbstractConfigurationModule;
import org.ieee11073.sdc.dpws.DpwsConfig;
import org.ieee11073.sdc.dpws.client.ClientConfig;
import org.ieee11073.sdc.dpws.crypto.CryptoConfig;
import org.ieee11073.sdc.dpws.crypto.CryptoSettings;
import org.ieee11073.sdc.dpws.device.DeviceConfig;
import org.ieee11073.sdc.dpws.soap.SoapConfig;
import org.ieee11073.sdc.dpws.soap.wsaddressing.WsAddressingConfig;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.WsDiscoveryConfig;
import org.ieee11073.sdc.dpws.soap.wseventing.WsEventingConfig;

import java.time.Duration;

/**
 * Default configuration module to configure {@link DefaultDpwsModule}.
 * <p>
 * Derive from this class to override default configuration values. Use {@link #bind(String, Class, Object)}
 * to set your default values.
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

        bind(ClientConfig.ENABLE_WATCHDOG,
                Boolean.class,
                true);

        bind(ClientConfig.WATCHDOG_PERIOD,
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
        bind(WsEventingConfig.AUTO_RENEW_BEFORE_EXPIRES,
                Duration.class,
                Duration.ofMinutes(1));
        bind(WsEventingConfig.NOTIFICATION_STALE_DURATION,
                Duration.class,
                Duration.ofSeconds(10));
        bind(WsEventingConfig.SINK_DEFAULT_REQUESTED_EXPIRES,
                Duration.class,
                Duration.ofHours(2));
        bind(WsEventingConfig.SOURCE_MAX_EXPIRES,
                Duration.class,
                Duration.ofHours(1));
        bind(WsEventingConfig.SOURCE_MAX_RETRIES_ON_DELIVERY_FAILURE,
                Integer.class,
                3);
        bind(WsEventingConfig.SOURCE_SUBSCRIPTION_MANAGER_PATH,
                String.class,
                "SubscriptionManager");
        bind(SoapConfig.JAXB_CONTEXT_PATH,
                String.class,
                "");
    }
}
