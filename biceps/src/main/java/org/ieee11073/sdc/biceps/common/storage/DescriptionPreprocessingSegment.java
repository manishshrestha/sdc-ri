package org.ieee11073.sdc.biceps.common.storage;

import org.ieee11073.sdc.biceps.common.MdibDescriptionModification;
import org.ieee11073.sdc.biceps.common.MdibDescriptionModifications;

/**
 * A segment that is applied during description modifications.
 */
public interface DescriptionPreprocessingSegment extends PreprocessingSegment {
    /**
     * In a sequence of modifications this function processes one modification.
     *
     * @param allModifications all modifications.
     * @param currentModification the current modification to be processed.
     * @param storage the MDIB storage for access.
     * @throws Exception an arbitrary exception if something goes wrong.
     */
    void process(MdibDescriptionModifications allModifications,
                 MdibDescriptionModification currentModification,
                 MdibStorage storage) throws Exception;
}
