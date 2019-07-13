package org.ieee11073.sdc.biceps.testutil;

import org.ieee11073.sdc.biceps.model.participant.AbstractContextState;
import org.ieee11073.sdc.biceps.model.participant.AbstractDescriptor;
import org.ieee11073.sdc.biceps.model.participant.AbstractState;

/**
 * Test utility to create MDIB types with handle information.
 */
public class MockModelFactory {
    public static <T extends AbstractDescriptor> T createDescriptor(String handle, Class<T> type)
            throws IllegalAccessException, InstantiationException {
        T instance = type.newInstance();
        instance.setHandle(handle);
        return instance;
    }

    public static <T extends AbstractState> T createState(String handle, Class<T> type)
            throws IllegalAccessException, InstantiationException {
        T instance = type.newInstance();
        instance.setDescriptorHandle(handle);
        return instance;
    }

    public static <T extends AbstractContextState> T createContextState(String handle, String descrHandle, Class<T> type)
            throws IllegalAccessException, InstantiationException {
        T instance = type.newInstance();
        instance.setHandle(handle);
        instance.setDescriptorHandle(descrHandle);
        return instance;
    }
}
