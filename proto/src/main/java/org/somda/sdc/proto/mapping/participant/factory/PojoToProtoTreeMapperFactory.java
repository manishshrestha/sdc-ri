package org.somda.sdc.proto.mapping.participant.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.biceps.common.access.MdibAccess;
import org.somda.sdc.proto.mapping.participant.PojoToProtoTreeMapper;

/**
 * Factory to create {@linkplain PojoToProtoTreeMapper} instances.
 */
public interface PojoToProtoTreeMapperFactory {
    /**
     * Creates a new {@linkplain PojoToProtoTreeMapper} that maps from {@linkplain MdibAccess}
     * to {@linkplain org.somda.protosdc.proto.model.biceps.MdibMsg}.
     *
     * @param mdibAccess the MDIB access where to map data from.
     * @return a new instance to map to {@link org.somda.protosdc.proto.model.biceps.MdibMsg}.
     */
    PojoToProtoTreeMapper create(@Assisted MdibAccess mdibAccess);
}
