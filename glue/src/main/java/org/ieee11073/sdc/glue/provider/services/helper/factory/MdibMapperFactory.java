package org.ieee11073.sdc.glue.provider.services.helper.factory;

import com.google.inject.assistedinject.Assisted;
import org.ieee11073.sdc.biceps.common.access.MdibAccess;
import org.ieee11073.sdc.glue.provider.services.helper.MdibMapper;

/**
 * Factory to create {@linkplain MdibMapper} instances.
 */
public interface MdibMapperFactory {
    /**
     * Creates a new {@linkplain MdibMapper} that maps from {@linkplain MdibAccess} to {@linkplain org.ieee11073.sdc.biceps.model.participant.Mdib}.
     *
     * @param mdibAccess the MDIB access where to map data from.
     * @return a new instance to map to {@link org.ieee11073.sdc.biceps.model.participant.Mdib}.
     */
    MdibMapper createMdibMapper(@Assisted MdibAccess mdibAccess);
}
