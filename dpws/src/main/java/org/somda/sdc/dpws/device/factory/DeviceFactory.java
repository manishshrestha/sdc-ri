package org.somda.sdc.dpws.device.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.dpws.device.Device;
import org.somda.sdc.dpws.device.DeviceSettings;
import org.somda.sdc.dpws.soap.wseventing.EventSourceFilterPlugin;

import java.util.Map;

/**
 * Factory to create {@linkplain Device} instances.
 */
public interface DeviceFactory {
    /**
     * Creates a new {@linkplain Device} instance.
     *
     * @param deviceSettings           the settings to be applied on the device.
     * @param eventSourceFilterPlugins a map of event source filters where map key is a supported filter dialect URI
     *                                 and value is a custom (not action-based) implementation of the filter dialect.
     * @return a new {@linkplain Device} instance, not running. Use {@link Device#startAsync()} to start the device.
     */
    Device createDevice(@Assisted DeviceSettings deviceSettings,
                        @Assisted Map<String, EventSourceFilterPlugin> eventSourceFilterPlugins);
}
