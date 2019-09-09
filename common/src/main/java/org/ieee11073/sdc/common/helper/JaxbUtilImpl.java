package org.ieee11073.sdc.common.helper;

import org.w3c.dom.Element;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.util.List;
import java.util.Optional;

public class JaxbUtilImpl implements JaxbUtil {
    @Override
    public <T> Optional<T> extractElement(Object element, QName elementType, Class<T> typeClass) {
        Optional<Object> extractedObj = extractElement(element, elementType);
        if (extractedObj.isPresent()) {
            if (typeClass.isAssignableFrom(extractedObj.get().getClass())) {
                return Optional.ofNullable(typeClass.cast(extractedObj.get()));
            }
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> extractElement(Object element, Class<T> typeClass) {
        try {
            if (element instanceof JAXBElement) {
                T value = ((JAXBElement<T>) element).getValue();
                return Optional.of(typeClass.cast(value));
            } else {
                return Optional.of(typeClass.cast(element));
            }
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> extractElement(Object element, QName elementType) {
        try {
            JAXBElement<T> elementAsJaxb = (JAXBElement<T>) element;
            if (elementAsJaxb.getName().equals(elementType)) {
                return Optional.ofNullable(elementAsJaxb.getValue());
            }
        } catch (Exception e) {
            if (element instanceof Element) {
                throw new RuntimeException("JAXB object conversion failed. Make sure the expected class is known to JAXB via context path.");
            }
            // ignore, empty optional will be returned
        }
        return Optional.empty();
    }

    @Override
    public <T> Optional<T> extractFirstElementFromAny(List<Object> anyList, QName elementType, Class<T> typeClass) {
        if (anyList.isEmpty()) {
            return Optional.empty();
        }

        Optional<T> first = extractElement(anyList.get(0), elementType, typeClass);
        if (first.isPresent()) {
            try {
                return Optional.ofNullable(typeClass.cast(first.get()));
            } catch (Exception e) {
                // ignore, empty optional will be returned
            }
        }

        return Optional.empty();
    }

    @Override
    public <T> Optional<T> extractFirstElementFromAny(List<Object> anyList, Class<T> typeClass) {
        if (anyList.isEmpty()) {
            return Optional.empty();
        }

        Optional<T> first = extractElement(anyList.get(0), typeClass);
        if (first.isPresent()) {
            try {
                return Optional.ofNullable(typeClass.cast(first.get()));
            } catch (Exception e) {
                // ignore, empty optional will be returned
            }
        }

        return Optional.empty();
    }
}
