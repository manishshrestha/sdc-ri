package org.somda.sdc.proto.mapping.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.glue.common.DefaultStateValues;
import org.somda.sdc.proto.mapping.ProtoToPojoModificationsBuilder;
import org.somda.sdc.proto.model.biceps.MdibMsg;

import javax.annotation.Nullable;

/**
 * Factory to create {@linkplain ProtoToPojoModificationsBuilder} instances.
 */
public interface ProtoToPojoModificationsBuilderFactory {
    /**
     * Creates a {@linkplain ProtoToPojoModificationsBuilder} instance.
     * <p>
     * <em>Important note: the MDIB passed to the {@linkplain ProtoToPojoModificationsBuilder} will be modified.
     * Make sure to pass a copy if necessary.</em>
     *
     * @param mdibMsg the proto-based MDIB used to create the modifications from.
     * @return a new {@linkplain ProtoToPojoModificationsBuilder}.
     * @throws RuntimeException if a single state descriptor does not have a corresponding state.
     */
    ProtoToPojoModificationsBuilder create(@Assisted MdibMsg mdibMsg);

    /**
     * Creates a {@linkplain ProtoToPojoModificationsBuilder} instance.
     * <p>
     * <em>Important note: the MDIB passed to the {@linkplain ProtoToPojoModificationsBuilder} will be modified.
     * Make sure to pass a copy if necessary.</em>
     *
     * @param mdibMsg                    the MDIB used to create the modifications from.
     * @param createSingleStateIfMissing if true then the builder tries to create a missing single state; if false then
     *                                   a runtime exception is thrown is a single state is missing. If single states
     *                                   are added automatically, then
     *                                   {@link org.somda.sdc.glue.common.RequiredDefaultStateValues} will be used to
     *                                   populate default values.
     * @return a new {@linkplain ProtoToPojoModificationsBuilder} instance.
     */
    ProtoToPojoModificationsBuilder create(@Assisted MdibMsg mdibMsg,
                                           @Assisted Boolean createSingleStateIfMissing);

    /**
     * Creates a {@linkplain ProtoToPojoModificationsBuilder} instance.
     * <p>
     * <em>Important note: the MDIB passed to the {@linkplain ProtoToPojoModificationsBuilder} will be modified.
     * Make sure to pass a copy if necessary.</em>
     *
     * @param mdibMsg                    the proto-based MDIB used to create the modifications from.
     * @param createSingleStateIfMissing if true then the builder tries to create a missing single state; if false then
     *                                   a runtime exception is thrown is a single state is missing.
     * @param defaultStateValues         defines callbacks for the builder to apply default state values.
     *                                   If null, then {@link org.somda.sdc.glue.common.RequiredDefaultStateValues} will
     *                                   be used to populate default values.
     *                                   This parameter does only take effect if createSingleStateIfMissing is true.
     * @return a new {@linkplain ProtoToPojoModificationsBuilder} instance.
     */
    ProtoToPojoModificationsBuilder create(@Assisted MdibMsg mdibMsg,
                                           @Assisted Boolean createSingleStateIfMissing,
                                           @Assisted @Nullable DefaultStateValues defaultStateValues);
}
