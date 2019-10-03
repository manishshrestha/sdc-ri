package org.ieee11073.sdc.common.util;

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

    /**
     * Given a getter/setter bean, this function creates an immutable facade that throws runtime exception when tried
     * to be modified.
     *
     * <b>Attention: implementations may reject instances of inner classes!</b>
     *
     * @throws RuntimeException In case object facade could not be created.
     */
    <C> C immutableFacade(C instance);
}
