package org.ieee11073.sdc.biceps.common.storage.factory;

import com.google.inject.assistedinject.Assisted;
import org.ieee11073.sdc.biceps.common.storage.MdibStorage;
import org.ieee11073.sdc.biceps.model.participant.MdibVersion;

/**
 * Factory to create {@linkplain MdibStorage} instances.
 */
public interface MdibStorageFactory {
    /**
     * Creates a storage initialized with the given MDIB version.
     *
     * @param mdibVersion the initial MDIB version.
     * @return an {@link MdibStorage} instance.
     */
    MdibStorage createMdibStorage(@Assisted MdibVersion mdibVersion);

    /**
     * Creates a storage initialized with a random MDIB version.
     *
     * @return an {@link MdibStorage} instance. The random MDIB version consists of a random sequence id and zeroed
     * instance id and version counter.
     */
    MdibStorage createMdibStorage();
}
