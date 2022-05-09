package org.somda.sdc.biceps.provider.preprocessing;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.common.MdibDescriptionModification;
import org.somda.sdc.biceps.common.MdibEntity;
import org.somda.sdc.biceps.common.MdibTreeValidator;
import org.somda.sdc.biceps.common.storage.DescriptionPreprocessingSegment;
import org.somda.sdc.biceps.common.storage.MdibStorage;
import org.somda.sdc.biceps.model.participant.AbstractDescriptor;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;

import java.util.List;
import java.util.Optional;

/**
 * Preprocessing segment that verifies correctness of child cardinality.
 * <p>
 * In BICEPS descriptors of a certain type can appear one or many times as a child of another descriptor.
 * This checker guarantees that no child is inserted twice if the maximum allowed number of children is one.
 */
public class CardinalityChecker implements DescriptionPreprocessingSegment {
    private static final Logger LOG = LogManager.getLogger(CardinalityChecker.class);

    private final MdibTreeValidator treeValidator;
    private final Logger instanceLogger;

    @Inject
    CardinalityChecker(MdibTreeValidator treeValidator,
                       @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.treeValidator = treeValidator;
    }

    @Override
    public List<MdibDescriptionModification> process(List<MdibDescriptionModification> modifications,
                                                     MdibStorage storage) throws CardinalityException {
        for (final MdibDescriptionModification currentModification : modifications) {
            if (currentModification.getModificationType() != MdibDescriptionModification.Type.INSERT) {
                // Only insert is affected
                continue;
            }

            final Optional<String> parentHandle = currentModification.getParentHandle();
            if (parentHandle.isEmpty()) {
                // No parent means: MDS, which may occur many times
                continue;
            }

            final AbstractDescriptor descriptor = currentModification.getDescriptor();
            if (isSameTypeInModifications(modifications, descriptor, parentHandle.get()) &&
                    !treeValidator.isManyAllowed(descriptor)) {
                // There is no other child of the same type to be inserted below the parent
                throwException(descriptor);
            }

            final Optional<MdibEntity> parentEntityFromStorage = storage.getEntity(parentHandle.get());
            if (parentEntityFromStorage.isEmpty()) {
                // No parent in the storage yet - early exit
                instanceLogger.warn("Expected a parent in the MDIB storage, but none found: {}", parentHandle.get());
                continue;
            }

            if (!storage.getChildrenByType(parentHandle.get(), descriptor.getClass()).isEmpty() &&
                    !treeValidator.isManyAllowed(descriptor)) {
                // There is at least one child of given type, but multiple children of that type are not allowed
                throwException(descriptor);
            }

        }

        // Every condition passed, insertion granted
        return modifications;
    }

    private boolean isSameTypeInModifications(List<MdibDescriptionModification> modifications,
                                              AbstractDescriptor descriptor,
                                              String parentHandle) {
        return modifications.stream()
                .filter(mod -> mod.getModificationType() == MdibDescriptionModification.Type.INSERT)
                .filter(mod -> !mod.getDescriptor().getHandle().equals(descriptor.getHandle()))
                .filter(mod -> mod.getParentHandle().isPresent() && mod.getParentHandle().get().equals(parentHandle))
                .anyMatch(mod -> mod.getDescriptor().getClass().equals(descriptor.getClass()));
    }

    private void throwException(AbstractDescriptor descriptor) throws CardinalityException {
        throw new CardinalityException(String.format("Type %s is not allowed to appear more than once as a " +
                        "child (see handle %s)",
                descriptor.getClass().getSimpleName(),
                descriptor.getHandle()));
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
