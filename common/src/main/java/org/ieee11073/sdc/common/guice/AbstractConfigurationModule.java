package org.ieee11073.sdc.common.guice;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.google.inject.util.Providers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.TreeMap;

/**
 * Default Guice app configuration module.
 * <p>
 * Derive from this class to override default configuration values. Use {@link #bind(String, Class, Object)} to
 * set values.
 */
public abstract class AbstractConfigurationModule extends AbstractModule {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractConfigurationModule.class);
    private final Map<String, ConfigurationValue> boundValues = new TreeMap<>();
    private boolean configureStarted = false;

    /**
     * Bind a configuration key to a value from outside.
     * <p>
     * This operation can only be performed once per key. All unpopulated keys are supposed to be filled with a
     * default value once {@link #configure()} is called by Guice.
     *
     * @param name     Configuration key.
     * @param dataType Data type bounded by that key (should be defined in configuration class).
     * @param value    The configuration value to set.
     */
    public <T> void bind(String name, Class<T> dataType, @Nullable T value) {
        if (!boundValues.containsKey(name)) {
            // Wrap binding into closure and call later as Guice's bind() is only available during configure()
            Runnable runBind = () -> {
                if (value == null) {
                    bind(dataType)
                            .annotatedWith(Names.named(name))
                            .toProvider(Providers.of(null));
                } else {
                    bind(dataType)
                            .annotatedWith(Names.named(name))
                            .toInstance(value);
                }
            };
            ValueOrigin valueOrigin = configureStarted ? ValueOrigin.DEFAULTED : ValueOrigin.CUSTOMIZED;
            boundValues.put(name, new ConfigurationValue(valueOrigin, runBind, value));

        } else {
            if (!configureStarted) {
                LOG.warn("Try to populate configuration key '{}' twice. Attempt skipped.", name);
            }
        }
    }

    /**
     * Conduct default configuration. This method is called by Guice.
     */
    @Override
    @SuppressWarnings("Unchecked")
    final protected void configure() {
        customConfigure();
        configureStarted = true;
        defaultConfigure();
        logConfiguredValues();

        boundValues.entrySet().forEach(configValue -> configValue.getValue().getBinder().run());
    }

    /**
     * Implement this method to settle your default configuration.
     */
    protected abstract void defaultConfigure();

    /**
     * Implement this method to apply custom configuration.
     * <p>
     * Optional override as values can also be set from outside the class with {@link #bind(String, Class, Object)}.
     */
    protected void customConfigure() {
        // Override is optional
    }

    private void logConfiguredValues() {
        boundValues.entrySet().forEach(value ->
                LOG.info("{} {} := {}",
                        value.getValue().getValueOrigin(),
                        value.getKey(),
                        value.getValue().getValue()));
    }

    private enum ValueOrigin {
        DEFAULTED("[defaulted ]"),
        CUSTOMIZED("[customized]");

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
        private final Runnable binder;
        private final Object value;

        ConfigurationValue(ValueOrigin valueOrigin, Runnable binder, Object value) {
            this.valueOrigin = valueOrigin;
            this.binder = binder;
            this.value = value;
        }

        ValueOrigin getValueOrigin() {
            return valueOrigin;
        }

        Runnable getBinder() {
            return binder;
        }

        Object getValue() {
            return value;
        }
    }
}
