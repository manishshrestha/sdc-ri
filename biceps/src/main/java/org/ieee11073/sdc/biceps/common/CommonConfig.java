package org.ieee11073.sdc.biceps.common;

import org.ieee11073.sdc.biceps.common.storage.MdibStorage;

/**
 * General configuration of the BICEPS common package.
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
}
