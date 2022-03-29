package org.somda.sdc.biceps.common.access;


import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.somda.sdc.biceps.common.CommonConfig;
import org.somda.sdc.biceps.common.MdibDescriptionModification;
import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.common.MdibStateModifications;
import org.somda.sdc.biceps.guice.JaxbBiceps;
import org.somda.sdc.biceps.model.participant.AbstractState;
import org.somda.sdc.common.util.ObjectUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    CopyManager(@JaxbBiceps ObjectUtil objectUtil,
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
        if (!copyInput) {
            return input;
        }

        if (input instanceof MdibDescriptionModifications) {
            var data = (MdibDescriptionModifications) input;
            var modificationsCopy = data.getModifications().stream()
                    .map(modification -> new MdibDescriptionModification(
                            modification.getModificationType(),
                            doDeepCopy(modification.getDescriptor()),
                            deepCopyStates(modification.getStates()),
                            modification.getParentHandle().orElse(null)))
                    .collect(Collectors.toList());
            var copy = data.deepCopy(modificationsCopy);

            return (T) copy;
        }

        if (input instanceof MdibStateModifications) {
            var data = (MdibStateModifications) input;
            var copy = MdibStateModifications.create(data.getChangeType());
            copy.addAll(deepCopyStates(data.getStates()));

            return (T) copy;
        }

        return doDeepCopyIfConfigured(true, input);
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
        if (output instanceof List<?>) {
            var list = (List<?>) output;
            return (T) list.stream()
                    .map(o -> doDeepCopyIfConfigured(copyOutput, o))
                    .collect(Collectors.toList());
        }
        return doDeepCopyIfConfigured(copyOutput, output);
    }

    private List<AbstractState> deepCopyStates(List<AbstractState> states) {
        if (states == null) {
            return new ArrayList<>();
        }
        return states.stream()
                .map(this::doDeepCopy)
                .collect(Collectors.toList());
    }

    private <T> T doDeepCopy(T data) {
        return doDeepCopyIfConfigured(true, data);
    }

    private <T> T doDeepCopyIfConfigured(boolean doCopy, T data) {
        if (doCopy) {
            return objectUtil.deepCopyJAXB(data);
        } else {
            return data;
        }
    }
}
