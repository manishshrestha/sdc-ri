package org.ieee11073.sdc.biceps.provider.preprocessing;

import com.google.common.base.Joiner;
import org.ieee11073.sdc.biceps.common.*;
import org.ieee11073.sdc.biceps.common.preprocessing.TypeConsistencyException;
import org.ieee11073.sdc.biceps.common.storage.DescriptionPreprocessingSegment;
import org.ieee11073.sdc.biceps.common.storage.MdibStorage;
import org.ieee11073.sdc.biceps.model.participant.AbstractDescriptor;
import org.ieee11073.sdc.biceps.model.participant.MdsDescriptor;

import javax.inject.Inject;
import java.util.Optional;

/**
 * Preprocessing segment that ensures correctness of child-parent type relationships.
 */
public class TypeConsistencyChecker implements DescriptionPreprocessingSegment {
    private MdibTreeValidator treeValidator;

    @Inject
    TypeConsistencyChecker(MdibTreeValidator treeValidator) {
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

            // todo replace with Exception
            throw new TypeConsistencyException(String.format("Inserted entities other than MDS require a parent handle. " +
                    "Handle is %s, type is %s", descriptor.getHandle(), descriptor.getClass()));
        } else {
            if (descriptor instanceof MdsDescriptor) {
                throw new TypeConsistencyException(String.format("MDS shall not possess a parent handle. Handle is %s",
                        descriptor.getHandle()));
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
                            new TypeConsistencyException(String.format("No parent descriptor found with handle %s",
                                    currentModification.getParentHandle().get())));
        }

        if (!treeValidator.isValidParent(parentDescriptor, descriptor)) {
            throw new TypeConsistencyException(String.format("Parent descriptor of %s is invalid: %s. Valid parents: [%s].",
                    descriptor.getClass(),
                    parentDescriptor.getClass(),
                    Joiner.on(", ").join(treeValidator.allowedParents(descriptor))));
        }
    }
}
