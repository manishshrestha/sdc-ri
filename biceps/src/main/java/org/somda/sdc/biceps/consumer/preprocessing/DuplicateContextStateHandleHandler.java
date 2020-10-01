package org.somda.sdc.biceps.consumer.preprocessing;

import com.google.inject.Inject;
import org.somda.sdc.biceps.common.MdibStateModifications;
import org.somda.sdc.biceps.common.MdibTypeValidator;
import org.somda.sdc.biceps.common.storage.MdibStorage;
import org.somda.sdc.biceps.common.storage.StatePreprocessingSegment;
import org.somda.sdc.biceps.model.participant.AbstractMultiState;
import org.somda.sdc.biceps.model.participant.AbstractState;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Preprocessing segment that throws an Exception, if a duplicate context state handle is used.
 */
public class DuplicateContextStateHandleHandler implements StatePreprocessingSegment {
    private final MdibTypeValidator typeValidator;

    private Map<String, AbstractMultiState> allContextStates;

    @Inject
    DuplicateContextStateHandleHandler(MdibTypeValidator typeValidator) {
        this.typeValidator = typeValidator;
    }

    @Override
    public void beforeFirstModification(MdibStateModifications modifications, MdibStorage mdibStorage) {
        allContextStates = new HashMap<>();
        for (var state : mdibStorage.getStatesByType(AbstractMultiState.class)) {
            allContextStates.put(state.getHandle(), state);
        }
    }

    @Override
    public void process(MdibStateModifications modifications, AbstractState modification, MdibStorage storage)
            throws DuplicateContextStateHandleException {

        final Optional<AbstractMultiState> multiState = typeValidator.toMultiState(modification);
        if (multiState.isPresent()) {
            String handle = multiState.get().getHandle();

            if (allContextStates.containsKey(handle)) {
                var existingContextState = allContextStates.get(handle);
                if (!existingContextState.getDescriptorHandle().equals(multiState.get().getDescriptorHandle())) {
                    throw new DuplicateContextStateHandleException(String.format("Two different descriptors:"
                                    + " %s and %s can not have the same context state: %s",
                            existingContextState.getDescriptorHandle(),
                            multiState.get().getDescriptorHandle(),
                            multiState.get().getHandle()));
                }
            }
            allContextStates.put(handle, multiState.get());
        }
    }

    @Override
    public void afterLastModification(MdibStateModifications modifications, MdibStorage mdibStorage) {
        allContextStates = null;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
