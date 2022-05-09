package org.somda.sdc.dpws;

import com.google.inject.Inject;
import org.somda.sdc.dpws.model.LocalizedStringType;
import org.somda.sdc.dpws.model.ThisDeviceType;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * Utility functions for DPWS.
 * <p>
 * Can be used to get convenient builders for DPWS' ThisDevice and ThisModel objects.
 */
public class DpwsUtil {

    private static final String XML_NAMESPACE = "http://www.w3.org/XML/1998/namespace";
    private static final String XML_LANG = "lang";

    @Inject
    DpwsUtil() {}

    /**
     * Creates a {@link ThisDeviceType} from given parameters.
     *
     * @param friendlyName    the device's friendly name.
     * @param firmwareVersion the device's firmware version.
     * @param serialNumber    the device's serial number.
     * @return the ThisDevice object.
     */
    public ThisDeviceType createThisDevice(List<LocalizedStringType> friendlyName,
                                           @Nullable String firmwareVersion,
                                           @Nullable String serialNumber) {
        var devType = ThisDeviceType.builder()
            .withFriendlyName(friendlyName);
        Optional.ofNullable(firmwareVersion).ifPresent(devType::withFirmwareVersion);
        Optional.ofNullable(serialNumber).ifPresent(devType::withSerialNumber);
        return devType.build();
    }


    /**
     * Sets the language in a LocalizedStringType.
     *
     * @param localizedStringType to set language in
     * @param otherValue language
     * @return updated string type
     */
    public LocalizedStringType setLang(LocalizedStringType localizedStringType, String otherValue) {
        return LocalizedStringsBuilder.setLang(localizedStringType, otherValue);
    }

    /**
     * Creates a builder to create localized texts.
     *
     * @param lang a language tag initially set.
     * @param text a text that matches the given language.
     * @return the localized text builder fluent interface.
     */
    public LocalizedStringsBuilder createLocalizedStrings(String lang, String text) {
        return new LocalizedStringsBuilder(lang, text);
    }

    /**
     * Creates an empty builder with no predefined texts.
     *
     * @return the localized text builder fluent interface.
     */
    public LocalizedStringsBuilder createLocalizedStrings() {
        return new LocalizedStringsBuilder();
    }

}
