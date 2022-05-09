package org.somda.sdc.biceps.testutil;

import org.checkerframework.checker.units.qual.C;
import org.somda.sdc.biceps.model.participant.AbstractContextState;
import org.somda.sdc.biceps.model.participant.AbstractDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractState;
import org.somda.sdc.biceps.model.participant.ContextAssociation;

import java.math.BigInteger;

/**
 * Test utility to create MDIB types with handle information.
 */
public class MockModelFactory {
    public static <T extends AbstractDescriptor.Builder<?>> T createDescriptor(String handle, T builder) {
        return createDescriptor(handle, BigInteger.ZERO, builder);
    }

    public static <T extends AbstractDescriptor.Builder<?>> T createDescriptor(String handle, BigInteger version, T builder) {
        try {
            return (T) builder.withHandle(handle)
                .withDescriptorVersion(version);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T extends AbstractState.Builder<?>> T createState(String handle, T type) {
        try {
            return createState(handle, BigInteger.ZERO, BigInteger.ZERO, type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T extends AbstractState.Builder<?>> T createState(String handle, BigInteger stateVersion, T type) {
        try {
            return createState(handle, stateVersion, BigInteger.ZERO, type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T extends AbstractState.Builder<?>> T createState(String handle,
                                                          BigInteger stateVersion,
                                                          BigInteger descriptorVersion,
                                                          T type) {
        try {
            return (T) type
                .withDescriptorHandle(handle)
                .withStateVersion(stateVersion)
                .withDescriptorVersion(descriptorVersion);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T extends AbstractContextState.Builder<?>> T createContextState(String handle, String descrHandle, T builderClass) {
        return createContextState(handle, descrHandle, BigInteger.ZERO, BigInteger.ZERO, builderClass);
    }

    public static <T extends AbstractContextState.Builder<?>> T createContextState(String handle,
                                                                        String descrHandle,
                                                                        BigInteger stateVersion,
                                                                        T builderClass) {
        return createContextState(handle, descrHandle, stateVersion, BigInteger.ZERO, builderClass);
    }

    public static <T extends AbstractContextState.Builder<?>> T createContextState(String handle,
                                                                        String descrHandle,
                                                                        BigInteger stateVersion,
                                                                        BigInteger descriptorVersion,
                                                                        T builderClass) {
        try {
            return (T) builderClass.withContextAssociation(ContextAssociation.DIS)
                .withHandle(handle)
                .withDescriptorHandle(descrHandle)
                .withStateVersion(stateVersion)
                .withDescriptorVersion(descriptorVersion);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
