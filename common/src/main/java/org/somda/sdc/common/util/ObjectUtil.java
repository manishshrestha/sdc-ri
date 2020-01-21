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

    /**
     * Given a getter/setter bean, this function creates an immutable facade.
     * <p>
     * The facade then throws runtime exceptions when tried to be modified.
     * <p>
     * <em>Attention: implementations may reject instances of inner classes!</em>
     *
     * @param instance the object to convert to an immutable.
     * @param <C>      type of the object that is supposed to be made immutable.
     * @throws RuntimeException in case object facade could not be created.
     * @return given instance as immutable facade.
     */
    <C> C immutableFacade(C instance);
}
