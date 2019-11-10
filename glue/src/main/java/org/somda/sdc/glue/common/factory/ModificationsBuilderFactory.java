package org.somda.sdc.glue.common.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.biceps.model.participant.Mdib;
import org.somda.sdc.glue.common.ModificationsBuilder;

/**
 * Factory to create {@linkplain ModificationsBuilder} instances.
 */
public interface ModificationsBuilderFactory {
    /**
     * Creates a {@linkplain ModificationsBuilder} instance.
     *
     * @param mdib the MDIB used to create the modifications from.
     * @return a new {@linkplain ModificationsBuilder} instance.
     */
    ModificationsBuilder createModificationsBuilder(@Assisted Mdib mdib);
}
