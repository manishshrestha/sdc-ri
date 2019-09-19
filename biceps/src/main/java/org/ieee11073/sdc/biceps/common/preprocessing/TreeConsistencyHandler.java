package org.ieee11073.sdc.biceps.common.preprocessing;

import com.google.common.base.Joiner;
import org.ieee11073.sdc.biceps.common.*;
import org.ieee11073.sdc.biceps.model.participant.AbstractDescriptor;
import org.ieee11073.sdc.biceps.model.participant.MdsDescriptor;

import javax.inject.Inject;
import java.util.Optional;

/**
 * Preprocessing segment that ensures correctness of child-parent type relationships.
 */
public class TreeConsistencyHandler implements DescriptionPreprocessingSegment {
    private MdibTreeValidator treeValidator;

    @Inject
    TreeConsistencyHandler(MdibTreeValidator treeValidator) {
        this.treeValidator = treeValidator;
    }

    @Override
    public void process(MdibDescriptionModifications allModifications,
                        MdibDescriptionModification currentModification,
                        MdibStorage storage) throws Exception {
        if (currentModification.getModificationType() != MdibDescriptionModification.Type.INSERT) {
            return;
        }

        AbstractDescriptor descriptor = currentModification.getDescriptor();
        if (currentModification.getParentHandle().isEmpty()) {
            if (descriptor instanceof MdsDescriptor) {
                return;
            }

            throw new Exception(String.format("Inserted entities other than MDS require a parent handle: %s",
                    descriptor.getClass()));
        } else {
            if (descriptor instanceof MdsDescriptor) {
                throw new Exception(String.format("MDS shall not possess a parent handle",
                        descriptor.getClass()));
            }
        }

        Optional<AbstractDescriptor> parentFromStorage = storage.getDescriptor(currentModification.getParentHandle().get());
        AbstractDescriptor parentDescriptor;
        if (parentFromStorage.isPresent()) {
            parentDescriptor = parentFromStorage.get();
        } else {
            parentDescriptor = allModifications.getModifications().parallelStream()
                    .filter(mod -> mod.getModificationType() == MdibDescriptionModification.Type.INSERT)
                    .filter(mod -> mod.getDescriptor().getHandle().equals(currentModification.getParentHandle().get()))
                    .map(mod -> mod.getDescriptor())
                    .findAny().orElseThrow(() ->
                            new Exception(String.format("No parent descriptor found with handle %s",
                                    currentModification.getParentHandle().get())));
        }

        if (!treeValidator.isValidParent(parentDescriptor, descriptor)) {
            throw new Exception(String.format("Parent descriptor of %s is invalid: %s. Valid parents: [%s].",
                    descriptor.getClass(),
                    parentDescriptor.getClass(),
                    Joiner.on(", ").join(treeValidator.allowedParents(descriptor))));
        }
    }
}
