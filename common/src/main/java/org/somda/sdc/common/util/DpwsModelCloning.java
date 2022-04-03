package org.somda.sdc.common.util;

/**
 * Creates deep copies of DPWS model objects.
 */
public interface DpwsModelCloning {
    /**
     * Creates a deep copy of given DPWS model object.
     *
     * @param object the object to copy.
     * @param <T> DPWS model class.
     * @return deep copy of given object.
     */
    <T> T deepCopy(T object);
}
