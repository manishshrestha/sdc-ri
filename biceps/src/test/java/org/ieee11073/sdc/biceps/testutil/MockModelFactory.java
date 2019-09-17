package org.ieee11073.sdc.biceps.testutil;

import org.ieee11073.sdc.biceps.model.participant.AbstractContextState;
import org.ieee11073.sdc.biceps.model.participant.AbstractDescriptor;
import org.ieee11073.sdc.biceps.model.participant.AbstractState;

import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;

/**
 * Test utility to create MDIB types with handle information.
 */
public class MockModelFactory {
    public static <T extends AbstractDescriptor> T createDescriptor(String handle, Class<T> type)
            throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        return createDescriptor(handle, BigInteger.ZERO, type);
    }

    public static <T extends AbstractDescriptor> T createDescriptor(String handle, BigInteger version, Class<T> type)
            throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        T instance = type.getDeclaredConstructor().newInstance();
        instance.setHandle(handle);
        instance.setDescriptorVersion(version);
        return instance;
    }

    public static <T extends AbstractState> T createState(String handle, Class<T> type)
            throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        return createState(handle, BigInteger.ZERO, BigInteger.ZERO, type);
    }

    public static <T extends AbstractState> T createState(String handle, BigInteger stateVersion, Class<T> type)
            throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        return createState(handle, stateVersion, BigInteger.ZERO, type);
    }

    public static <T extends AbstractState> T createState(String handle,
                                                          BigInteger stateVersion,
                                                          BigInteger descriptorVersion,
                                                          Class<T> type)
            throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        T instance = type.getDeclaredConstructor().newInstance();
        instance.setDescriptorHandle(handle);
        instance.setStateVersion(stateVersion);
        instance.setDescriptorVersion(descriptorVersion);
        return instance;
    }

    public static <T extends AbstractContextState> T createContextState(String handle, String descrHandle, Class<T> type)
            throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        return createContextState(handle, descrHandle, BigInteger.ZERO, BigInteger.ZERO, type);
    }

    public static <T extends AbstractContextState> T createContextState(String handle,
                                                                        String descrHandle,
                                                                        BigInteger stateVersion,
                                                                        Class<T> type)
            throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        return createContextState(handle, descrHandle, stateVersion, BigInteger.ZERO, type);
    }

    public static <T extends AbstractContextState> T createContextState(String handle,
                                                                        String descrHandle,
                                                                        BigInteger stateVersion,
                                                                        BigInteger descriptorVersion,
                                                                        Class<T> type)
            throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        T instance = type.getDeclaredConstructor().newInstance();
        instance.setHandle(handle);
        instance.setDescriptorHandle(descrHandle);
        instance.setStateVersion(stateVersion);
        instance.setDescriptorVersion(descriptorVersion);
        return instance;
    }
}
