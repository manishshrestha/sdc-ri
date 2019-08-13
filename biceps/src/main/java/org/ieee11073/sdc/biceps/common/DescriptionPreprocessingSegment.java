package org.ieee11073.sdc.biceps.common;

public interface DescriptionPreprocessingSegment extends PreprocessingSegment {
    void process(MdibDescriptionModifications modifications,
                 MdibDescriptionModification modification,
                 MdibStorage storage) throws Exception;
}
