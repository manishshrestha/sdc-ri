package org.somda.sdc.biceps.common;

import org.somda.sdc.biceps.provider.preprocessing.HandleDuplicatedException;
import org.somda.sdc.biceps.model.participant.AbstractDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractMultiState;
import org.somda.sdc.biceps.model.participant.AbstractState;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Container to collect changes supposed to be applied on an MDIB.
 * <p>
 * The {@linkplain MdibDescriptionModifications} is a fluent interface.
 */
public class MdibDescriptionModifications {
    private List<MdibDescriptionModification> modifications;
    private Set<String> insertedHandles;
    private Set<String> updatedHandles;
    private Set<String> deletedHandles;

    /**
     * Creates a set.
     *
     * @return a new {@link MdibDescriptionModifications} instance.
     */
    public static MdibDescriptionModifications create() {
        return new MdibDescriptionModifications();
    }

    /**
     * Adds a change.
     *
     * @param modType the modification type.
     * @param entry   an entry that contains all necessary modification information.
     * @return this object for fluent access.
     */
    public MdibDescriptionModifications add(MdibDescriptionModification.Type modType,
                                            Entry entry) {
        return add(modType, entry.getDescriptor(), entry.getState(), entry.getParentHandle());
    }

    /**
     * Adds a change.
     *
     * @param modType the modification type.
     * @param entry   a multi-state entry that contains all necessary modification information.
     * @return this object for fluent access.
     */
    public MdibDescriptionModifications add(MdibDescriptionModification.Type modType,
                                            MultiStateEntry entry) {
        return add(modType, entry.getDescriptor(), entry.getStates(), entry.getParentHandle());
    }

    /**
     * Adds single or multi state descriptor to change set without state information.
     * <p>
     * It's up to the change set processor to align state information.
     *
     * @param modType      the modification type.
     * @param descriptor   the descriptor to add.
     * @param parentHandle the parent handle, allowed to be null in case of MDS or if it can be derived during preprocessing.
     * @return this object for fluent access.
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
     *
     * @param modType    the modification type.
     * @param descriptor the descriptor to add.
     * @return this object for fluent access.
     */
    public MdibDescriptionModifications add(MdibDescriptionModification.Type modType,
                                            AbstractDescriptor descriptor) {
        return addMdibModification(modType, descriptor, null, null);
    }

    /**
     * Add single state descriptor to change set with state information.
     * <p>
     * Caveat: the change set processor might check descriptor state consistency.
     *
     * @param modType    the modification type.
     * @param descriptor the descriptor to add.
     * @param state      the state to add.
     * @return this object for fluent access.
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
     *
     * @param modType      the modification type.
     * @param descriptor   the descriptor to add.
     * @param state        the state to add.
     * @param parentHandle the parent handle for this modification.
     * @return this object for fluent access.
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
     *
     * @param modType     the modification type.
     * @param descriptor  the descriptor to add.
     * @param multiStates the states to add.
     * @return this object for fluent access.
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
     *
     * @param modType      the modification type.
     * @param descriptor   the descriptor to add.
     * @param multiStates  the states to add.
     * @param parentHandle the parent handle for this modification.
     * @return this object for fluent access.
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
     * @param entry bundled information.
     * @return this object for fluent access.
     */
    public MdibDescriptionModifications insert(Entry entry) {
        return add(MdibDescriptionModification.Type.INSERT, entry.getDescriptor(), entry.getState(), entry.getParentHandle());
    }

    /**
     * Convenient function to insert a descriptor.
     *
     * @param entry bundled information.
     * @return this object for fluent access.
     */
    public MdibDescriptionModifications insert(MultiStateEntry entry) {
        return add(MdibDescriptionModification.Type.INSERT, entry.getDescriptor(), entry.getStates(), entry.getParentHandle());
    }

    /**
     * Convenient function to insert a descriptor.
     *
     * @param descriptor the descriptor to insert.
     * @return this object for fluent access.
     */
    public MdibDescriptionModifications insert(AbstractDescriptor descriptor) {
        return add(MdibDescriptionModification.Type.INSERT, descriptor);
    }

