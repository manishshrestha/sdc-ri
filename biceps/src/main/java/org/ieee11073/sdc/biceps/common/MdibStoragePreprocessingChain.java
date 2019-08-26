package org.ieee11073.sdc.biceps.common;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.ieee11073.sdc.biceps.model.participant.AbstractMultiState;
import org.ieee11073.sdc.biceps.model.participant.AbstractState;

import java.util.List;
import java.util.Optional;

public class MdibStoragePreprocessingChain {
    private final MdibStorage mdibStorage;
    private final List<DescriptionPreprocessingSegment> descriptionChainSegments;
    private final List<StatePreprocessingSegment> stateChainSegments;
    private final MdibTypeValidator typeValidator;

    @Inject
    MdibStoragePreprocessingChain(@Assisted MdibStorage mdibStorage,
                                  @Assisted List<DescriptionPreprocessingSegment> descriptionChainSegments,
                                  @Assisted List<StatePreprocessingSegment> stateChainSegments,
                                  MdibTypeValidator typeValidator) {
        this.mdibStorage = mdibStorage;
        this.descriptionChainSegments = descriptionChainSegments;
        this.stateChainSegments = stateChainSegments;
        this.typeValidator = typeValidator;
    }

    public void processDescriptionModifications(MdibDescriptionModifications modifications) throws PreprocessingException {
        final List<MdibDescriptionModification> modificationList = modifications.getModifications();
        int sizeToIterate = modificationList.size();
        for (int i = 0; i < sizeToIterate; ++i) {
            for (DescriptionPreprocessingSegment chainSegment : descriptionChainSegments) {
                try {
                    chainSegment.process(modifications, modificationList.get(i), mdibStorage);
                } catch (Exception e) {
                    throw new PreprocessingException(e.getMessage(), e.getCause(),
                            modificationList.get(i).getHandle(), chainSegment.toString());
                }
            }
            sizeToIterate = modificationList.size();
        }
    }

    public void processStateModifications(MdibStateModifications modifications) throws PreprocessingException {
        for (AbstractState modification : modifications.getStates()) {
            for (StatePreprocessingSegment chainSegment : stateChainSegments) {
                try {
                    chainSegment.process(modification, mdibStorage);
                } catch (Exception e) {
                    final Optional<AbstractMultiState> multiState = typeValidator.toMultiState(modification);
                    String handle = modification.getDescriptorHandle();
                    if (multiState.isPresent()) {
                        handle = multiState.get().getHandle();
                    }

                    throw new PreprocessingException(e.getMessage(), e.getCause(), handle, chainSegment.toString());
                }
            }
        }
    }
}
