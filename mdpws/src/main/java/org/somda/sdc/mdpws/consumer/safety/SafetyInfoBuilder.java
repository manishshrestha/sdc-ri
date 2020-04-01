package org.somda.sdc.mdpws.consumer.safety;

import org.somda.sdc.mdpws.model.ObjectFactory;
import org.somda.sdc.mdpws.model.SafetyInfoType;

import javax.xml.bind.JAXBElement;
import java.util.HashMap;
import java.util.Map;

/**
 * Builder class to conveniently assemble safety info that is intended to be added as {@linkplain org.somda.sdc.dpws.soap.SoapMessage} headers.
 */
public class SafetyInfoBuilder {
    private final ObjectFactory objectFactory;
    private final Map<String, Object> dualChannelValues;
    private final Map<String, Object> safetyContextValues;

    /**
     * Creates a new fluent interface object.
     *
     * @return a new instance.
     */
    public static SafetyInfoBuilder create() {
        return new SafetyInfoBuilder();
    }

    private SafetyInfoBuilder() {
        objectFactory = new ObjectFactory();
        dualChannelValues = new HashMap<>();
        safetyContextValues = new HashMap<>();
    }

    /**
     * Adds a new dual channel value.
     * <p>
     * Please note that duplicates will not be checked and existing entries are overwritten silently.
     *
     * @param selector the selector value retrieved from safety requirements.
     * @param value    the dual channel value.
     * @return this object.
     */
    public SafetyInfoBuilder addDualChannelValue(String selector, Object value) {
        dualChannelValues.put(selector, value);
        return this;
    }

    /**
     * Adds a new safety context value.
     * <p>
     * Please note that duplicates will not be checked and existing entries are overwritten silently.
     *
     * @param selector the selector value retrieved from safety requirements.
     * @param value    the safety context value.
     * @return this object.
     */
    public SafetyInfoBuilder addSafetyContextValue(String selector, Object value) {
        safetyContextValues.put(selector, value);
        return this;
    }

    /**
     * Creates a safety info object based on added the dual channel and safety context values.
     *
     * @return JAXB element that can be hooked into {@link org.somda.sdc.dpws.soap.SoapMessage} headers.
     */
    public JAXBElement<SafetyInfoType> get() {
        var safetyInfoType = objectFactory.createSafetyInfoType();
        var dualChannelType = objectFactory.createDualChannelType();
        dualChannelValues.forEach((selector, value) -> {
            var dualChannelValueType = objectFactory.createDualChannelValueType();
            dualChannelValueType.setReferencedSelector(selector);
            dualChannelValueType.setValue(value);
            dualChannelType.getValue().add(objectFactory.createDualChannelTypeValue(dualChannelValueType));
        });
        var safetyContextType = objectFactory.createSafetyContextType();
        safetyContextValues.forEach((selector, value) -> {
            var safetyContextValueType = objectFactory.createContextValueType();
            safetyContextValueType.setReferencedSelector(selector);
            safetyContextValueType.setValue(value);
            safetyContextType.getValue().add(objectFactory.createSafetyContextTypeValue(safetyContextValueType));
        });
        safetyInfoType.setDualChannel(dualChannelType);
        safetyInfoType.setSafetyContext(safetyContextType);
        return objectFactory.createSafetyInfo(safetyInfoType);
    }
}
