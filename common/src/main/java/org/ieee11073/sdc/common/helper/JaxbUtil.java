package org.ieee11073.sdc.common.helper;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.util.List;
import java.util.Optional;

/**
 * Utility functions for JAXB.
 */
public interface JaxbUtil {

    /**
     * Try to cast the element to given type object and return it as an {@link Optional}.
     *
     * If anything fails, {@link Optional#empty()} is returned.
     *
     * @param element   The element to inspect.
     * @param typeClass The QName type specification.
     * @param <T>       The casted generic type
     * @return {@link Optional} of {@link JAXBElement#getValue()} or {@link Optional#empty()} on error.
     */
    <T> Optional<T> extractElement(Object element, Class<T> typeClass);

    /**
     * Try to cast the element to a {@link JAXBElement} object with given QName type and return value as
     * an {@link Optional}.
     *
     * If anything fails, {@link Optional#empty()} is returned.
     *
     * @param element     The element to inspect.
     * @param elementType The QName type specification.
     * @param <T>         The casted generic type
     * @return {@link Optional} of {@link JAXBElement#getValue()} or {@link Optional#empty()} on error.
     */
    <T> Optional<T> extractElement(Object element, QName elementType);

    /**
     * Same as {@link #extractElement(Object, QName)}, but with QName JAXB type class in addition.
     *
     * @param element     The element to inspect.
     * @param elementType The QName type specification.
     * @param typeClass   Java JAXB class that matches elementType.
     * @param <T>         The casted generic type
     * @return {@link Optional} of {@link JAXBElement#getValue()} or {@link Optional#empty()} on error.
     */
    <T> Optional<T> extractElement(Object element, QName elementType, Class<T> typeClass);

    /**
     * From a list of {@link JAXBElement} objects, this function retrieves the first element in the list and tries
     * to cast if to the QName type given by elementType.
     *
     * @param anyList     A list of {@link JAXBElement} objects.
     * @param elementType The QName type specification.
     * @param typeClass   Java JAXB class that matches elementType.
     * @param <T>         The casted generic type
     * @return {@link Optional} of the first element's {@link JAXBElement#getValue()} or {@link Optional#empty()} when
     * the list is empty or an error occurs.
     */
    <T> Optional<T> extractFirstElementFromAny(List<Object> anyList, QName elementType, Class<T> typeClass);

    /**
     * Same as {@link #extractFirstElementFromAny(List, QName, Class)}, but without comparing QName in advance.
     *
     * @param anyList     A list of {@link JAXBElement} objects.
     * @param typeClass   Java JAXB class that matches elementType.
     * @param <T>         The casted generic type
     * @return {@link Optional} of the first element's {@link JAXBElement#getValue()} or {@link Optional#empty()} when
     * the list is empty or an error occurs.
     */
    <T> Optional<T> extractFirstElementFromAny(List<Object> anyList, Class<T> typeClass);
}
