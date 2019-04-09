package org.ieee11073.sdc.dpws;

import com.google.inject.Inject;
import org.ieee11073.sdc.dpws.model.LocalizedStringType;
import org.ieee11073.sdc.dpws.model.ObjectFactory;
import org.ieee11073.sdc.dpws.model.ThisDeviceType;
import org.ieee11073.sdc.dpws.model.ThisModelType;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * Utility functions for DPWS.
 *
 * Use to get convenient builders for DPWS' ThisDevice and ThisModel.
 */
public class DpwsUtil {
    private final ObjectFactory dpwsFactory;

    @Inject
    DpwsUtil(ObjectFactory dpwsFactory) {
        this.dpwsFactory = dpwsFactory;
    }

    public ThisDeviceType createThisDevice(List<LocalizedStringType> friendlyName,
                                           @Nullable String firmwareVersion,
                                           @Nullable String serialNumber) {
        ThisDeviceType devType = dpwsFactory.createThisDeviceType();
        devType.setFriendlyName(friendlyName);
        Optional.ofNullable(firmwareVersion).ifPresent(devType::setFirmwareVersion);
        Optional.ofNullable(serialNumber).ifPresent(devType::setSerialNumber);
        return devType;
    }

    public ThisDeviceBuilder createDeviceBuilder() {
        return new ThisDeviceBuilder();
    }

    public ThisDeviceBuilder createDeviceBuilder(List<LocalizedStringType> friendlyName) {
        return new ThisDeviceBuilder(friendlyName);
    }

    public ThisModelBuilder createModelBuilder() {
        return new ThisModelBuilder();
    }

    public ThisModelBuilder createModelBuilder(List<LocalizedStringType> manufacturer,
                                               List<LocalizedStringType> modelName) {
        return new ThisModelBuilder(manufacturer, modelName);
    }

    public ThisModelType createThisModel(List<LocalizedStringType> manufacturer,
                                         @Nullable String manufacturerUrl,
                                         List<LocalizedStringType> modelName,
                                         @Nullable String modelNumber,
                                         @Nullable String modelUrl,
                                         @Nullable String presentationUrl) {
        ThisModelType modType = dpwsFactory.createThisModelType();
        modType.setManufacturer(manufacturer);
        Optional.ofNullable(manufacturerUrl).ifPresent(modType::setManufacturerUrl);
        modType.setModelName(modelName);
        Optional.ofNullable(modelNumber).ifPresent(modType::setModelNumber);
        Optional.ofNullable(modelUrl).ifPresent(modType::setModelUrl);
        Optional.ofNullable(presentationUrl).ifPresent(modType::setPresentationUrl);
        return modType;
    }

    public LocalizedStringsBuilder createLocalizedStrings(String lang, String text) {
        return new LocalizedStringsBuilder(lang, text);
    }

    public LocalizedStringsBuilder createLocalizedStrings(String text) {
        return new LocalizedStringsBuilder(text);
    }

    public LocalizedStringsBuilder createLocalizedStrings() {
        return new LocalizedStringsBuilder();
    }

}
