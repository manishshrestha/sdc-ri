package org.somda.sdc.biceps.provider.preprocessing.helper;

import org.somda.sdc.biceps.common.MdibEntity;
import org.somda.sdc.biceps.model.participant.AbstractMultiState;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.Optional;


/**
 * Version information used by {@link org.somda.sdc.biceps.provider.preprocessing.VersionHandler}.
 */
public class VersionPair {
    private final @Nullable BigInteger descriptorVersion;
    private final @Nullable BigInteger stateVersion;

    public VersionPair() {
        descriptorVersion = null;
        stateVersion = null;
    }

    public VersionPair(BigInteger descriptorVersion, BigInteger stateVersion) {
        this.descriptorVersion = descriptorVersion;
        this.stateVersion = stateVersion;
    }

    public VersionPair(BigInteger descriptorVersion) {
        this.descriptorVersion = descriptorVersion;
        this.stateVersion = null;
    }

    public VersionPair(MdibEntity entity) {
        this.descriptorVersion = entity.getDescriptor().getDescriptorVersion();
        final BigInteger[] newStateVersion = {BigInteger.valueOf(-1)};
        entity.doIfSingleState(it -> newStateVersion[0] = it.getStateVersion());
        this.stateVersion = newStateVersion[0];
    }

    public VersionPair(AbstractMultiState state) {
        this.descriptorVersion = state.getDescriptorVersion();
        this.stateVersion = state.getStateVersion();
    }

    public BigInteger getDescriptorVersion() {
        return Optional.ofNullable(descriptorVersion).orElse(BigInteger.ZERO);
    }

    public BigInteger getStateVersion() {
        return Optional.ofNullable(stateVersion).orElse(BigInteger.ZERO);
    }

    /**
     * @return an incremented descriptor version or zero if no version was set previously
     */
    public BigInteger incrementDescriptorVersion() {
        return Optional.ofNullable(descriptorVersion).map(it -> it.add(BigInteger.ONE)).orElse(BigInteger.ZERO);
    }

    /**
     * @return an incremented state version or zero if no version was set previously
     */
    public BigInteger incrementStateVersion() {
        return Optional.ofNullable(stateVersion).map(it -> it.add(BigInteger.ONE)).orElse(BigInteger.ZERO);
    }
}