    /**
     * Convenient function to insert a descriptor.
     *
     * @param descriptor   the descriptor to insert.
     * @param parentHandle the parent handle for this modification.
     * @return this object for fluent access.
     */
    public MdibDescriptionModifications insert(AbstractDescriptor descriptor, @Nullable String parentHandle) {
        return add(MdibDescriptionModification.Type.INSERT, descriptor, parentHandle);
    }

    /**
     * Convenient function to insert a single state descriptor with state information.
     *
     * @param descriptor the descriptor to insert.
     * @param state      the state to insert.
     * @return this object for fluent access.
     */
    public MdibDescriptionModifications insert(AbstractDescriptor descriptor, AbstractState state) {
        return add(MdibDescriptionModification.Type.INSERT, descriptor, state);
    }

    /**
     * Convenient function to insert a single state descriptor with state information.
     *
     * @param descriptor   the descriptor to insert.
     * @param state        the state to insert.
     * @param parentHandle the parent handle for this modification.
     * @return this object for fluent access.
     */
    public MdibDescriptionModifications insert(AbstractDescriptor descriptor,
                                               AbstractState state,
                                               @Nullable String parentHandle) {
        return add(MdibDescriptionModification.Type.INSERT, descriptor, state, parentHandle);
    }

    /**
     * Convenient function to insert a multi state descriptor with state information.
     *
     * @param descriptor  the descriptor to insert.
     * @param multiStates the states to insert.
     * @return this object for fluent access.
     */
    public MdibDescriptionModifications insert(AbstractDescriptor descriptor,
                                               List<? extends AbstractMultiState> multiStates) {
        return add(MdibDescriptionModification.Type.INSERT, descriptor, multiStates);
    }

    /**
     * Convenient function to insert a multi state descriptor with state information.
     *
     * @param descriptor   the descriptor to insert.
     * @param multiStates  the states to insert.
     * @param parentHandle the parent handle for this modification.
     * @return this object for fluent access.
     */
    public MdibDescriptionModifications insert(AbstractDescriptor descriptor,
                                               List<? extends AbstractMultiState> multiStates,
                                               @Nullable String parentHandle) {
        return add(MdibDescriptionModification.Type.INSERT, descriptor, multiStates, parentHandle);
    }

    /**
     * Convenient function to update a descriptor.
     *
     * @param entry bundled information.
     * @return this object for fluent access.
     */
    public MdibDescriptionModifications update(Entry entry) {
        return add(MdibDescriptionModification.Type.UPDATE, entry.getDescriptor(), entry.getState(), entry.getParentHandle());
    }

    /**
     * Convenient function to update a descriptor.
     *
     * @param entry bundled information.
     * @return this object for fluent access.
     */
    public MdibDescriptionModifications update(MultiStateEntry entry) {
        return add(MdibDescriptionModification.Type.UPDATE, entry.getDescriptor(), entry.getStates(), entry.getParentHandle());
    }

    /**
     * Convenient function to update a descriptor.
     *
     * @param descriptor the descriptor to update.
     * @return this object for fluent access.
     */
    public MdibDescriptionModifications update(AbstractDescriptor descriptor) {
        return add(MdibDescriptionModification.Type.UPDATE, descriptor);
    }

    /**
     * Convenient function to update a single state descriptor with state information.
     *
     * @param descriptor the descriptor to update.
     * @param state      the state to update.
     * @return this object for fluent access.
     */
    public MdibDescriptionModifications update(AbstractDescriptor descriptor, AbstractState state) {
        return add(MdibDescriptionModification.Type.UPDATE, descriptor, state);
    }

    /**
     * Convenient function to update a multi state descriptor with state information.
     *
     * @param descriptor  the descriptor to update.
     * @param multiStates the states to update.
     * @return this object for fluent access.
     */
    public MdibDescriptionModifications update(AbstractDescriptor descriptor,
                                               List<AbstractMultiState> multiStates) {
        return add(MdibDescriptionModification.Type.UPDATE, descriptor, multiStates);
    }

