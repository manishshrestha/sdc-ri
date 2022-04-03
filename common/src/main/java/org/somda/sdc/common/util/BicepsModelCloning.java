package org.somda.sdc.common.util;

/**
 * Creates deep copies of BICEPS model objects.
 */
public interface BicepsModelCloning {
    /**
     * Creates a deep copy of given BICEPS model object.
     *
     * @param object the object to copy.
     * @param <T> any copyable object.
     * @return deep copy of given object.
     */
    <T> T deepCopy(T object);
}
