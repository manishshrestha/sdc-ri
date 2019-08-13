package org.ieee11073.sdc.biceps.common;

import com.google.inject.Inject;
import org.ieee11073.sdc.biceps.model.participant.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Utility class to validate MDIB instances.
 */
public class MdibTypeValidator {
    @Inject
    MdibTypeValidator() {
    }

    /**
     * Check if descriptor and state classes match.
     * <p>
     * A match is given if both classes do not implement the abstract flavor and share the same name prefix up to
     * the Descriptor and State suffix.
     *
     * @return true if classes match, otherwise false.
     */
    public boolean match(Class<? extends AbstractDescriptor> descrClass,
                         Class<? extends AbstractState> stateClass) {
        final String abstractPrefix = "Abstract";
        final int descriptorSuffixLength = "Descriptor".length();
        final int stateSuffixLength = "State".length();

        if (descrClass.getSimpleName().startsWith(abstractPrefix) ||
                stateClass.getSimpleName().startsWith(abstractPrefix)) {
            return false;
        }

        final String name1 = descrClass.getSimpleName().substring(0, descrClass.getSimpleName().length() - descriptorSuffixLength);
        final String name2 = stateClass.getSimpleName().substring(0, stateClass.getSimpleName().length() - stateSuffixLength);
        return name1.equals(name2);
    }

    /**
     * Checks if the given descriptor state pairing is valid.
     *
     * A match is given if
     * - descriptor and states match in terms of {@link #match(Class, Class)},
     * - {@link AbstractState#getDescriptorHandle()} to {@link AbstractDescriptor#getHandle()} equality, and
     * - single state descriptors do correspond to exactly one state.
     *
     * @return true if instances match, otherwise false.
     */
    public <D extends AbstractDescriptor, S extends AbstractState> boolean match(D descriptor, List<S> states) {
        boolean singleStateOk = isSingleStateDescriptor(descriptor) && states.size() == 1;
        boolean multiStateOk = isMultiStateDescriptor(descriptor);
        boolean typesAndHandleRefsOk = !states.parallelStream().filter(s ->
                !descriptor.getHandle().equals(s.getDescriptorHandle()) ||
                        !match(descriptor.getClass(), s.getClass())).findAny().isPresent();
        return typesAndHandleRefsOk && (singleStateOk || multiStateOk);
    }

    /**
     * Tries to match with exactly one state.
     *
     * Hint: does also work for multi state list of size 1.
     *
     * @see #match(AbstractDescriptor, List)
     */
    public <D extends AbstractDescriptor, S extends AbstractState> boolean match(D descriptor, S state) {
        return match(descriptor, Collections.singletonList(state));
    }

    /**
     * Checks if a descriptor is a single state descriptor (true) or not (false).
     */
    public <T extends AbstractDescriptor> boolean isSingleStateDescriptor(T descriptor) {
        return !isMultiStateDescriptor(descriptor);
    }

    /**
     * Checks if a descriptor is a single state (true) or not (false).
     */
    public <T extends AbstractState> boolean isSingleState(T state) {
        return !isMultiState(state);
    }

    /**
     * Checks if a descriptor is a multi state descriptor (true) or not (false).
     */
    public <T extends AbstractDescriptor> boolean isMultiStateDescriptor(T descriptor) {
        return descriptor instanceof AbstractContextDescriptor;
    }

    /**
     * Checks if a descriptor is a multi state (true) or not (false).
     */
    public <T extends AbstractState> boolean isMultiState(T state) {
        return state instanceof AbstractMultiState;
    }

    public <T extends AbstractState> Optional<AbstractMultiState> toMultiState(T state) {
        if (isMultiState(state)) {
            return Optional.of((AbstractMultiState)state);
        }
        return Optional.empty();
    }
}