    /**
     * Convenient function to update a descriptor.
     *
     * @param entry bundled information.
     * @return this object for fluent access.
     */
    public MdibDescriptionModifications delete(Entry entry) {
        return add(MdibDescriptionModification.Type.DELETE, entry.getDescriptor());
    }

    /**
     * Convenient function to delete a descriptor.
     *
     * @param descriptor the descriptor to delete. It is sufficient to set a handle.
     * @return this object for fluent access.
     */
    public MdibDescriptionModifications delete(AbstractDescriptor descriptor) {
        return add(MdibDescriptionModification.Type.DELETE, descriptor);
    }

    /**
     * Convenient function to delete a descriptor.
     *
     * @param handle the handle that is used for deletion.
     * @return this object for fluent access.
     */
    public MdibDescriptionModifications delete(String handle) {
        final AbstractDescriptor descr = new AbstractDescriptor();
        descr.setHandle(handle);
        return add(MdibDescriptionModification.Type.DELETE, descr);
    }

    /**
     * Checks if a handle was added as inserted.
     *
     * @param handle the handle to seek.
     * @return true if added as inserted, false otherwise.
     */
    public boolean isAddedAsInserted(String handle) {
        return insertedHandles.contains(handle);
    }

    /**
     * Checks if a handle was added as updated.
     *
     * @param handle the handle to seek.
     * @return true if added as updated, false otherwise.
     */
    public boolean isAddedAsUpdated(String handle) {
        return updatedHandles.contains(handle);
    }

    /**
     * Checks if a handle was added as deleted.
     *
     * @param handle the handle to seek.
     * @return true if added as deleted, false otherwise.
     */
    public boolean isAddedAsDeleted(String handle) {
        return deletedHandles.contains(handle);
    }

    /**
     * Function to get all changes as unmodifiable list.
     *
     * @return all modifications collected so far.
     */
    public List<MdibDescriptionModification> getModifications() {
        return Collections.unmodifiableList(modifications);
    }

    /**
     * Empties all modifications.
     */
    public void clear() {
        modifications.clear();
        insertedHandles.clear();
        updatedHandles.clear();
        deletedHandles.clear();
    }

    /**
     * An entry that allows to bundle all single-state modification information in a single element.
     */
    public static class Entry {
        private AbstractDescriptor descriptor;
        private AbstractState state;
        private String parentHandle;

        /**
         * Constructor that accepts a descriptor and state (no parent).
         *
         * @param descriptor the descriptor.
         * @param state      the state.
         * @see #Entry(AbstractDescriptor, AbstractState, String)
         * @see MultiStateEntry
         */
        public Entry(AbstractDescriptor descriptor, AbstractState state) {
            this(descriptor, state, null);
        }

        /**
         * Constructor that accepts a descriptor, state and parent handle.
         *
         * @param descriptor   the descriptor.
         * @param state        the state.
         * @param parentHandle the parent handle.
         * @see #Entry(AbstractDescriptor, AbstractState)
         * @see MultiStateEntry
         */
        public Entry(AbstractDescriptor descriptor, AbstractState state, @Nullable String parentHandle) {
            this.descriptor = descriptor;
            this.state = state;
            this.parentHandle = parentHandle;
        }

        private AbstractDescriptor getDescriptor() {
            return descriptor;
        }

        private AbstractState getState() {
            return state;
        }

        private String getParentHandle() {
            return parentHandle;
        }
    }

    /**
     * An entry that allows to bundle all multi-state modification information in a single element.
     */
    public static class MultiStateEntry {
        private AbstractDescriptor descriptor;
        private List<AbstractMultiState> states;
        private String parentHandle;

        /**
         * Constructor that accepts a descriptor, states and parent handle.
         *
         * @param descriptor   the descriptor.
         * @param states       the states.
         * @param parentHandle the affected parent handle for this entry.
         * @see Entry
         */
        public MultiStateEntry(AbstractDescriptor descriptor, List<AbstractMultiState> states, String parentHandle) {
            this.descriptor = descriptor;
            this.states = states;
            this.parentHandle = parentHandle;
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

    private MdibDescriptionModifications() {
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
