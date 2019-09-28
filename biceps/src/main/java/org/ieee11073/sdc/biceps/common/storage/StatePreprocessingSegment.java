package org.ieee11073.sdc.biceps.common.storage;

import org.ieee11073.sdc.biceps.model.participant.AbstractState;

/**
 * A segment that is applied during state modifications.
 */
public interface StatePreprocessingSegment extends PreprocessingSegment {
    /**
     * In a sequence of modifications this function processes one modification.
     *
     * @param modification the current modification to be processed.
     * @param storage the MDIB storage for access.
     * @throws Exception an arbitrary exception if something goes wrong.
     */
    void process(AbstractState modification, MdibStorage storage) throws Exception;
}
