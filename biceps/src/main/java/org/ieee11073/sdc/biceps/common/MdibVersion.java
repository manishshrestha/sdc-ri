package org.ieee11073.sdc.biceps.common;

import org.ieee11073.sdc.biceps.model.participant.Mdib;

import java.math.BigInteger;
import java.net.URI;
import java.util.UUID;

public class MdibVersion {
    private final URI sequenceId;
    private final BigInteger version;
    private final BigInteger instanceId;

    public static MdibVersion create() {
        return new MdibVersion(URI.create("urn:uuid:" + UUID.randomUUID().toString()));
    }

    public static MdibVersion increment(MdibVersion mdibVersion) {
        return new MdibVersion(mdibVersion.getSequenceId(), mdibVersion.getVersion().add(BigInteger.ONE),
                mdibVersion.getInstanceId());
    }

    public static MdibVersion setVersionCounter(MdibVersion mdibVersion, BigInteger versionCounter) {
        return new MdibVersion(mdibVersion.getSequenceId(), versionCounter, mdibVersion.getInstanceId());
    }

    public MdibVersion(URI sequenceId) {
        this.sequenceId = sequenceId;
        this.version = BigInteger.ZERO;
        this.instanceId = BigInteger.ZERO;
    }

    public MdibVersion(URI sequenceId, BigInteger version) {
        this.sequenceId = sequenceId;
        this.version = version;
        this.instanceId = BigInteger.ZERO;
    }

    public MdibVersion(URI sequenceId, BigInteger version, BigInteger instanceId) {
        this.sequenceId = sequenceId;
        this.version = version;
        this.instanceId = instanceId;
    }

    public URI getSequenceId() {
        return sequenceId;
    }

    public BigInteger getVersion() {
        return version;
    }

    public BigInteger getInstanceId() {
        return instanceId;
    }

    public boolean equals(Object rhsObject) {
        if (rhsObject == this) {
            return true;
        }
        if (!(rhsObject instanceof MdibVersion)) {
            return false;
        }

        MdibVersion rhs = (MdibVersion) rhsObject;
        return this.sequenceId.equals(rhs.sequenceId)
                && this.version.equals(rhs.version)
                && this.instanceId.equals(rhs.instanceId);
    }

    @Override
    public String toString() {
        return String.format("MdibVersion(sequence=%s;instance=%s;version=%s)", sequenceId, instanceId, version);
    }
}
