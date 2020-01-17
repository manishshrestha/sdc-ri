package org.somda.sdc.common.util;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.util.List;
import java.util.Optional;

/**
 * Default implementation of {@linkplain JaxbUtil}.
 */
public class JaxbUtilImpl implements JaxbUtil {
    private static final Logger LOG = LoggerFactory.getLogger(JaxbUtilImpl.class);

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
        } catch (ClassCastException e) {
            LOG.trace("Object was not a JAXBElement, extracting elements failed but it's alright.");
        } catch (Exception e) {
            LOG.warn("Element could not be extracted. Is the QName {} known to JAXB via context path?. " +
                    "Exception message: {}", elementType, e.getMessage());
            LOG.trace("Element could not be extracted.", e);
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
