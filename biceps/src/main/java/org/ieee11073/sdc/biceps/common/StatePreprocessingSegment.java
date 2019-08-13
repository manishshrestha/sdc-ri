package org.ieee11073.sdc.biceps.common;

import org.ieee11073.sdc.biceps.model.participant.AbstractState;

public interface StatePreprocessingSegment extends PreprocessingSegment {
    void process(AbstractState modification, MdibStorage storage) throws Exception;
}
