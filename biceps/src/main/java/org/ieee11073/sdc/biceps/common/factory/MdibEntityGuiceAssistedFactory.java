package org.ieee11073.sdc.biceps.common.factory;

import com.google.inject.assistedinject.Assisted;
import org.ieee11073.sdc.biceps.common.MdibEntity;
import org.ieee11073.sdc.biceps.model.participant.AbstractDescriptor;
import org.ieee11073.sdc.biceps.model.participant.AbstractState;

import javax.annotation.Nullable;
import java.util.List;

public interface MdibEntityGuiceAssistedFactory {
    MdibEntity createMdibEntity(@Assisted @Nullable String parent,
                                @Assisted("children") List<String> children,
                                @Assisted AbstractDescriptor descriptor,
                                @Assisted("states") List<? extends AbstractState> states);
}
