package org.somda.sdc.common.util;

/**
 * Creates deep copies of arbitrary objects.
 */
public interface ObjectUtil {
    /**
     * Creates deep copy of given object.
     * <p>
     * <em>Attention: Do not clone nested classes, implementations might attempt to clone the surrounding object as well!</em>
     *
     * @param obj the object to copy.
     * @param <T> any copyable object.
     * @return deep copy of given object.
     */
    <T> T deepCopy(T obj);
}
