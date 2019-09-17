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
     * Checks if descriptor and state classes match.
     * <p>
     * A match is given if both classes do not implement the abstract flavor and share the same name prefix
     * excluding the Descriptor and State suffix.
     *
     * @param descrClass the descriptor class to match.
     * @param stateClass the state class to match.
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
     * <p>
     * A match is given if
     * <ul>
     * <li>descriptor and states match in terms of {@link #match(Class, Class)},
     * <li>{@link AbstractState#getDescriptorHandle()} equals {@link AbstractDescriptor#getHandle()}, and
     * <li>single state descriptors do correspond to exactly one state.
     * </ul>
     *
     * @param descriptor the descriptor to test.
     * @param states the list of states to test.
     * @param <D> any descriptor class.
     * @param <S> any state class.
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
     * <p>
     * Hint: does also work for multi state lists of size 1.
     *
     * @param descriptor the descriptor to test.
     * @param state the state to test.
     * @param <D> any descriptor class.
     * @param <S> any state class.
     * @see #match(AbstractDescriptor, List)
     */
    public <D extends AbstractDescriptor, S extends AbstractState> boolean match(D descriptor, S state) {
        return match(descriptor, Collections.singletonList(state));
    }

    /**
     * Checks if a descriptor is a single state descriptor (true) or not (false).
     *
     * @param descriptor the descriptor to test.
     * @param <T> any descriptor class.
     * @return true if the descriptor is a single state descriptor, false otherwise.
     */
    public <T extends AbstractDescriptor> boolean isSingleStateDescriptor(T descriptor) {
        return !isMultiStateDescriptor(descriptor);
    }

    /**
     * Checks if a state is a single state (true) or not (false).
     *
     * @param state the state to test.
     * @param <T> any state class.
     * @return true if the state is a single state, false otherwise.
     */
    public <T extends AbstractState> boolean isSingleState(T state) {
        return !isMultiState(state);
    }

    /**
     * Checks if a descriptor is a multi state descriptor (true) or not (false).
     *
     * @param descriptor the descriptor to test.
     * @param <T> any descriptor class.
     * @return true if the descriptor is a multi state descriptor, false otherwise.
     */
    public <T extends AbstractDescriptor> boolean isMultiStateDescriptor(T descriptor) {
        return descriptor instanceof AbstractContextDescriptor;
    }

    /**
     * Checks if a state is a multi state (true) or not (false).
     *
     * @param state the state to test.
     * @param <T> any state class.
     * @return true if the state is a multi state, false otherwise.
     */
    public <T extends AbstractState> boolean isMultiState(T state) {
        return state instanceof AbstractMultiState;
    }

    /**
     * Tries to cast to a multi state.
     *
     * @param state the state to cast.
     * @param <T> any state class.
     * @return The cast multi state or {@linkplain Optional#empty()} if the state was not a multi state.
     */
    public <T extends AbstractState> Optional<AbstractMultiState> toMultiState(T state) {
        if (isMultiState(state)) {
            return Optional.of((AbstractMultiState)state);
        }
        return Optional.empty();
    }
}
