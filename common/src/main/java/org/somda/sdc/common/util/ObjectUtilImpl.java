package org.somda.sdc.common.util;

import com.google.inject.Inject;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.JAXBSource;
import javax.xml.namespace.QName;

/**
 * Default implementation of {@linkplain ObjectUtil}.
 */
public class ObjectUtilImpl implements ObjectUtil {

    private final JAXBContext jaxbContext;
    private final Unmarshaller unmarshaller;

    @Inject
    public ObjectUtilImpl(JAXBContext jaxbContext) {
        this.jaxbContext = jaxbContext;
        unmarshaller = createUnmarshaller();
    }

    @Override
    public <T> T deepCopyJAXB(T object) {
        return deepCopyJAXB(object, (Class<T>) object.getClass());
    }

    private <T> T deepCopyJAXB(T object, Class<T> clazz) {
        try {
            JAXBElement<T> contentObject = new JAXBElement<>(new QName(clazz.getSimpleName()), clazz, object);
            JAXBSource source = new JAXBSource(jaxbContext, contentObject);
            return unmarshaller.unmarshal(source, clazz).getValue();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    private Unmarshaller createUnmarshaller() {
        try {
            return jaxbContext.createUnmarshaller();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }
}