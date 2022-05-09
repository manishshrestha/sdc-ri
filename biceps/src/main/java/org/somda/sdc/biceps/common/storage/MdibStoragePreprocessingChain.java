package org.somda.sdc.biceps.common.storage;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.biceps.common.MdibDescriptionModification;
import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.common.MdibStateModifications;
import org.somda.sdc.biceps.common.MdibTypeValidator;

import java.util.List;

/**
 * Provides a processing chain that is supposed to be run before any interaction with
 * an {@linkplain MdibStorage} instance.
 * <p>
 * The {@linkplain MdibStoragePreprocessingChain} offers processing functions for description and state change sets.
 */
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

    /**
     * Accepts a set of description modifications and applies them on every available description chain segment.
     *
     * @param modifications the modification to pass to the chain segments.
     * @throws PreprocessingException in case a chain segment fails.
     * @return processed description modifications
     */
    public List<MdibDescriptionModification> processDescriptionModifications(MdibDescriptionModifications modifications)
            throws PreprocessingException {

        // why is there no fold :(
        List<MdibDescriptionModification> modificationList = modifications.getModifications();

        for (final DescriptionPreprocessingSegment descriptionChainSegment : descriptionChainSegments) {
            modificationList = descriptionChainSegment.beforeFirstModification(modificationList, mdibStorage);
        }

        for (final DescriptionPreprocessingSegment descriptionChainSegment : descriptionChainSegments) {
            try {
                modificationList = descriptionChainSegment.process(modificationList, mdibStorage);
            // CHECKSTYLE.OFF: IllegalCatch
            } catch (Exception e) {
            // CHECKSTYLE.ON: IllegalCatch
                throw new PreprocessingException(e.getMessage(), e.getCause(), descriptionChainSegment.toString());
            }
        }

        for (final DescriptionPreprocessingSegment descriptionChainSegment : descriptionChainSegments) {
            modificationList = descriptionChainSegment.afterLastModification(modificationList, mdibStorage);
        }

        return modificationList;
    }

    /**
     * Accepts a set of state modifications and applies them on every available state chain segment.
     *
     * @param modifications the modification to pass to the chain segments.
     * @throws PreprocessingException in case a chain segment fails.
     */
    public void processStateModifications(MdibStateModifications modifications) throws PreprocessingException {
        stateChainSegments.forEach(chainSegment -> chainSegment.beforeFirstModification(modifications, mdibStorage));

        for (StatePreprocessingSegment chainSegment : stateChainSegments) {
            try {
                chainSegment.process(modifications, mdibStorage);
            // CHECKSTYLE.OFF: IllegalCatch
            } catch (Exception e) {
            // CHECKSTYLE.ON: IllegalCatch
                throw new PreprocessingException(e.getMessage(), e.getCause(), chainSegment.toString());
            }
        }

        stateChainSegments.forEach(chainSegment -> chainSegment.afterLastModification(modifications, mdibStorage));
    }
}
