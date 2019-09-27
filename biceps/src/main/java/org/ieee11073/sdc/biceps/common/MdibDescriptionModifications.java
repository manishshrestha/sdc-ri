package org.ieee11073.sdc.biceps.common;

import org.ieee11073.sdc.biceps.common.preprocessing.HandleDuplicatedException;
import org.ieee11073.sdc.biceps.model.participant.AbstractDescriptor;
import org.ieee11073.sdc.biceps.model.participant.AbstractMultiState;
import org.ieee11073.sdc.biceps.model.participant.AbstractState;
import org.ieee11073.sdc.biceps.model.participant.MdibVersion;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Container to collect changes supposed to be applied on an MDIB.
 * <p>
 * The {@linkplain MdibDescriptionModifications} is a fluent interface.
 */
public class MdibDescriptionModifications {
    private final MdibVersion mdibVersion;
    private List<MdibDescriptionModification> modifications;
    private Set<String> insertedHandles;
    private Set<String> updatedHandles;
    private Set<String> deletedHandles;

    /**
     * Creates a set.
     */
    public static MdibDescriptionModifications create() {
        return new MdibDescriptionModifications(null);
    }

    /**
     * Create set with version number.
     */
    public static MdibDescriptionModifications create(MdibVersion mdibVersion) {
        return new MdibDescriptionModifications(mdibVersion);
    }

    /**
     * Create set with version number form existing base.
     */
    public static MdibDescriptionModifications create(MdibVersion mdibVersion, MdibDescriptionModifications existingModifications) {
        MdibDescriptionModifications newModifications = new MdibDescriptionModifications(mdibVersion);
        newModifications.modifications = existingModifications.modifications;
        newModifications.insertedHandles = existingModifications.insertedHandles;
        newModifications.updatedHandles = existingModifications.updatedHandles;
        newModifications.deletedHandles = existingModifications.deletedHandles;

        return newModifications;
    }

    public MdibDescriptionModifications add(MdibDescriptionModification.Type modType,
                                            Entry entry) {
        return add(modType, entry.getDescriptor(), entry.getState(), entry.getParentHandle());
    }

    public MdibDescriptionModifications add(MdibDescriptionModification.Type modType,
                                            MultiStateEntry entry) {
        return add(modType, entry.getDescriptor(), entry.getStates(), entry.getParentHandle());
    }

    /**
     * Add single or multi state descriptor to change set without state information.
     * <p>
     * It's up to the change set processor to align state information.
     */
    public MdibDescriptionModifications add(MdibDescriptionModification.Type modType,
                                            AbstractDescriptor descriptor,
                                            @Nullable String parentHandle) {
        return addMdibModification(modType, descriptor, null, parentHandle);
    }

    /**
     * Add single or multi state descriptor to change set without state information.
     * <p>
     * It's up to the change set processor to align state information.
     */
    public MdibDescriptionModifications add(MdibDescriptionModification.Type modType,
                                            AbstractDescriptor descriptor) {
        return addMdibModification(modType, descriptor, null, null);
    }

    /**
     * Add single state descriptor to change set with state information.
     * <p>
     * Caveat: the change set processor might check descriptor state consistency.
     */
    public MdibDescriptionModifications add(MdibDescriptionModification.Type modType,
                                            AbstractDescriptor descriptor,
                                            @Nullable AbstractState state) {
        return addMdibModification(modType, descriptor, Collections.singletonList(state));
    }

    /**
     * Add single state descriptor to change set with state information.
     * <p>
     * Caveat: the change set processor might check descriptor state consistency.
     */
    public MdibDescriptionModifications add(MdibDescriptionModification.Type modType,
                                            AbstractDescriptor descriptor,
                                            @Nullable AbstractState state,
                                            @Nullable String parentHandle) {
        return addMdibModification(modType, descriptor, Collections.singletonList(state), parentHandle);
    }

    /**
     * Add multi state descriptor to change set with state information.
     * <p>
     * Caveat: the change set processor might check descriptor state consistency.
     */
    public MdibDescriptionModifications add(MdibDescriptionModification.Type modType,
                                            AbstractDescriptor descriptor,
                                            @Nullable List<? extends AbstractMultiState> multiStates) {
        multiStates.stream().forEach(state -> duplicateDetection(modType, state.getHandle()));
        return addMdibModification(modType, descriptor, multiStates);
    }

