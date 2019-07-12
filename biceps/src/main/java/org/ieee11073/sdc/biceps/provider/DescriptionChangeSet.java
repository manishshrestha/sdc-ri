package org.ieee11073.sdc.biceps.provider;

import org.ieee11073.sdc.biceps.model.participant.AbstractContextDescriptor;
import org.ieee11073.sdc.biceps.model.participant.AbstractContextState;
import org.ieee11073.sdc.biceps.model.participant.AbstractDescriptor;
import org.ieee11073.sdc.biceps.model.participant.AbstractState;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Container to collect changes supposed to be applied on an MDIB.
 *
 * The {@linkplain DescriptionChangeSet} is a fluent interface.
 */
public class DescriptionChangeSet {
    private List<ChangeItem> changeItems;
    private Set<String> insertedHandles;
    private Set<String> updatedHandles;
    private Set<String> deletedHandles;

    /**
     * Use to create a container instance.
     */
    static DescriptionChangeSet create() {
        return new DescriptionChangeSet();
    }

    /**
     * Add single or multi state descriptor to change set without state information.
     *
     * It's up to the change set processor to align state information.
     */
    public DescriptionChangeSet addItem(ModificationType modType, AbstractDescriptor descriptor) {
        return addChangeItem(modType, descriptor, null);
    }

    /**
     * Add single state descriptor to change set with state information.
     *
     * Caveat: the change set processor might check descriptor state consistency.
     */
    public DescriptionChangeSet addItem(ModificationType modType, AbstractDescriptor descriptor, AbstractState state) {
        return addChangeItem(modType, descriptor, Collections.singletonList(state));
    }

    /**
     * Add multi state descriptor to change set with state information.
     *
     * Caveat: the change set processor might check descriptor state consistency.
     */
    public DescriptionChangeSet addItem(ModificationType modType,
                                        AbstractContextDescriptor context,
                                        List<? extends AbstractContextState> contextStates) {
        contextStates.stream().forEach(state -> duplicateDetection(modType, state.getHandle()));
        return addChangeItem(modType, context, contextStates);
    }

    /**
     * Convenient function to insert a descriptor.
     * @see #addItem(ModificationType, AbstractDescriptor)
     */
    public DescriptionChangeSet insert(AbstractDescriptor descriptor) {
        return addItem(ModificationType.INSERT, descriptor);
    }

    /**
     * Convenient function to insert a single state descriptor with state information.
     * @see #addItem(ModificationType, AbstractDescriptor, AbstractState)
     */
    public DescriptionChangeSet insert(AbstractDescriptor descriptor, AbstractState state) {
        return addItem(ModificationType.INSERT, descriptor, state);
    }

    /**
     * Convenient function to insert a multi state descriptor with state information.
     * @see #addItem(ModificationType, AbstractContextDescriptor, List)
     */
    public DescriptionChangeSet insert(AbstractContextDescriptor context,
                                       List<? extends AbstractContextState> contextStates) {
        return addItem(ModificationType.INSERT, context, contextStates);
    }

    /**
     * Convenient function to update a descriptor.
     * @see #addItem(ModificationType, AbstractDescriptor)
     */
    public DescriptionChangeSet update(AbstractDescriptor descriptor) {
        return addItem(ModificationType.UPDATE, descriptor);
    }

    /**
     * Convenient function to update a single state descriptor with state information.
     * @see #addItem(ModificationType, AbstractDescriptor, AbstractState)
     */
    public DescriptionChangeSet update(AbstractDescriptor descriptor, AbstractState state) {
        return addItem(ModificationType.UPDATE, descriptor, state);
    }

    /**
     * Convenient function to update a multi state descriptor with state information.
     * @see #addItem(ModificationType, AbstractContextDescriptor, List)
     */
    public DescriptionChangeSet update(AbstractContextDescriptor context,
                                       List<AbstractContextState> contextStates) {
        return addItem(ModificationType.UPDATE, context, contextStates);
    }

    /**
     * Convenient function to delete a descriptor.
     * @see #addItem(ModificationType, AbstractDescriptor)
     */
    public DescriptionChangeSet delete(AbstractDescriptor descriptor) {
        return addItem(ModificationType.DELETE, descriptor);
    }

    /**
     * Designates the MDIB modification type, i.e., insert, update, or delete.
     */
    public enum ModificationType {
        INSERT,
        UPDATE,
        DELETE
    }

    /**
     * Function to get changes. Only visible to classes from the same package.
     */
    List<ChangeItem> getChanges() {
        return changeItems;
    }

    /**
     * Single change item retrievable via {@link #getChanges()}. Only visible to classes from the same package.
     */
    class ChangeItem {
        final private ModificationType modificationType;
        final private AbstractDescriptor descriptor;
        final private List<? extends AbstractState> states;

        public ChangeItem(ModificationType modificationType,
                          AbstractDescriptor descriptor,
                          @Nullable List<? extends AbstractState> states) {
            this.modificationType = modificationType;
            this.descriptor = descriptor;
            this.states = states == null ? Collections.emptyList() : states;
        }

        public ModificationType getModificationType() {
            return modificationType;
        }

        public AbstractDescriptor getDescriptor() {
            return descriptor;
        }

        public List<? extends AbstractState> getStates() {
            return states;
        }

    }

    private DescriptionChangeSet() {
        this.changeItems = new ArrayList<>();
        this.insertedHandles = new HashSet<>();
        this.updatedHandles = new HashSet<>();
        this.deletedHandles = new HashSet<>();
    }

    private DescriptionChangeSet addChangeItem(ModificationType modType,
                                               AbstractDescriptor descriptor,
                                               List<? extends AbstractState> states) {
        duplicateDetection(modType, descriptor.getHandle());
        changeItems.add(new ChangeItem(modType, descriptor, states));
        return this;
    }

    private void duplicateDetection(ModificationType modificationType, String handle) {
        Set<String> handleSet = insertedHandles;
        switch (modificationType) {
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
