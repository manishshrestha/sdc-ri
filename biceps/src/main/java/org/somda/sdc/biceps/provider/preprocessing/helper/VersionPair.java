package org.somda.sdc.biceps.provider.preprocessing.helper;

import java.math.BigInteger;

/**
 * Version information used by {@link org.somda.sdc.biceps.provider.preprocessing.VersionHandler}.
 * <p>
 * This class had to be moved from a nested class in {@link org.somda.sdc.biceps.provider.preprocessing.VersionHandler}
 * into a separate class, because {@link org.somda.sdc.common.util.ObjectUtilImpl} always cloned the
 * {@link org.somda.sdc.biceps.provider.preprocessing.VersionHandler} as well, which caused a massive memory leak
 * and eventually lead to a stack overflow as well.
 */
public class VersionPair {
    private final BigInteger descriptorVersion;
    private final BigInteger stateVersion;

    public VersionPair() {
        descriptorVersion = BigInteger.valueOf(-1);
        stateVersion = BigInteger.valueOf(-1);
    }

    public VersionPair(BigInteger descriptorVersion, BigInteger stateVersion) {
        this.descriptorVersion = descriptorVersion;
        this.stateVersion = stateVersion;
    }

    public VersionPair(BigInteger descriptorVersion) {
        this.descriptorVersion = descriptorVersion;
        this.stateVersion = BigInteger.ZERO;
    }

    public BigInteger getDescriptorVersion() {
        return descriptorVersion;
    }

    public BigInteger getStateVersion() {
        return stateVersion;
    }
}