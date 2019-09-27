package org.ieee11073.sdc.biceps.common.storage;

public interface PreprocessingSegment {
    default void beforeFirstModification(MdibStorage mdibStorage) {
    }

    default void afterLastModification(MdibStorage mdibStorage) {
    }
}