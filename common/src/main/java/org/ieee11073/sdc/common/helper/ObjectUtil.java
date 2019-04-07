package org.ieee11073.sdc.common.helper;

/**
 * Utility class to deep copy arbitrary objects.
 */
public interface ObjectUtil {
    /**
     * Create deep copy of given object
     * @param obj Object to copy.
     * @return Deep copy of given object or null if given object is null.
     */
    <T> T deepCopy(T obj);
}
