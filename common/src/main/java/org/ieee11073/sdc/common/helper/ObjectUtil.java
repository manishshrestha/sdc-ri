package org.ieee11073.sdc.common.helper;

/**
 * Utility class to deep copy arbitrary objects.
 */
public interface ObjectUtil {
    /**
     * Creates deep copy of given object.
     *
     * @param obj the object to copy.
     * @param <T> any copyable object.
     * @return deep copy of given object.
     */
    <T> T deepCopy(T obj);
}
