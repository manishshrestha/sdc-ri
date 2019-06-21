package org.ieee11073.sdc.dpws.guice;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import org.ieee11073.sdc.dpws.DpwsConfig;
import org.ieee11073.sdc.dpws.client.ClientConfig;
import org.ieee11073.sdc.dpws.http.HttpConfig;
import org.ieee11073.sdc.dpws.soap.SoapConfig;
import org.ieee11073.sdc.dpws.soap.wsaddressing.WsAddressingConfig;
import org.ieee11073.sdc.dpws.soap.wsdiscovery.WsDiscoveryConfig;
import org.ieee11073.sdc.dpws.soap.wseventing.WsEventingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.TreeMap;

/**
 * Default configuration module to configure {@link DefaultDpwsModule}.
 *
 * Derive from this class to override default configuration values. Use {@link #bind(String, Class, Object)}
 * to set your custom values.
 *
 * You can either configure values on construction or by overriding {@link #customConfigure()}.
 */
public class DefaultDpwsConfigModule extends AbstractModule {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultDpwsConfigModule.class);
    private final Map<String, ConfigurationValue> boundedValues = new TreeMap<>();
    private boolean configureStarted = false;

    /**
     * Bind a configuration key to a value.
     *
     * This operation can only be performed once per key. All unpopulated keys will be settled with a default value
     * once {@link #configure()} is called by Guice.
     *
     * @param name     Configuration key.
     * @param dataType Data type bounded by that key (should be defined in configuration class).
     * @param value    The configuration value to set.
     */
    protected <T> void bind(String name, Class<T> dataType, T value) {
        if (!boundedValues.containsKey(name)) {
            ValueOrigin valueOrigin = configureStarted ? ValueOrigin.INHERITED : ValueOrigin.OVERRIDDEN;
            boundedValues.put(name, new ConfigurationValue(valueOrigin, value));
            bind(dataType).annotatedWith(Names.named(name)).toInstance(value);
        } else {
            if (!configureStarted) {
                LOG.warn("Try to populate configuration key '{}' again. Skipped.", name);
            }
        }
    }

    /**
     * Perform default configuration.
     */
    @Override
    final protected void configure() {
        customConfigure();

        configureStarted = true;

        configureWsAddressingConfig();
        configureWsDiscoveryConfig();
        configureWsEventingConfig();
        configureClientConfig();
        configureHttpConfig();
        configureDpws();
        logConfiguredValues();
    }



    /**
     * Override this method in derived class for custom configuration.
     */
    protected void customConfigure() {
        // nothing to do here - override on derived class to add custom configuration
    }

    private void logConfiguredValues() {
        boundedValues.entrySet().stream().forEach(value ->
                LOG.debug("Configure {} key: {} := {}",
                        value.getValue().getValueOrigin(),
                        value.getKey(),
                        value.getValue().getValue()));
    }

    private void configureDpws() {
        bind(DpwsConfig.MAX_WAIT_FOR_FUTURES,
                Duration.class,
                Duration.ofSeconds(10));
    }

    private void configureHttpConfig() {
        bind(HttpConfig.PORT_MIN,
                Integer.class,
                49152);
        bind(HttpConfig.PORT_MAX,
                Integer.class,
                65535);
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

    private enum ValueOrigin {
        INHERITED("inherited"),
        OVERRIDDEN("overridden");

        ValueOrigin(String value) {
            caption = value;
        }

        @Override
        public String toString() {
            return caption;
        }

        private final String caption;
    }

    private class ConfigurationValue {
        private final ValueOrigin valueOrigin;
        private final Object value;

        ConfigurationValue(ValueOrigin valueOrigin, Object value) {
            this.valueOrigin = valueOrigin;
            this.value = value;
        }

        ValueOrigin getValueOrigin() {
            return valueOrigin;
        }

        Object getValue() {
            return value;
        }
    }
}
