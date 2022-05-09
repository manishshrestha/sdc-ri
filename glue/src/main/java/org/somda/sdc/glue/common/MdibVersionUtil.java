package org.somda.sdc.glue.common;

import com.google.inject.Inject;
import org.somda.sdc.biceps.model.message.AbstractGetResponse;
import org.somda.sdc.biceps.model.message.AbstractReport;
import org.somda.sdc.biceps.model.message.AbstractSetResponse;
import org.somda.sdc.biceps.model.participant.MdibVersion;

import javax.annotation.Nullable;
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
     * @param <T>         any type that supports setSequenceId, setInstanceId and setMdibVersion
     *                    (e.g., {@link org.somda.sdc.biceps.model.participant.Mdib}.
     * @throws NoSuchMethodException     in case one of the methods setSequenceId, setInstanceId and setMdibVersion
     *                                   does not exist.
     * @throws InvocationTargetException in case one of the methods setSequenceId, setInstanceId and setMdibVersion
     *                                   cannot be applied on target.
     * @throws IllegalAccessException    in case one of the methods setSequenceId, setInstanceId and setMdibVersion
     *                                   cannot be applied on target.
     */
    public <T> void setMdibVersion(MdibVersion mdibVersion, T target)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        final Method setSequenceId = target.getClass().getMethod("setSequenceId", String.class);
        final Method setInstanceId = target.getClass().getMethod("setInstanceId", BigInteger.class);
        final Method setMdibVersion = target.getClass().getMethod("setMdibVersion", BigInteger.class);

        setSequenceId.invoke(target, mdibVersion.getSequenceId());
        setInstanceId.invoke(target, mdibVersion.getInstanceId());
        setMdibVersion.invoke(target, mdibVersion.getVersion());
    }

    /**
     * Stores {@linkplain MdibVersion} attributes in any {@link AbstractGetResponse} builder.
     *
     * @param version the {@link MdibVersion} to store.
     * @param target      the target where to store sequence id, instance id and version from given {@link MdibVersion}.
     * @param <T>         any {@link AbstractGetResponse} builder
     */
    public <T extends AbstractGetResponse.Builder<?>> void setResponseMdibVersion(MdibVersion version, T target) {
        target.withMdibVersion(version.getVersion());
        target.withInstanceId(version.getInstanceId());
        target.withSequenceId(version.getSequenceId());
    }

    /**
     * Stores {@linkplain MdibVersion} attributes in any {@link AbstractGetResponse} builder.
     *
     * @param version the {@link MdibVersion} to store.
     * @param target      the target where to store sequence id, instance id and version from given {@link MdibVersion}.
     * @param <T>         any {@link AbstractGetResponse} builder
     */
    public <T extends AbstractSetResponse.Builder<?>> void setResponseMdibVersion(MdibVersion version, T target) {
        target.withMdibVersion(version.getVersion());
        target.withInstanceId(version.getInstanceId());
        target.withSequenceId(version.getSequenceId());
    }

    /**
     * Stores {@linkplain MdibVersion} attributes in any {@link AbstractReport} builder.
     *
     * @param version the {@link MdibVersion} to store.
     * @param target      the target where to store sequence id, instance id and version from given {@link MdibVersion}.
     * @param <T>         any {@link AbstractReport} builder
     */
    public <T extends AbstractReport.Builder<?>> void setReportMdibVersion(MdibVersion version, T target) {
        target.withMdibVersion(version.getVersion());
        target.withInstanceId(version.getInstanceId());
        target.withSequenceId(version.getSequenceId());
    }

    /**
     * Extracts the MDIB version from a report.
     *
     * @param msg the report to extract data from.
     * @return the converted MDIB version. Default values are transformed according to BICEPS's prose information.
     */
    public MdibVersion getMdibVersion(AbstractReport msg) {
        return new MdibVersion(msg.getSequenceId(),
                defaultZero(msg.getMdibVersion()), defaultZero(msg.getInstanceId()));
    }

    /**
     * Extracts the MDIB version from a get response.
     *
     * @param msg the get response to extract data from.
     * @return the converted MDIB version. Default values are transformed according to BICEPS's prose information.
     */
    public MdibVersion getMdibVersion(AbstractGetResponse msg) {
        return new MdibVersion(msg.getSequenceId(), defaultZero(msg.getMdibVersion()),
                defaultZero(msg.getInstanceId()));
    }

    private BigInteger defaultZero(@Nullable BigInteger instanceId) {
        return instanceId == null ? BigInteger.ZERO : instanceId;
    }
}