    /**
     * Add multi state descriptor to change set with state information.
     * <p>
     * Caveat: the change set processor might check descriptor state consistency.
     */
    public MdibDescriptionModifications add(MdibDescriptionModification.Type modType,
                                            AbstractDescriptor descriptor,
                                            @Nullable List<? extends AbstractMultiState> multiStates,
                                            @Nullable String parentHandle) {
        multiStates.stream().forEach(state -> duplicateDetection(modType, state.getHandle()));
        return addMdibModification(modType, descriptor, multiStates, parentHandle);
    }

    /**
     * Convenient function to insert a descriptor.
     *
     * @see #add(MdibDescriptionModification.Type, AbstractDescriptor)
     */
    public MdibDescriptionModifications insert(Entry entry) {
        return add(MdibDescriptionModification.Type.INSERT, entry.getDescriptor(), entry.getState(), entry.getParentHandle());
    }

    /**
     * Convenient function to insert a descriptor.
     *
     * @see #add(MdibDescriptionModification.Type, AbstractDescriptor)
     */
    public MdibDescriptionModifications insert(MultiStateEntry entry) {
        return add(MdibDescriptionModification.Type.INSERT, entry.getDescriptor(), entry.getStates(), entry.getParentHandle());
    }

    /**
     * Convenient function to insert a descriptor.
     *
     * @see #add(MdibDescriptionModification.Type, AbstractDescriptor)
     */
    public MdibDescriptionModifications insert(AbstractDescriptor descriptor) {
        return add(MdibDescriptionModification.Type.INSERT, descriptor);
    }

    /**
     * Convenient function to insert a descriptor.
     *
     * @see #add(MdibDescriptionModification.Type, AbstractDescriptor, AbstractState, String)
     */
    public MdibDescriptionModifications insert(AbstractDescriptor descriptor, @Nullable String parentHandle) {
        return add(MdibDescriptionModification.Type.INSERT, descriptor, parentHandle);
    }

    /**
     * Convenient function to insert a single state descriptor with state information.
     *
     * @see #add(MdibDescriptionModification.Type, AbstractDescriptor, AbstractState)
     */
    public MdibDescriptionModifications insert(AbstractDescriptor descriptor, AbstractState state) {
        return add(MdibDescriptionModification.Type.INSERT, descriptor, state);
    }

    /**
     * Convenient function to insert a single state descriptor with state information.
     *
     * @see #add(MdibDescriptionModification.Type, AbstractDescriptor, AbstractState)
     */
    public MdibDescriptionModifications insert(AbstractDescriptor descriptor,
                                               AbstractState state,
                                               @Nullable String parentHandle) {
        return add(MdibDescriptionModification.Type.INSERT, descriptor, state, parentHandle);
    }

    /**
     * Convenient function to insert a multi state descriptor with state information.
     *
     * @see #add(MdibDescriptionModification.Type, AbstractDescriptor, List)
     */
    public MdibDescriptionModifications insert(AbstractDescriptor descriptor,
                                               List<? extends AbstractMultiState> multiStates) {
        return add(MdibDescriptionModification.Type.INSERT, descriptor, multiStates);
    }

    /**
     * Convenient function to insert a multi state descriptor with state information.
     *
     * @see #add(MdibDescriptionModification.Type, AbstractDescriptor, List)
     */
    public MdibDescriptionModifications insert(AbstractDescriptor descriptor,
                                               List<? extends AbstractMultiState> multiStates,
                                               @Nullable String parentHandle) {
        return add(MdibDescriptionModification.Type.INSERT, descriptor, multiStates, parentHandle);
    }

    /**
     * Convenient function to update a descriptor.
     *
     * @see #add(MdibDescriptionModification.Type, AbstractDescriptor)
     */
    public MdibDescriptionModifications update(Entry entry) {
        return add(MdibDescriptionModification.Type.UPDATE, entry.getDescriptor(), entry.getState(), entry.getParentHandle());
    }

    /**
     * Convenient function to update a descriptor.
     *
     * @see #add(MdibDescriptionModification.Type, AbstractDescriptor)
     */
    public MdibDescriptionModifications update(MultiStateEntry entry) {
        return add(MdibDescriptionModification.Type.UPDATE, entry.getDescriptor(), entry.getStates(), entry.getParentHandle());
    }

    /**
     * Convenient function to update a descriptor.
     *
     * @see #add(MdibDescriptionModification.Type, AbstractDescriptor)
     */
    public MdibDescriptionModifications update(AbstractDescriptor descriptor) {
        return add(MdibDescriptionModification.Type.UPDATE, descriptor);
    }

    /**
     * Convenient function to update a single state descriptor with state information.
     *
     * @see #add(MdibDescriptionModification.Type, AbstractDescriptor, AbstractState)
     */
    public MdibDescriptionModifications update(AbstractDescriptor descriptor, AbstractState state) {
        return add(MdibDescriptionModification.Type.UPDATE, descriptor, state);
    }

