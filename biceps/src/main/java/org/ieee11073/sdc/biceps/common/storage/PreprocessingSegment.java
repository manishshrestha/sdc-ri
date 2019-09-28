package org.ieee11073.sdc.biceps.common.storage;

/**
 * Common preprocessing segment.
 */
public interface PreprocessingSegment {

    /**
     * Function that is invoked before the first modification in a processing chain is applied.
     * <p>
     * Default behavior is <em>do nothing</em>.
     *
     * @param mdibStorage the MDIB storage to be used by the callback.
     */
    default void beforeFirstModification(MdibStorage mdibStorage) {
    }

    /**
     * Function that is invoked after the last modification in a processing chain has been applied.
     * <p>
     * Default behavior is <em>do nothing</em>.
     *
     * @param mdibStorage the MDIB storage to be used by the callback.
     */
    default void afterLastModification(MdibStorage mdibStorage) {
    }
}