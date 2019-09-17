package org.ieee11073.sdc.biceps.common.factory;

import com.google.inject.assistedinject.Assisted;
import org.ieee11073.sdc.biceps.common.*;
import org.ieee11073.sdc.biceps.model.participant.AbstractState;

import java.util.List;

public interface MdibStoragePreprocessingChainFactory {
    MdibStoragePreprocessingChain createMdibStoragePreprocessingChain(@Assisted MdibStorage mdibStorage,
                                                                      @Assisted List<DescriptionPreprocessingSegment> descriptionChainSegments,
                                                                      @Assisted List<StatePreprocessingSegment> stateChainSegments);
}
