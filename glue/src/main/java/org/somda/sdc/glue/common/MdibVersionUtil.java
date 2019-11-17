package org.somda.sdc.glue.provider.services.helper;

import com.google.inject.Inject;
import org.somda.sdc.biceps.model.message.AbstractGetResponse;
import org.somda.sdc.biceps.model.message.AbstractReport;
import org.somda.sdc.biceps.model.participant.MdibVersion;

import javax.annotation.Nullable;
import javax.validation.constraints.Null;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.URI;

/**
 * Utility functions for the {@link MdibVersion} container.
 */
public class MdibVersionUtil {
    @Inject
    MdibVersionUtil() {
    }

    /**
     * Stores {@linkplain MdibVersion} attributes in any MDIB version supporting objects.
     *
     * @param mdibVersion the {@link MdibVersion} to store.
     * @param target      the target where to store sequence id, instance id and version from given {@link MdibVersion}.
     * @param <T>         any type that supports setSequenceId, setInstanceId and setMdibVersion (e.g., {@link org.somda.sdc.biceps.model.participant.Mdib}.
     * @throws NoSuchMethodException     in case one of the methods setSequenceId, setInstanceId and setMdibVersion does not exist.
     * @throws InvocationTargetException in case one of the methods setSequenceId, setInstanceId and setMdibVersion cannot be applied on target.
     * @throws IllegalAccessException    in case one of the methods setSequenceId, setInstanceId and setMdibVersion cannot be applied on target.
     */
    public <T> void setMdibVersion(MdibVersion mdibVersion, T target) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        final Method setSequenceId = target.getClass().getMethod("setSequenceId", String.class);
        final Method setInstanceId = target.getClass().getMethod("setInstanceId", BigInteger.class);
        final Method setMdibVersion = target.getClass().getMethod("setMdibVersion", BigInteger.class);

        setSequenceId.invoke(target, mdibVersion.getSequenceId().toString());
        setInstanceId.invoke(target, mdibVersion.getInstanceId());
        setMdibVersion.invoke(target, mdibVersion.getVersion());
    }

    public MdibVersion getMdibVersion(AbstractReport msg) {
        return new MdibVersion(sequenceId(msg.getSequenceId()), msg.getMdibVersion(), instanceId(msg.getInstanceId()));
    }

    public MdibVersion getMdibVersion(AbstractGetResponse msg) {
        return new MdibVersion(sequenceId(msg.getSequenceId()), msg.getMdibVersion(), instanceId(msg.getInstanceId()));
    }

    private BigInteger instanceId(@Nullable BigInteger instanceId) {
        return instanceId == null ? BigInteger.ZERO : instanceId;
    }

    private URI sequenceId(String sequenceId) {
        return URI.create(sequenceId);
    }
}
