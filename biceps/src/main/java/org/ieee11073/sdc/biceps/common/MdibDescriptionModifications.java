package org.ieee11073.sdc.biceps.common;

import org.ieee11073.sdc.biceps.model.participant.*;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Container to collect changes supposed to be applied on an MDIB.
 *
 * The {@linkplain MdibDescriptionModifications} is a fluent interface.
 */
public class MdibDescriptionModifications {
    private final MdibVersion mdibVersion;
    private List<MdibDescriptionModification> modifications;
    private Set<String> insertedHandles;
    private Set<String> updatedHandles;
    private Set<String> deletedHandles;

    /**
     * Create set.
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


    /**
     * Add single or multi state descriptor to change set without state information.
     *
     * It's up to the change set processor to align state information.
     */
    public MdibDescriptionModifications add(MdibDescriptionModification.Type modType,
                                            AbstractDescriptor descriptor,
                                            @Nullable String parentHandle) {
        return addMdibModification(modType, descriptor, null, parentHandle);
    }

    /**
     * Add single or multi state descriptor to change set without state information.
     *
     * It's up to the change set processor to align state information.
     */
    public MdibDescriptionModifications add(MdibDescriptionModification.Type modType,
                                            AbstractDescriptor descriptor) {
        return addMdibModification(modType, descriptor, null, null);
    }

    /**
     * Add single state descriptor to change set with state information.
     *
     * Caveat: the change set processor might check descriptor state consistency.
     */
    public MdibDescriptionModifications add(MdibDescriptionModification.Type modType,
                                            AbstractDescriptor descriptor,
                                            AbstractState state) {
        return addMdibModification(modType, descriptor, Collections.singletonList(state));
    }

    /**
     * Add single state descriptor to change set with state information.
     *
     * Caveat: the change set processor might check descriptor state consistency.
     */
    public MdibDescriptionModifications add(MdibDescriptionModification.Type modType,
                                            AbstractDescriptor descriptor,
                                            AbstractState state,
                                            @Nullable String parentHandle) {
        return addMdibModification(modType, descriptor, Collections.singletonList(state), parentHandle);
    }

    /**
     * Add context state descriptor to change set with state information.
     *
     * Caveat: the change set processor might check descriptor state consistency.
     */
    public MdibDescriptionModifications add(MdibDescriptionModification.Type modType,
                                            AbstractContextDescriptor context,
                                            List<? extends AbstractContextState> contextStates) {
        contextStates.stream().forEach(state -> duplicateDetection(modType, state.getHandle()));
        return addMdibModification(modType, context, contextStates);
    }

    /**
     * Add multi state descriptor to change set with state information.
     *
     * Caveat: the change set processor might check descriptor state consistency.
     */
    public MdibDescriptionModifications add(MdibDescriptionModification.Type modType,
                                            AbstractDescriptor descriptor,
                                            List<? extends AbstractMultiState> multiStates) {
        multiStates.stream().forEach(state -> duplicateDetection(modType, state.getHandle()));
        return addMdibModification(modType, descriptor, multiStates);
    }

    /**
     * Add context state descriptor to change set with state information.
     *
     * Caveat: the change set processor might check descriptor state consistency.
     */
    public MdibDescriptionModifications add(MdibDescriptionModification.Type modType,
                                            AbstractContextDescriptor context,
                                            List<? extends AbstractContextState> contextStates,
                                            @Nullable String parentHandle) {
        contextStates.stream().forEach(state -> duplicateDetection(modType, state.getHandle()));
        return addMdibModification(modType, context, contextStates, parentHandle);
    }

    /**
     * Add multi state descriptor to change set with state information.
     *
     * Caveat: the change set processor might check descriptor state consistency.
     */
    public MdibDescriptionModifications add(MdibDescriptionModification.Type modType,
                                            AbstractDescriptor descriptor,
                                            List<? extends AbstractMultiState> multiStates,
                                            @Nullable String parentHandle) {
        multiStates.stream().forEach(state -> duplicateDetection(modType, state.getHandle()));
        return addMdibModification(modType, descriptor, multiStates, parentHandle);
    }


    /**
     * Convenient function to insert a descriptor.
     * @see #add(MdibDescriptionModification.Type, AbstractDescriptor)
     */
    public MdibDescriptionModifications insert(AbstractDescriptor descriptor) {
        return add(MdibDescriptionModification.Type.INSERT, descriptor);
    }

