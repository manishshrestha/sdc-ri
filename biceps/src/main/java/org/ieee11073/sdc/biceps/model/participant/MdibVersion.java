package org.ieee11073.sdc.biceps.model.participant;

import org.ieee11073.sdc.common.helper.ObjectStringifier;

import java.math.BigInteger;
import java.net.URI;
import java.util.UUID;

/**
 * Container for MDIB version attributes.
 * <p>
 * {@linkplain MdibVersion} models sequence id, instance id and version number, enclosed in one class.
 * {@linkplain MdibVersion} is an immutable class and provides means to
 * <ul>
 * <li>create versions with random UUID by {@link #create()}
 * <li>increment versions by {@link #increment(MdibVersion)}
 * <li>set the version counter of an {@linkplain MdibVersion} by {@link #setVersionCounter(MdibVersion, BigInteger)}
 * <li>compare versions (see {@link #equals(Object)})
 * </ul>
 */
public class MdibVersion {
    private final URI sequenceId;
    private final BigInteger instanceId;
    private final BigInteger version;


    /**
     * Creates a new instance with a random sequence id.
     *
     * @return a new instance.
     */
    public static MdibVersion create() {
        return new MdibVersion(URI.create("urn:uuid:" + UUID.randomUUID().toString()));
    }

    /**
     * Accepts an existing instance and increments the version counter.
     *
     * @param mdibVersion the version base.
     * @return a new instance with same sequence and instance id as in {@code mdibVersion} plus a version counter
     * incremented by one.
     */
    public static MdibVersion increment(MdibVersion mdibVersion) {
        return new MdibVersion(mdibVersion.getSequenceId(), mdibVersion.getVersion().add(BigInteger.ONE),
                mdibVersion.getInstanceId());
    }

    /**
     * Accepts an existing instance and resets the version counter to the given number
     * @param mdibVersion the version base.
     * @param versionCounter
     * @return a new instance with same sequence and instance id as in {@code mdibVersion} plus a version counter
     * incremented by {@code versionCounter}.
     */
    public static MdibVersion setVersionCounter(MdibVersion mdibVersion, BigInteger versionCounter) {
        return new MdibVersion(mdibVersion.getSequenceId(), versionCounter, mdibVersion.getInstanceId());
    }

    /**
     * Constructor that sets a given sequence id.
     * <p>
     * Instance id and version counter are initialized with 0.
     *
     * @param sequenceId the sequence id to set.
     */
    public MdibVersion(URI sequenceId) {
        this.sequenceId = sequenceId;
        this.version = BigInteger.ZERO;
        this.instanceId = BigInteger.ZERO;
    }

    /**
     * Constructor that sets a given sequence id and version counter.
     * <p>
     * Instance id is initialized with 0.
     *
     * @param sequenceId the sequence id to set.
     * @param version the version counter to set.
     */
    public MdibVersion(URI sequenceId, BigInteger version) {
        this.sequenceId = sequenceId;
        this.version = version;
        this.instanceId = BigInteger.ZERO;
    }

    /**
     * Constructor that sets all version attributes.
     *
     * @param sequenceId the sequence id to set.
     * @param version the version counter to set.
     * @param instanceId the instance id to set.
     */
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

    /**
     * Compares two {@linkplain MdibVersion} objects on equality.
     *
     * @param rhsObject the right hand side to compare against with this object.
     * @return true if all version attributes equal true, otherwise false.
     */
    @Override
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
        return ObjectStringifier.stringifyAll(this);
    }
}
