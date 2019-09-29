package org.ieee11073.sdc.biceps.consumer.access.factory;

import org.ieee11073.sdc.biceps.consumer.access.RemoteMdibAccess;

/**
 * Factory to create {@linkplain RemoteMdibAccess} instances.
 */
public interface RemoteMdibAccessFactory {
    /**
     * Creates a local mdib access with an MDIB version that has a random sequence id.
     * <p>
     * The following preprocessing steps are visited in the following order:
     * <ol>
     * <li>{@link org.ieee11073.sdc.biceps.common.preprocessing.DuplicateChecker}
     * <li>{@link org.ieee11073.sdc.biceps.common.preprocessing.TypeConsistencyChecker}
     * <li>{@link org.ieee11073.sdc.biceps.consumer.preprocessing.VersionDuplicateHandler}
     * </ol>
     *
     * @return the remote mdib access instance.
     */
    RemoteMdibAccess createRemoteMdibAccess();
}
