package org.ieee11073.sdc.biceps.provider.access.factory;

import org.ieee11073.sdc.biceps.provider.access.LocalMdibAccess;
import org.ieee11073.sdc.biceps.provider.preprocessing.DuplicateChecker;
import org.ieee11073.sdc.biceps.provider.preprocessing.TypeConsistencyChecker;
import org.ieee11073.sdc.biceps.provider.preprocessing.VersionHandler;

/**
 * Factory to create {@linkplain LocalMdibAccess} instances.
 */
public interface LocalMdibAccessFactory {
    /**
     * Creates a local mdib access with an MDIB version that has a random sequence id.
     * <p>
     * The following preprocessing steps are visited in the following order:
     * <ol>
     * <li>{@link DuplicateChecker}
     * <li>{@link TypeConsistencyChecker}
     * <li>{@link VersionHandler}
     * </ol>
     *
     * @return the local mdib access instance.
     */
    LocalMdibAccess createLocalMdibAccess();
}
