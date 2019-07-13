package org.ieee11073.sdc.common.helper;


/**
 * Utility class to deep copy arbitrary objects.
 */
public interface ObjectUtil {
    /**
     * Create deep copy of given object.
     *
     * @param obj Object to copy.
     * @return Deep copy of given object.
     * @throws RuntimeException In case object could not be deep-copied.
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
