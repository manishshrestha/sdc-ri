package org.somda.sdc.biceps.provider.preprocessing.helper;

import org.somda.sdc.biceps.common.MdibEntity;
import org.somda.sdc.biceps.model.participant.AbstractContextState;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.Optional;

/**
 * Represents a singular version for use in {@link org.somda.sdc.biceps.provider.preprocessing.VersionHandler}.
 */
public class Version {
    private final @Nullable BigInteger version;

    public Version(MdibEntity entity) {
        this(entity.getDescriptor().getDescriptorVersion());
    }

    public Version(AbstractContextState state) {
        this(state.getStateVersion());
    }

    public Version(@Nullable BigInteger version) {
        this.version = Optional.ofNullable(version).orElse(BigInteger.ZERO);;
    }

    public Version() {
        this.version = null;
    }

    public BigInteger getVersion() {
        return Optional.ofNullable(version).orElse(BigInteger.ZERO);
    }

    /**
     * @return a new incremented version, zero if no previous version set.
     */
    public Version increment() {
        return new Version(
            Optional.ofNullable(this.version).map(it -> it.add(BigInteger.ONE)).orElse(BigInteger.ZERO)
        );
    }

}
