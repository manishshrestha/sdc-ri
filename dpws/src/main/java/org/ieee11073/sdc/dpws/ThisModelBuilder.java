package org.ieee11073.sdc.dpws;

import org.ieee11073.sdc.dpws.model.LocalizedStringType;
import org.ieee11073.sdc.dpws.model.ObjectFactory;
import org.ieee11073.sdc.dpws.model.ThisModelType;

import java.util.List;

/**
 * Convenient class to build DPWS' ThisModel.
 */
public class ThisModelBuilder {
    private final ThisModelType thisModel;

    public ThisModelBuilder() {
        thisModel = (new ObjectFactory()).createThisModelType();
    }

    public ThisModelBuilder(List<LocalizedStringType> manufacturer,
                            List<LocalizedStringType> modelName) {
        this();
        thisModel.setManufacturer(manufacturer);
        thisModel.setModelName(modelName);
    }

    public ThisModelBuilder setManufacturer(List<LocalizedStringType> manufacturer) {
        thisModel.setManufacturer(manufacturer);
        return this;
    }

    public ThisModelBuilder setManufacturerUrl(String manufacturerUrl) {
        thisModel.setManufacturerUrl(manufacturerUrl);
        return this;
    }

    public ThisModelBuilder setModelName(List<LocalizedStringType> modelName) {
        thisModel.setModelName(modelName);
        return this;
    }

    public ThisModelBuilder setModelNumber(String modelNumber) {
        thisModel.setModelNumber(modelNumber);
        return this;
    }

    public ThisModelBuilder setModelUrl(String modelUrl) {
        thisModel.setModelUrl(modelUrl);
        return this;
    }

    public ThisModelBuilder setPresentationUrl(String presentationUrl) {
        thisModel.setPresentationUrl(presentationUrl);
        return this;
    }

    public ThisModelType get() {
        return thisModel;
    }
}
