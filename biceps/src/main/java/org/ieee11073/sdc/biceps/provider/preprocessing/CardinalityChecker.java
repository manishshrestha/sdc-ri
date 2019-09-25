package org.ieee11073.sdc.biceps.provider.preprocessing;

import com.google.inject.Inject;
import org.ieee11073.sdc.biceps.common.*;
import org.ieee11073.sdc.biceps.model.participant.AbstractDescriptor;

import java.util.Optional;

/**
 * Preprocessing segment that verifies correctness of child cardinality.
 * <p>
 * In BICEPS descriptors of a certain type can appear one or many times as a child of another descriptor.
 * This checker guarantees that no child is inserted twice if the maximum allowed number of children is one.
 */
public class CardinalityChecker implements DescriptionPreprocessingSegment {
    private final MdibTreeValidator treeValidator;

    @Inject
    CardinalityChecker(MdibTreeValidator treeValidator) {

        this.treeValidator = treeValidator;
    }

    @Override
    public void process(MdibDescriptionModifications allModifications,
                        MdibDescriptionModification currentModification,
                        MdibStorage storage) throws CardinalityException {
        if (currentModification.getModificationType() != MdibDescriptionModification.Type.INSERT) {
            // Only insert is affected
            return;
        }

        final Optional<String> parentHandle = currentModification.getParentHandle();
        if (parentHandle.isEmpty()) {
            // No parent means: MDS, which may occur many times
            return;
        }

        final AbstractDescriptor descriptor = currentModification.getDescriptor();
        if (isSameTypeInModifications(allModifications, descriptor, parentHandle.get()) &&
                !treeValidator.isManyAllowed(descriptor)) {
            // There is no other child of the same type to be inserted below the parent
            throwException(descriptor);
        }

        final Optional<MdibEntity> parentEntityFromStorage = storage.getEntity(parentHandle.get());
        if (parentEntityFromStorage.isEmpty()) {
            // No parent in the storage yet - early exit
            return;
        }

        if (storage.getChildrenByType(parentHandle.get(), descriptor.getClass()).size() > 0 &&
                !treeValidator.isManyAllowed(descriptor)) {
            // There is at least one child of given type, but multiple children of that type are not allowed
            throwException(descriptor);
        }

        // Every condition passed, insertion granted
    }

    private boolean isSameTypeInModifications(MdibDescriptionModifications modifications,
                                              AbstractDescriptor descriptor,
                                              String parentHandle) {
        return modifications.getModifications().parallelStream()
                .filter(mod -> mod.getModificationType() == MdibDescriptionModification.Type.INSERT)
                .filter(mod -> !mod.getDescriptor().getHandle().equals(descriptor.getHandle()))
                .filter(mod -> mod.getParentHandle().isPresent() && mod.getParentHandle().get().equals(parentHandle))
                .filter(mod -> mod.getDescriptor().getClass().equals(descriptor.getClass()))
                .findAny().isPresent();
    }

    private void throwException(AbstractDescriptor descriptor) throws CardinalityException {
        throw new CardinalityException(String.format("Type %s is not allowed to appear more than once as a " +
                        "child (see handle %s)",
                descriptor.getClass().getSimpleName(),
                descriptor.getHandle()));
    }
}
