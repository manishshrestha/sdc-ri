package org.ieee11073.sdc.biceps.provider.preprocessing;

import com.google.inject.Inject;
import org.ieee11073.sdc.biceps.common.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Preprocessing segment that checks for handle duplicates on inserted entities during description modifications.
 */
public class DuplicateDetector implements DescriptionPreprocessingSegment {
    private final Set<String> handleCache;

    @Inject
    DuplicateDetector() {
        handleCache = new HashSet<>();
    }

    @Override
    public void beforeFirstModification(MdibStorage mdibStorage) {
        handleCache.clear();
    }

    @Override
    public void process(MdibDescriptionModifications allModifications,
                        MdibDescriptionModification currentModification,
                        MdibStorage storage) throws Exception {
        if (currentModification.getModificationType() == MdibDescriptionModification.Type.INSERT) {
            if (storage.getEntity(currentModification.getHandle()).isPresent()) {
                throw new HandleDuplicatedException(String.format("Inserted handle is a duplicate: %s",
                        currentModification.getHandle()));
            }
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
