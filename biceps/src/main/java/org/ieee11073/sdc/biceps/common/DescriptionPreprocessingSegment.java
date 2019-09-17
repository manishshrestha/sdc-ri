package org.ieee11073.sdc.biceps.common;

public interface DescriptionPreprocessingSegment extends PreprocessingSegment {
    void process(MdibDescriptionModifications allModifications,
                 MdibDescriptionModification currentModification,
                 MdibStorage storage) throws Exception;
}
