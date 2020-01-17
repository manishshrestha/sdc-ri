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
     * <p>
     * <em>Important note: the MDIB passed to the {@linkplain ModificationsBuilder} will be modified. Make sure to pass a
     * copy if necessary.</em>
     *
     * @param mdib the MDIB used to create the modifications from.
     * @return a new {@linkplain ModificationsBuilder}.
     * @throws RuntimeException if a single state descriptor does not have a corresponding state.
     */
    ModificationsBuilder createModificationsBuilder(@Assisted Mdib mdib);

    /**
     * Creates a {@linkplain ModificationsBuilder} instance.
     * <p>
     * <em>Important note: the MDIB passed to the {@linkplain ModificationsBuilder} will be modified. Make sure to pass a
     * copy if necessary.</em>
     *
     * @param mdib                       the MDIB used to create the modifications from.
     * @param createSingleStateIfMissing if true then the builder tries to create a missing single state; if false then
     *                                   a runtime exception is thrown is a single state is missing.
     * @return a new {@linkplain ModificationsBuilder} instance.
     */
    ModificationsBuilder createModificationsBuilder(@Assisted Mdib mdib,
                                                    @Assisted Boolean createSingleStateIfMissing);
}
