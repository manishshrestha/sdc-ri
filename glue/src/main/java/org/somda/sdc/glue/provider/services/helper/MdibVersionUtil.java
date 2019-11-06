package org.somda.sdc.glue.provider.services.helper;

import com.google.inject.Inject;
import org.somda.sdc.biceps.model.participant.MdibVersion;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;

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
}
