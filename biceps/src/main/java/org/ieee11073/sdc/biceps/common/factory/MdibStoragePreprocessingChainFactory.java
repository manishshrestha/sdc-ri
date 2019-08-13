package org.ieee11073.sdc.biceps.common.factory;

import com.google.inject.assistedinject.Assisted;
import org.ieee11073.sdc.biceps.common.MdibDescriptionModification;
import org.ieee11073.sdc.biceps.common.MdibStorage;
import org.ieee11073.sdc.biceps.common.MdibStoragePreprocessingChain;
import org.ieee11073.sdc.biceps.model.participant.AbstractState;

import java.util.List;

public interface MdibStoragePreprocessingChainFactory {
    MdibStoragePreprocessingChain createMdibStoragePreprocessingChain(@Assisted MdibStorage mdibStorage,
                                                                      @Assisted List<MdibStoragePreprocessingChain.Segment<MdibDescriptionModification>> descriptionChainSegments,
                                                                      @Assisted List<MdibStoragePreprocessingChain.Segment<AbstractState>> stateChainSegments);
}
