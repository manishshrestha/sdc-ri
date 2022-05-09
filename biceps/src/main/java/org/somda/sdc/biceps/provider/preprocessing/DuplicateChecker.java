package org.somda.sdc.biceps.provider.preprocessing;

import com.google.inject.Inject;
import org.somda.sdc.biceps.common.MdibDescriptionModification;
import org.somda.sdc.biceps.common.storage.DescriptionPreprocessingSegment;
import org.somda.sdc.biceps.common.storage.MdibStorage;

import java.util.List;

/**
 * Preprocessing segment that checks for handle duplicates on inserted entities during description modifications.
 */
public class DuplicateChecker implements DescriptionPreprocessingSegment {

    @Inject
    DuplicateChecker() {}

    @Override
    public List<MdibDescriptionModification> process(List<MdibDescriptionModification> modifications,
                                                     MdibStorage storage) throws Exception {
        for (final MdibDescriptionModification currentModification : modifications) {
            if (currentModification.getModificationType() == MdibDescriptionModification.Type.INSERT) {
                if (storage.getEntity(currentModification.getHandle()).isPresent()) {
                    throw new HandleDuplicatedException(String.format("Inserted handle is a duplicate: %s",
                        currentModification.getHandle()));
                }
            }
        }
        return modifications;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
