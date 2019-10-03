package org.ieee11073.sdc.biceps.common.access;


import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.ieee11073.sdc.biceps.common.CommonConfig;
import org.ieee11073.sdc.common.util.ObjectUtil;

/**
 * A utility class to deep copy any input and output if configured.
 * <p>
 * <b>Problem description:</b>
 * <p>
 * Any JAXB converted classes come with getters and setters, hence it is not possible to create read only entities as
 * one can always access a descriptor or state of an entity and change it if desired.
 * By copying incoming and outgoing data the MDIB entities are protected against inadvertent or deliberate
 * modification at the cost of performance.
 * If performance plays a role and it can be guaranteed that MDIB data will not be modified at all, then
 * {@link CommonConfig#COPY_MDIB_INPUT} and {@link CommonConfig#COPY_MDIB_OUTPUT} can be configured such that no copies
 * of descriptors and states are made throughout the MDIB.
 * <p>
 * Default configuration is <em>yes, copy input and output data</em>.
 */
public class CopyManager {
    private final ObjectUtil objectUtil;
    private final Boolean copyInput;
    private final Boolean copyOutput;

    @Inject
    CopyManager(ObjectUtil objectUtil,
                @Named(CommonConfig.COPY_MDIB_INPUT) Boolean copyInput,
                @Named(CommonConfig.COPY_MDIB_OUTPUT) Boolean copyOutput) {
        this.objectUtil = objectUtil;
        this.copyInput = copyInput;
        this.copyOutput = copyOutput;
    }

    /**
     * Copies input data if configured.
     * <p>
     * Data is copied if {@link CommonConfig#COPY_MDIB_INPUT} is configured true.
     * Data is forwarded otherwise.
     *
     * @param input data to be copied.
     * @param <T>   any type that is supposed to be deep-copied.
     * @return a copy of {@code input} if configured, otherwise a forwarded reference.
     */
    public <T> T processInput(T input) {
        return doDeepCopyIfConfigured(copyInput, input);
    }

    /**
     * Copies output data if configured.
     * <p>
     * Data is copied if {@link CommonConfig#COPY_MDIB_OUTPUT} is configured true.
     * Data is forwarded otherwise.
     *
     * @param output data to be copied.
     * @param <T>    any type that is supposed to be deep-copied.
     * @return a copy of {@code output} if configured, otherwise a forwarded reference.
     */
    public <T> T processOutput(T output) {
        return doDeepCopyIfConfigured(copyOutput, output);
    }

    private <T> T doDeepCopyIfConfigured(boolean doCopy, T data) {
        if (doCopy) {
            return objectUtil.deepCopy(data);
        } else {
            return data;
        }
    }
}
