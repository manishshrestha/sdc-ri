package org.ieee11073.sdc.biceps.common.storage;

import org.ieee11073.sdc.biceps.common.MdibDescriptionModification;
import org.ieee11073.sdc.biceps.common.MdibDescriptionModifications;

public interface DescriptionPreprocessingSegment extends PreprocessingSegment {
    void process(MdibDescriptionModifications allModifications,
                 MdibDescriptionModification currentModification,
                 MdibStorage storage) throws Exception;
}
