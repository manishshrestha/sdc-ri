package org.ieee11073.sdc.biceps.common.access;

public interface ReadTransaction extends MdibAccess, AutoCloseable {
    // Create default to avoid throwing a checked exception - not required for read transaction.
    default void close() {
    }
}
