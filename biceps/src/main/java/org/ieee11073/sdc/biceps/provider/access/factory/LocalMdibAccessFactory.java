package org.ieee11073.sdc.biceps.provider.access.factory;

import org.ieee11073.sdc.biceps.provider.access.LocalMdibAccess;

/**
 * Factory to create {@linkplain LocalMdibAccess} instances.
 */
public interface LocalMdibAccessFactory {
    /**
     * Creates a local mdib access with an MDIB version that has a random sequence id.
     * <p>
     * The following preprocessing steps are visited in the following order:
     * <ol>
     * <li>{@link org.ieee11073.sdc.biceps.common.preprocessing.DuplicateChecker}
     * <li>{@link org.ieee11073.sdc.biceps.common.preprocessing.TypeConsistencyChecker}
     * <li>{@link org.ieee11073.sdc.biceps.provider.preprocessing.VersionHandler}
     * </ol>
     *
     * @return the local mdib access instance.
     */
    LocalMdibAccess createLocalMdibAccess();
}
