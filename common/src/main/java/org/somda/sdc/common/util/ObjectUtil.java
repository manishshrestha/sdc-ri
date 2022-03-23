package org.somda.sdc.common.util;

/**
 * Creates deep copies of BICEPS and DPWS model objects.
 */
public interface ObjectUtil {
    /**
     * Creates a deep copy of given JAXB object.
     * There are separate {@linkplain ObjectUtil} instances based on BICEPS and DPWS models.
     *
     * @param object the object to copy.
     * @param <T> any copyable object.
     * @return deep copy of given object.
     */
    <T> T deepCopyJAXB(T object);
}