    /**
     * Convenient function to update a multi state descriptor with state information.
     *
     * @see #add(MdibDescriptionModification.Type, AbstractDescriptor, List)
     */
    public MdibDescriptionModifications update(AbstractDescriptor descriptor,
                                               List<AbstractMultiState> multiStates) {
        return add(MdibDescriptionModification.Type.UPDATE, descriptor, multiStates);
    }

    /**
     * Convenient function to update a descriptor.
     *
     * @see #add(MdibDescriptionModification.Type, AbstractDescriptor)
     */
    public MdibDescriptionModifications delete(Entry entry) {
        return add(MdibDescriptionModification.Type.DELETE, entry.getDescriptor());
    }

    /**
     * Convenient function to delete a descriptor.
     *
     * @see #add(MdibDescriptionModification.Type, AbstractDescriptor)
     */
    public MdibDescriptionModifications delete(AbstractDescriptor descriptor) {
        return add(MdibDescriptionModification.Type.DELETE, descriptor);
    }

    /**
     * MDIB version that comes with this change set.
     */
    public Optional<MdibVersion> getMdibVersion() {
        return Optional.ofNullable(mdibVersion);
    }

    public boolean isAddedAsUpdated(String handle) {
        return updatedHandles.contains(handle);
    }

    public boolean isAddedAsInserted(String handle) {
        return insertedHandles.contains(handle);
    }

    public boolean isAddedAsDeleted(String handle) {
        return deletedHandles.contains(handle);
    }

    /**
     * Function to get changes.
     */
    public List<MdibDescriptionModification> getModifications() {
        return Collections.unmodifiableList(modifications);
    }

    public static class Entry {
        private AbstractDescriptor descriptor;
        private AbstractState state;
        private String parentHandle;

        public Entry(AbstractDescriptor descriptor, AbstractState state) {
            this(descriptor, state, null);
        }

        public Entry(AbstractDescriptor descriptor, AbstractState state, @Nullable String handle) {
            this.descriptor = descriptor;
            this.state = state;
            this.parentHandle = handle;
        }

        private AbstractDescriptor getDescriptor() {
            return descriptor;
        }

        private AbstractState getState() {
            return state;
        }

        private @Nullable
        String getParentHandle() {
            return parentHandle;
        }
    }

    public static class MultiStateEntry {
        private AbstractDescriptor descriptor;
        private List<AbstractMultiState> states;
        private String parentHandle;

        public MultiStateEntry(AbstractDescriptor descriptor, List<AbstractMultiState> states, String handle) {
            this.descriptor = descriptor;
            this.states = states;
            this.parentHandle = handle;
        }

        private AbstractDescriptor getDescriptor() {
            return descriptor;
        }

        private List<AbstractMultiState> getStates() {
            return states;
        }

        private @Nullable
        String getParentHandle() {
            return parentHandle;
        }
    }

    private MdibDescriptionModifications(@Nullable MdibVersion mdibVersion) {
        this.mdibVersion = mdibVersion;
        this.modifications = new ArrayList<>();
        this.insertedHandles = new HashSet<>();
        this.updatedHandles = new HashSet<>();
        this.deletedHandles = new HashSet<>();
    }

    private MdibDescriptionModifications addMdibModification(MdibDescriptionModification.Type modType,
                                                             AbstractDescriptor descriptor,
                                                             List<? extends AbstractState> states) {
        return addMdibModification(modType, descriptor, states, null);
    }

    private MdibDescriptionModifications addMdibModification(MdibDescriptionModification.Type modType,
                                                             AbstractDescriptor descriptor,
                                                             List<? extends AbstractState> states,
                                                             @Nullable String parentHandle) {
        duplicateDetection(modType, descriptor.getHandle());
        modifications.add(new MdibDescriptionModification(modType, descriptor, (List) states, parentHandle));
        return this;
    }

    private void duplicateDetection(MdibDescriptionModification.Type modType, String handle) {
        Set<String> handleSet = insertedHandles;
        switch (modType) {
            case UPDATE:
                handleSet = updatedHandles;
                break;
            case DELETE:
                handleSet = deletedHandles;
                break;
        }

        if (handleSet.contains(handle)) {
            // Make this a runtime exception as duplicated handles should not ensue by design
            throw new RuntimeException(new HandleDuplicatedException(
                    String.format("Handle %s has already been inserted into description change set.", handle)));
        }

        handleSet.add(handle);
    }
}
