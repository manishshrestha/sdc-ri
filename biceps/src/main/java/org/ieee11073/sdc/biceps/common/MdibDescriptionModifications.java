package org.ieee11073.sdc.biceps.common;

import org.ieee11073.sdc.biceps.model.participant.AbstractContextDescriptor;
import org.ieee11073.sdc.biceps.model.participant.AbstractContextState;
import org.ieee11073.sdc.biceps.model.participant.AbstractDescriptor;
import org.ieee11073.sdc.biceps.model.participant.AbstractState;

import java.util.*;

/**
 * Container to collect changes supposed to be applied on an MDIB.
 *
 * The {@linkplain MdibDescriptionModifications} is a fluent interface.
 */
public class MdibDescriptionModifications {
    private List<MdibDescriptionModification> modifications;
    private Set<String> insertedHandles;
    private Set<String> updatedHandles;
    private Set<String> deletedHandles;

    /**
     * Create set.
     */
    static MdibDescriptionModifications create() {
        return new MdibDescriptionModifications();
    }

    /**
     * Add single or multi state descriptor to change set without state information.
     *
     * It's up to the change set processor to align state information.
     */
    public MdibDescriptionModifications addItem(MdibDescriptionModification.Type modType, AbstractDescriptor descriptor) {
        return addMdibModification(modType, descriptor, null);
    }

    /**
     * Add single state descriptor to change set with state information.
     *
     * Caveat: the change set processor might check descriptor state consistency.
     */
    public MdibDescriptionModifications addItem(MdibDescriptionModification.Type modType, AbstractDescriptor descriptor, AbstractState state) {
        return addMdibModification(modType, descriptor, Collections.singletonList(state));
    }

    /**
     * Add multi state descriptor to change set with state information.
     *
     * Caveat: the change set processor might check descriptor state consistency.
     */
    public MdibDescriptionModifications addItem(MdibDescriptionModification.Type modType,
                                                AbstractContextDescriptor context,
                                                List<? extends AbstractContextState> contextStates) {
        contextStates.stream().forEach(state -> duplicateDetection(modType, state.getHandle()));
        return addMdibModification(modType, context, contextStates);
    }

    /**
     * Convenient function to insert a descriptor.
     * @see #addItem(MdibDescriptionModification.Type, AbstractDescriptor)
     */
    public MdibDescriptionModifications insert(AbstractDescriptor descriptor) {
        return addItem(MdibDescriptionModification.Type.INSERT, descriptor);
    }

    /**
     * Convenient function to insert a single state descriptor with state information.
     * @see #addItem(MdibDescriptionModification.Type, AbstractDescriptor, AbstractState)
     */
    public MdibDescriptionModifications insert(AbstractDescriptor descriptor, AbstractState state) {
        return addItem(MdibDescriptionModification.Type.INSERT, descriptor, state);
    }

    /**
     * Convenient function to insert a multi state descriptor with state information.
     * @see #addItem(MdibDescriptionModification.Type, AbstractContextDescriptor, List)
     */
    public MdibDescriptionModifications insert(AbstractContextDescriptor context,
                                               List<? extends AbstractContextState> contextStates) {
        return addItem(MdibDescriptionModification.Type.INSERT, context, contextStates);
    }

    /**
     * Convenient function to update a descriptor.
     * @see #addItem(MdibDescriptionModification.Type, AbstractDescriptor)
     */
    public MdibDescriptionModifications update(AbstractDescriptor descriptor) {
        return addItem(MdibDescriptionModification.Type.UPDATE, descriptor);
    }

    /**
     * Convenient function to update a single state descriptor with state information.
     * @see #addItem(MdibDescriptionModification.Type, AbstractDescriptor, AbstractState)
     */
    public MdibDescriptionModifications update(AbstractDescriptor descriptor, AbstractState state) {
        return addItem(MdibDescriptionModification.Type.UPDATE, descriptor, state);
    }

    /**
     * Convenient function to update a multi state descriptor with state information.
     * @see #addItem(MdibDescriptionModification.Type, AbstractContextDescriptor, List)
     */
    public MdibDescriptionModifications update(AbstractContextDescriptor context,
                                               List<AbstractContextState> contextStates) {
        return addItem(MdibDescriptionModification.Type.UPDATE, context, contextStates);
    }

    /**
     * Convenient function to delete a descriptor.
     * @see #addItem(MdibDescriptionModification.Type, AbstractDescriptor)
     */
    public MdibDescriptionModifications delete(AbstractDescriptor descriptor) {
        return addItem(MdibDescriptionModification.Type.DELETE, descriptor);
    }

    /**
     * Function to get changes. Only visible to classes from the same package.
     */
    List<MdibDescriptionModification> getModifications() {
        return modifications;
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
        duplicateDetection(modType, descriptor.getHandle());
        modifications.add(new MdibDescriptionModification(modType, descriptor, states));
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
            throw new RuntimeException(
                    String.format("Handle %s has already been inserted into description change set.", handle));
        }

        handleSet.add(handle);
    }
}
