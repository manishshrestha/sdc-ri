package org.ieee11073.sdc.biceps.common;

import com.google.inject.Inject;
import org.ieee11073.sdc.biceps.model.participant.AbstractContextDescriptor;
import org.ieee11073.sdc.biceps.model.participant.AbstractDescriptor;
import org.ieee11073.sdc.biceps.model.participant.AbstractMultiState;
import org.ieee11073.sdc.biceps.model.participant.AbstractState;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Utility class to validate MDIB instances.
 */
public class MdibTypeValidator {
    private static final String ABSTRACT_PREFIX = "Abstract";

    private static String DESCRIPTOR_SUFFIX = "Descriptor";
    private static int DESCRIPTOR_SUFFIX_LENGTH = DESCRIPTOR_SUFFIX.length();

    private static final String STATE_SUFFIX = "State";
    private static final int STATE_SUFFIX_LENGTH = STATE_SUFFIX.length();

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

        if (descrClass.getSimpleName().startsWith(ABSTRACT_PREFIX) ||
                stateClass.getSimpleName().startsWith(ABSTRACT_PREFIX)) {
            return false;
        }

        final String name1 = descrClass.getSimpleName().substring(0, descrClass.getSimpleName().length() - DESCRIPTOR_SUFFIX_LENGTH);
        final String name2 = stateClass.getSimpleName().substring(0, stateClass.getSimpleName().length() - STATE_SUFFIX_LENGTH);
        return name1.equals(name2);
    }

    /**
     * Checks if the given descriptor states pairing is valid.
     * <p>
     * A match is given if
     * <ul>
     * <li>descriptor and states match in terms of {@link #match(Class, Class)},
     * <li>{@link AbstractState#getDescriptorHandle()} equals {@link AbstractDescriptor#getHandle()}, and
     * <li>single state descriptors do correspond to exactly one state.
     * </ul>
     *
     * @param descriptor the descriptor to test.
     * @param states     the list of states to test.
     * @param <D>        any descriptor class.
     * @param <S>        any state class.
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
     * Tries to match a descriptor with exactly one state.
     * <p>
     * Hint: does also work for multi-state lists of size 1.
     *
     * @param descriptor the descriptor to test.
     * @param state      the state to test.
     * @param <D>        any descriptor class.
     * @param <S>        any state class.
     * @return true if descriptor and state match, otherwise false.
     * @see #match(AbstractDescriptor, List)
     */
    public <D extends AbstractDescriptor, S extends AbstractState> boolean match(D descriptor, S state) {
        return match(descriptor, Collections.singletonList(state));
    }

    /**
     * Checks if a descriptor is a single state descriptor (true) or not (false).
     *
     * @param descriptor the descriptor to test.
     * @param <T>        any descriptor class.
     * @return true if the descriptor is a single state descriptor, false otherwise.
     */
    public <T extends AbstractDescriptor> boolean isSingleStateDescriptor(T descriptor) {
        return !isMultiStateDescriptor(descriptor);
    }

    /**
     * Checks if a state is a single state (true) or not (false).
     *
     * @param state the state to test.
     * @param <T>   any state class.
     * @return true if the state is a single state, false otherwise.
     */
    public <T extends AbstractState> boolean isSingleState(T state) {
        return !isMultiState(state);
    }

    /**
     * Checks if a descriptor is a multi state descriptor (true) or not (false).
     *
     * @param descriptor the descriptor to test.
     * @param <T>        any descriptor class.
     * @return true if the descriptor is a multi state descriptor, false otherwise.
     */
    public <T extends AbstractDescriptor> boolean isMultiStateDescriptor(T descriptor) {
        return descriptor instanceof AbstractContextDescriptor;
    }

    /**
     * Checks if a state is a multi state (true) or not (false).
     *
     * @param state the state to test.
     * @param <T>   any state class.
     * @return true if the state is a multi state, false otherwise.
     */
    public <T extends AbstractState> boolean isMultiState(T state) {
        return state instanceof AbstractMultiState;
    }

    /**
     * Tries to cast to a multi state.
     *
     * @param state the state to cast.
     * @param <T>   any state class.
     * @return The cast multi state or {@linkplain Optional#empty()} if the state was not a multi state.
     */
    public <T extends AbstractState> Optional<AbstractMultiState> toMultiState(T state) {
        if (isMultiState(state)) {
            return Optional.of((AbstractMultiState) state);
        }
        return Optional.empty();
    }

    public <T extends AbstractState, V extends AbstractDescriptor> Class<V> resolveDescriptorType(Class<T> stateClass) throws ClassNotFoundException {
        final String baseName = stateClass.getCanonicalName()
                .substring(0, stateClass.getCanonicalName().length() - STATE_SUFFIX_LENGTH);
        return (Class<V>)Class.forName(baseName + DESCRIPTOR_SUFFIX);
    }

    public <T extends AbstractDescriptor, V extends AbstractState> Class<V> resolveStateType(Class<T> descriptorClass) throws ClassNotFoundException {
        final String baseName = descriptorClass.getCanonicalName()
                .substring(0, descriptorClass.getCanonicalName().length() - DESCRIPTOR_SUFFIX_LENGTH);
        return (Class<V>)Class.forName(baseName + STATE_SUFFIX);
    }
}
