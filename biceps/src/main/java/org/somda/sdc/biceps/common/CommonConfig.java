package org.somda.sdc.biceps.common;

import org.somda.sdc.biceps.common.storage.MdibStorage;

import java.util.List;

/**
 * General configuration of the BICEPS common package.
 *
 * @see org.somda.sdc.biceps.guice.DefaultBicepsConfigModule
 */
public class CommonConfig {
    /**
     * If true any input to an MDIB is copied before stored in the {@link MdibStorage}.
     * <p>
     * This inhibits the user from changing the data stored in the {@link MdibStorage}.
     * <em>Not being able to change the data after writing is at the expense of copying memory!</em>
     * <ul>
     * <li>Data type: {@linkplain Boolean}
     * <li>Use: optional
     * </ul>
     */
    public static final String COPY_MDIB_INPUT = "Biceps.Common.CopyMdibInput";

    /**
     * If true any output from an MDIB is copied before exposed to the user.
     * <p>
     * This inhibits the user from changing the data stored in the {@link MdibStorage}.
     * <em>Not being able to change the data while reading is at the expense of copying memory!</em>
     * <ul>
     * <li>Data type: {@linkplain Boolean}
     * <li>Use: optional
     * </ul>
     */
    public static final String COPY_MDIB_OUTPUT = "Biceps.Common.CopyMdibInput";

    /**
     * If true context states which are not associated are not stored.
     * <p>
     * Since there is no mechanism which allows discarding context states, enabling this feature
     * automatically prunes likely irrelevant data from the storage.
     * <ul>
     * <li>Data type: {@linkplain Boolean}
     * <li>Use: optional
     * </ul>
     */
    public static final String STORE_NOT_ASSOCIATED_CONTEXT_STATES = "Biceps.Common.StoreNotAssociatedContextStates";

    /**
     * If true, states can be added to the storage without having a descriptor present.
     * <p>
     * If a consumer is only interested in state updates, it may not want to know about or care about
     * having the descriptor in the storage.
     * <em>Inserting a state without a descriptor will throw a RuntimeException if this is disabled!</em>
     * <ul>
     * <li>Data type: {@linkplain Boolean}
     * <li>Use: optional
     * </ul>
     */
    public static final String ALLOW_STATES_WITHOUT_DESCRIPTORS = "Biceps.Common.AllowStatesWithoutDescriptors";

    /**
     * A list of all StatePreprocessingSegments, which are applied during state modifications.
     * <p>
     * A consumer can specify which StatePreprocessingSegments should be used, by adding them to the list.
     * <ul>
     * <li>Data type: {@link List}
     * <li>Use: optional
     * </ul>
     */
    public static final String CONSUMER_PREPROCESSING_SEGMENTS = "Biceps.Common.ConsumerPreprocessingSegments";
}
