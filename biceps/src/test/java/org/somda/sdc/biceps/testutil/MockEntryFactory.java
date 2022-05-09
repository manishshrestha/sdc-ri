package org.somda.sdc.biceps.testutil;

import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.common.MdibTypeValidator;
import org.somda.sdc.biceps.model.participant.AbstractContextState;
import org.somda.sdc.biceps.model.participant.AbstractDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractState;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MockEntryFactory {
    private final MdibTypeValidator typeValidator;

    public MockEntryFactory(MdibTypeValidator typeValidator) {
        this.typeValidator = typeValidator;
    }

    public <T extends AbstractDescriptor.Builder<?>, V extends AbstractState.Builder<?>> MdibDescriptionModifications.Entry entry(String handle,
                                                                                                            T descriptorBuilderClass,
                                                                                                            V stateBuilderClass) throws Exception {
        return entry(handle, descriptorBuilderClass, stateBuilderClass, null);
    }

    public <T extends AbstractDescriptor.Builder<?>, V extends AbstractState.Builder<?>> MdibDescriptionModifications.Entry entry(String handle,
                                                                                                            T descriptorBuilderClass,
                                                                                                            V stateBuilderClass,
                                                                                                            @Nullable String parentHandle) throws Exception {
        return new MdibDescriptionModifications.Entry(
                descriptor(handle, descriptorBuilderClass).build(),
                state(handle, stateBuilderClass).build(),
                parentHandle);
    }

    public <T extends AbstractDescriptor.Builder<?>, V extends AbstractState.Builder<?>> MdibDescriptionModifications.Entry entry(
        String handle,
        T descriptorBuilderClass,
        V stateBuilderClass,
        Consumer<T> descrCustomizer,
        Consumer<V> stateCustomizer,
        @Nullable String parentHandle
    ) throws Exception {
        return new MdibDescriptionModifications.Entry(
                descriptor(handle, descriptorBuilderClass, descrCustomizer).build(),
                state(handle, stateBuilderClass, stateCustomizer).build(),
                parentHandle);
    }

    public <T extends AbstractDescriptor.Builder<T>, V extends AbstractContextState.Builder<T>> MdibDescriptionModifications.MultiStateEntry contextEntry(String handle,
                                                                                                                                    String stateHandle,
                                                                                                                                    T descrBuilderClass,
                                                                                                                                    V stateBuilderClass,
                                                                                                                                    String parentHandle) throws Exception {
        return new MdibDescriptionModifications.MultiStateEntry(
                descriptor(handle, descrBuilderClass).build(),
                Collections.singletonList(MockModelFactory.createContextState(stateHandle, handle, stateBuilderClass).build()),
                parentHandle);
    }

    public <T extends AbstractDescriptor.Builder<?>, V extends AbstractContextState.Builder<?>> MdibDescriptionModifications.MultiStateEntry contextEntry(String handle,
                                                                                                                                    List<String> stateHandles,
                                                                                                                                    T descrBuilderClass,
                                                                                                                                    V stateBuilderClass,
                                                                                                                                    String parentHandle) throws Exception {
        final List<AbstractContextState> states = stateHandles.stream()
                .map(s -> MockModelFactory.createContextState(s, handle, stateBuilderClass).build())
                .collect(Collectors.toList());
        return new MdibDescriptionModifications.MultiStateEntry(
                descriptor(handle, descrBuilderClass).build(),
                states,
                parentHandle);
    }

    public <T extends AbstractDescriptor.Builder<?>> T descriptor(String handle, T builder) {
        return descriptor(handle, builder, t -> {});
    }

    public <T extends AbstractDescriptor.Builder<?>> T descriptor(String handle, T builder, Consumer<T> customizer) {
        var descr = MockModelFactory.createDescriptor(handle, builder);
        customizer.accept(descr);
        return descr;
    }

    public <T extends AbstractState.Builder<?>> T state(String handle, T builderClass) {
        return state(handle, builderClass, t -> {});
    }

    public <T extends AbstractState.Builder<?>> T state(String handle, T builderClass, Consumer<T> customizer) {
        var state = MockModelFactory.createState(handle, builderClass);
        customizer.accept(state);
        return state;
    }

    public <T extends AbstractContextState.Builder<?>> T state(String handle, String descrHandle, T builderClass) {
        return MockModelFactory.createContextState(handle, descrHandle, builderClass);
    }
}
