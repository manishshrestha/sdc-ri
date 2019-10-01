package org.ieee11073.sdc.biceps.testutil;

import org.ieee11073.sdc.biceps.common.MdibDescriptionModifications;
import org.ieee11073.sdc.biceps.common.MdibTypeValidator;
import org.ieee11073.sdc.biceps.model.participant.AbstractContextState;
import org.ieee11073.sdc.biceps.model.participant.AbstractDescriptor;
import org.ieee11073.sdc.biceps.model.participant.AbstractState;

import javax.annotation.Nullable;
import java.util.Collections;

public class MockEntryFactory {
    private final MdibTypeValidator typeValidator;

    public MockEntryFactory(MdibTypeValidator typeValidator) {
        this.typeValidator = typeValidator;
    }

    public <T extends AbstractDescriptor, V extends AbstractState> MdibDescriptionModifications.Entry entry(String handle, Class<T> descrClass) throws Exception {
        return entry(handle, descrClass, null);
    }

    public <T extends AbstractDescriptor, V extends AbstractState> MdibDescriptionModifications.Entry entry(String handle, Class<T> descrClass, @Nullable String parentHandle) throws Exception {
        Class<V> stateClass = typeValidator.resolveStateType(descrClass);
        return new MdibDescriptionModifications.Entry(
                descriptor(handle, descrClass),
                state(handle, stateClass),
                parentHandle);
    }

    public <T extends AbstractDescriptor, V extends AbstractContextState> MdibDescriptionModifications.MultiStateEntry contextEntry(String handle, String stateHandle, Class<T> descrClass, String parentHandle) throws Exception {
        Class<V> stateClass = typeValidator.resolveStateType(descrClass);
        return new MdibDescriptionModifications.MultiStateEntry(
                descriptor(handle, descrClass),
                Collections.singletonList(MockModelFactory.createContextState(stateHandle, handle, stateClass)),
                parentHandle);
    }

    public <T extends AbstractDescriptor> T descriptor(String handle, Class<T> theClass) {
        return MockModelFactory.createDescriptor(handle, theClass);
    }

    public <T extends AbstractState> T state(String handle, Class<T> theClass) {
        return MockModelFactory.createState(handle, theClass);
    }
}