    /**
     * Convenient function to insert a descriptor.
     * @see #add(MdibDescriptionModification.Type, AbstractDescriptor, AbstractState, String)
     */
    public MdibDescriptionModifications insert(AbstractDescriptor descriptor, @Nullable String parentHandle) {
        return add(MdibDescriptionModification.Type.INSERT, descriptor, parentHandle);
    }

    /**
     * Convenient function to insert a single state descriptor with state information.
     * @see #add(MdibDescriptionModification.Type, AbstractDescriptor, AbstractState)
     */
    public MdibDescriptionModifications insert(AbstractDescriptor descriptor, AbstractState state) {
        return add(MdibDescriptionModification.Type.INSERT, descriptor, state);
    }

    /**
     * Convenient function to insert a single state descriptor with state information.
     * @see #add(MdibDescriptionModification.Type, AbstractDescriptor, AbstractState)
     */
    public MdibDescriptionModifications insert(AbstractDescriptor descriptor,
                                               AbstractState state,
                                               @Nullable String parentHandle) {
        return add(MdibDescriptionModification.Type.INSERT, descriptor, state, parentHandle);
    }

    /**
     * Convenient function to insert a multi state descriptor with state information.
     * @see #add(MdibDescriptionModification.Type, AbstractContextDescriptor, List)
     */
    public MdibDescriptionModifications insert(AbstractContextDescriptor context,
                                               List<? extends AbstractContextState> contextStates) {
        return add(MdibDescriptionModification.Type.INSERT, context, contextStates);
    }

    /**
     * Convenient function to insert a multi state descriptor with state information.
     * @see #add(MdibDescriptionModification.Type, AbstractContextDescriptor, List)
     */
    public MdibDescriptionModifications insert(AbstractDescriptor descriptor,
                                               List<? extends AbstractMultiState> multiStates) {
        return add(MdibDescriptionModification.Type.INSERT, descriptor, multiStates);
    }

    /**
     * Convenient function to insert a multi state descriptor with state information.
     * @see #add(MdibDescriptionModification.Type, AbstractContextDescriptor, List)
     */
    public MdibDescriptionModifications insert(AbstractContextDescriptor context,
                                               List<? extends AbstractContextState> contextStates,
                                               @Nullable String parentHandle) {
        return add(MdibDescriptionModification.Type.INSERT, context, contextStates, parentHandle);
    }

    /**
     * Convenient function to insert a multi state descriptor with state information.
     * @see #add(MdibDescriptionModification.Type, AbstractContextDescriptor, List)
     */
    public MdibDescriptionModifications insert(AbstractDescriptor descriptor,
                                               List<? extends AbstractMultiState> multiStates,
                                               @Nullable String parentHandle) {
        return add(MdibDescriptionModification.Type.INSERT, descriptor, multiStates, parentHandle);
    }

    /**
     * Convenient function to update a descriptor.
     * @see #add(MdibDescriptionModification.Type, AbstractDescriptor)
     */
    public MdibDescriptionModifications update(AbstractDescriptor descriptor) {
        return add(MdibDescriptionModification.Type.UPDATE, descriptor);
    }

    /**
     * Convenient function to update a single state descriptor with state information.
     * @see #add(MdibDescriptionModification.Type, AbstractDescriptor, AbstractState)
     */
    public MdibDescriptionModifications update(AbstractDescriptor descriptor, AbstractState state) {
        return add(MdibDescriptionModification.Type.UPDATE, descriptor, state);
    }

    /**
     * Convenient function to update a multi state descriptor with state information.
     * @see #add(MdibDescriptionModification.Type, AbstractContextDescriptor, List)
     */
    public MdibDescriptionModifications update(AbstractContextDescriptor context,
                                               List<AbstractContextState> contextStates) {
        return add(MdibDescriptionModification.Type.UPDATE, context, contextStates);
    }

    /**
     * Convenient function to update a multi state descriptor with state information.
     * @see #add(MdibDescriptionModification.Type, AbstractContextDescriptor, List)
     */
    public MdibDescriptionModifications update(AbstractDescriptor descriptor,
                                               List<AbstractMultiState> multiStates) {
        return add(MdibDescriptionModification.Type.UPDATE, descriptor, multiStates);
    }

    /**
     * Convenient function to delete a descriptor.
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
     * Function to get changes. Only visible to classes from the same package.
     */
    List<MdibDescriptionModification> getModifications() {
        return modifications;
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
        modifications.add(new MdibDescriptionModification(modType, descriptor, states, parentHandle));
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
            throw new DuplicateHandleException(
                    String.format("Handle %s has already been inserted into description change set.", handle));
        }

        handleSet.add(handle);
    }
}
