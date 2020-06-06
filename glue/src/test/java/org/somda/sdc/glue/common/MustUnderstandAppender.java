package org.somda.sdc.glue.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.common.CommonConstants;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * Appends a BICEPS extension MustUnderstand attribute to a {@linkplain javax.xml.bind.JAXBElement}.
 */
public class MustUnderstandAppender {
    private static final Logger LOG = LogManager.getLogger(MustUnderstandAppender.class);
    private static final String METHOD_NAME_GET_ATTRIBUTES = "getOtherAttributes";

    /**
     * Turns a JAXB XML element into a BICEPS extension with a specific MustUnderstand behavior.
     *
     * @param extensionElement the element to attach a MustUnderstand attribute to
     * @param value            the MustUnderstand boolean value (true for required to be understood, false otherwise)
     * @param <T>              any JAXB element value that supports attribute from different namespaces. Attributes from
     *                         different namespaces are supported if the JAXB element value possesses a function
     *                         {@code getOtherAttributes()} to retrieve a map of qualified names, i.e. attribute keys,
     *                         to values.
     * @return the extension element passed to the function for fluent interfacing.
     * @throws NoSuchMethodException     in case the {@code getOtherAttributes()} could not be found.
     * @throws ClassCastException        in case the result from {@code getOtherAttributes()} is not an object of type
     *                                   {@code java.util.Map<javax.xml.namespace.QName, String>}.
     * @throws InvocationTargetException in case a call {@code getOtherAttributes()} throws an exception.
     * @throws IllegalAccessException    the access to {@code getOtherAttributes()} was refused.
     */
    public static <T> JAXBElement<T> append(JAXBElement<T> extensionElement, boolean value)
            throws NoSuchMethodException, ClassCastException, InvocationTargetException, IllegalAccessException {
        var elementValue = extensionElement.getValue();
        var method = elementValue.getClass().getMethod(METHOD_NAME_GET_ATTRIBUTES);
        var attrMap =  (Map<QName, String>)method.invoke(elementValue);
        attrMap.put(CommonConstants.QNAME_MUST_UNDERSTAND_ATTRIBUTE, Boolean.toString(value));
        return extensionElement;
    }

    /**
     * Turns a JAXB XML element into a BICEPS extension with a specific MustUnderstand behavior.
     * <p>
     * In contrast to {@link #append(JAXBElement, boolean)} this call just logs a warning that the operation could not
     * be performed.
     *
     * @param extensionElement the element to attach a MustUnderstand attribute to
     * @param value            the MustUnderstand boolean value (true for required to be understood, false otherwise)
     * @param <T>              any JAXB element value that supports attribute from different namespaces. Attributes from
     *                         different namespaces are supported if the JAXB element value possesses a function
     *                         {@code getOtherAttributes()} to retrieve a map of qualified names, i.e. attribute keys,
     *                         to values.
     * @return the extension element passed to the function for fluent interfacing.
     */
    public static <T> JAXBElement<T> appendNoThrows(JAXBElement<T> extensionElement, boolean value) {
        try {
            return append(extensionElement, value);
        } catch (Exception e) {
            LOG.warn("MustUnderstand value could not be appended to {}. Reason: {}", extensionElement,
                    e.getMessage());
            LOG.trace("MustUnderstand value could not be appended to {}", extensionElement, e);
        }
        return extensionElement;
    }
}
