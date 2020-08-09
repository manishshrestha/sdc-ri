package org.somda.sdc.biceps.common;

import com.google.inject.Inject;
import org.somda.sdc.biceps.model.participant.AbstractContextDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractContextState;
import org.somda.sdc.biceps.model.participant.AbstractDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractMultiState;
import org.somda.sdc.biceps.model.participant.AbstractState;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 * Utility class to validate MDIB instances.
 */
public class MdibTypeValidator {
    private static final String ABSTRACT_PREFIX = "Abstract";

    private static final String DESCRIPTOR_SUFFIX = "Descriptor";
    private static final int DESCRIPTOR_SUFFIX_LENGTH = DESCRIPTOR_SUFFIX.length();

    private static final String STATE_SUFFIX = "State";
    private static final int STATE_SUFFIX_LENGTH = STATE_SUFFIX.length();
    private final HashMap<String, Class<?>> classCache;

    @Inject
    MdibTypeValidator() {
        this.classCache = new HashMap<>();
    }

    /**
     * Gets the base name of a descriptor class.
     *
     * The base name is the name of the class without <em>Descriptor</em> suffix.
     *
     * @param descrClass the class where to resolve the base name from.
     * @return the base name.
     */
    public String resolveDescriptorBaseName(Class<? extends AbstractDescriptor> descrClass) {
        return descrClass.getSimpleName().substring(0, descrClass.getSimpleName().length() - DESCRIPTOR_SUFFIX_LENGTH);
    }

    /**
     * Gets the base name of a state class.
     *
     * The base name is the name of the class without <em>State</em> suffix.
     *
     * @param stateClass the class where to resolve the base name from.
     * @return the base name.
     */
    public String resolveStateBaseName(Class<? extends AbstractState> stateClass) {
        return stateClass.getSimpleName().substring(0, stateClass.getSimpleName().length() - STATE_SUFFIX_LENGTH);
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

        final String name1 = resolveDescriptorBaseName(descrClass);
        final String name2 = resolveStateBaseName(stateClass);
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
        boolean typesAndHandleRefsOk = states.stream().noneMatch(s ->
                !descriptor.getHandle().equals(s.getDescriptorHandle()) ||
                        !match(descriptor.getClass(), s.getClass()));
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
     * Checks if a descriptor is a multi-state descriptor (true) or not (false).
     *
     * @param descriptor the descriptor to test.
     * @param <T>        any descriptor class.
     * @return true if the descriptor is a multi-state descriptor, false otherwise.
     */
    public <T extends AbstractDescriptor> boolean isMultiStateDescriptor(T descriptor) {
        return descriptor instanceof AbstractContextDescriptor;
    }

    /**
     * Checks if a state is a multi-state (true) or not (false).
     *
     * @param state the state to test.
     * @param <T>   any state class.
     * @return true if the state is a multi-state, false otherwise.
     */
    public <T extends AbstractState> boolean isMultiState(T state) {
        return state instanceof AbstractMultiState;
    }

    /**
     * Checks if a state is a context state (true) or not (false).
     *
     * @param state the state to test.
     * @param <T>   any state class.
     * @return true if the state is a context state, false otherwise.
     */
    public <T extends AbstractState> boolean isContextState(T state) {
        return state instanceof AbstractContextState;
    }

    /**
     * Tries to cast to a multi-state.
     *
     * @param state the state to cast.
     * @param <T>   any state class.
     * @return The cast multi-state or {@linkplain Optional#empty()} if the state was not a multi-state.
     */
    public <T extends AbstractState> Optional<AbstractMultiState> toMultiState(T state) {
        if (isMultiState(state)) {
            return Optional.of((AbstractMultiState) state);
        }
        return Optional.empty();
    }

    /**
     * Tries to cast to a context state.
     *
     * @param state the state to cast.
     * @param <T>   any state class.
     * @return The cast multi-state or {@linkplain Optional#empty()} if the state was not a multi-state.
     */
    public <T extends AbstractState> Optional<AbstractContextState> toContextState(T state) {
        if (isContextState(state)) {
            return Optional.of((AbstractContextState) state);
        }
        return Optional.empty();
    }

    /**
     * Resolves the descriptor type belonging to a state type.
     * @param stateClass to resolve the descriptor type for
     * @param <T> a state type
     * @param <V> a descriptor type
     * @return the descriptor type matching the passed state
     * @throws ClassNotFoundException if no matching descriptor class has been found
     */
    public <T extends AbstractState, V extends AbstractDescriptor> Class<V> resolveDescriptorType(Class<T> stateClass)
            throws ClassNotFoundException {
        final String baseName = stateClass.getCanonicalName()
                .substring(0, stateClass.getCanonicalName().length() - STATE_SUFFIX_LENGTH);
        return (Class<V>) Class.forName(baseName + DESCRIPTOR_SUFFIX);
    }

    /**
     * Resolves the state type belonging to a descriptor type.
     * @param descriptorClass to resolve the state type for
     * @param <T> a descriptor type
     * @param <V> a state type
     * @return the state type matching the passed descriptor
     * @throws ClassNotFoundException if no matching state class has been found
     */
    public <T extends AbstractDescriptor, V extends AbstractState> Class<V> resolveStateType(Class<T> descriptorClass)
            throws ClassNotFoundException {
        final String baseName = descriptorClass.getCanonicalName()
                .substring(0, descriptorClass.getCanonicalName().length() - DESCRIPTOR_SUFFIX_LENGTH);
        var className = baseName + STATE_SUFFIX;

        var cachedClass = classCache.get(className);
        if (cachedClass != null) {
            return (Class<V>)cachedClass;
        }
        var clazz = Class.forName(className);
        classCache.put(className, clazz);
        return (Class<V>) clazz;
    }
}
