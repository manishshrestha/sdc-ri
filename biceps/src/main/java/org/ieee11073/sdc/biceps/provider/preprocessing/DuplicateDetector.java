package org.ieee11073.sdc.biceps.provider.preprocessing;

import com.google.inject.Inject;
import org.ieee11073.sdc.biceps.common.MdibDescriptionModification;
import org.ieee11073.sdc.biceps.common.MdibStorage;
import org.ieee11073.sdc.biceps.common.PreprocessingException;
import org.ieee11073.sdc.biceps.common.PreprocessingSegment;

import java.util.HashSet;
import java.util.Set;

public class DuplicateDetector extends PreprocessingSegment<MdibDescriptionModification> {
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
    public void process(MdibDescriptionModification modification, MdibStorage storage) throws Exception {
        if (modification.getModificationType() == MdibDescriptionModification.Type.INSERT) {
            if (storage.getEntity(modification.getHandle()).isPresent()) {
                throw new HandleDuplicatedException(String.format("Inserted handle is a duplicate: %s",
                        modification.getHandle()));
            }
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
