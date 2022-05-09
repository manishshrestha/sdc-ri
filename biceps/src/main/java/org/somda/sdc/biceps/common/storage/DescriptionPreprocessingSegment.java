package org.somda.sdc.biceps.common.storage;

import org.somda.sdc.biceps.common.MdibDescriptionModification;

import java.util.List;

/**
 * A segment that is applied during description modifications.
 */
public interface DescriptionPreprocessingSegment {
    /**
     * Function that is invoked before the first modification in the processing chain is applied.
     * <p>
     * Default behavior is <em>do nothing</em>.
     *
     * @param modifications all modifications for preprocessing.
     * @param mdibStorage   the MDIB storage to be used by the callback.
     * @return updated set of modifications
     */
    default List<MdibDescriptionModification> beforeFirstModification(
        List<MdibDescriptionModification> modifications,
        MdibStorage mdibStorage
    ) {
        return modifications;
    }

    /**
     * Function that is invoked after the last modification in the processing chain has been applied.
     * <p>
     * Default behavior is <em>do nothing</em>.
     *
     * @param modifications all modifications for preprocessing.
     * @param mdibStorage   the MDIB storage to be used by the callback.
     * @return updated set of modifications
     */
    default List<MdibDescriptionModification> afterLastModification(
        List<MdibDescriptionModification> modifications,
        MdibStorage mdibStorage
    ) {
        return modifications;
    }

    /**
     * In a sequence of modifications this function processes one modification.
     *
     * @param modifications all modifications.
     * @param storage       the MDIB storage for access.
     * @return updated set of modifications
     * @throws Exception an arbitrary exception if something goes wrong.
     */
    List<MdibDescriptionModification> process(List<MdibDescriptionModification> modifications,
                                         MdibStorage storage) throws Exception;
}
