package org.ieee11073.sdc.biceps.common.factory;

import com.google.inject.Inject;
import org.ieee11073.sdc.biceps.common.MdibEntity;
import org.ieee11073.sdc.biceps.model.participant.MdibVersion;
import org.ieee11073.sdc.biceps.model.participant.AbstractDescriptor;
import org.ieee11073.sdc.biceps.model.participant.AbstractState;

import javax.annotation.Nullable;
import java.util.List;

public class MdibEntityFactory {
    private final MdibEntityGuiceAssistedFactory factory;

    @Inject
    MdibEntityFactory(MdibEntityGuiceAssistedFactory factory) {
        this.factory = factory;
    }

    public MdibEntity createMdibEntity(@Nullable String parent,
                                       List<String> children,
                                       AbstractDescriptor descriptor,
                                       List<AbstractState> states,
                                       MdibVersion mdibVersion) {
        return factory.createMdibEntity(parent, children, descriptor, states, mdibVersion);
    }

    public MdibEntity createShallowCopy(MdibEntity mdibEntity) {
        return factory.createMdibEntity(mdibEntity.getParent().orElse(null), mdibEntity.getChildren(),
                mdibEntity.getDescriptor(), mdibEntity.getStates(), mdibEntity.getLastChanged());
    }

    public MdibEntity replaceDescriptorAndStates(MdibEntity mdibEntity,
                                                 AbstractDescriptor descriptor,
                                                 List<AbstractState> states) {
        return factory.createMdibEntity(mdibEntity.getParent().orElse(null), mdibEntity.getChildren(),
                descriptor, states, mdibEntity.getLastChanged());
    }

    public MdibEntity replaceStates(MdibEntity mdibEntity,
                                    List<AbstractState> states) {
        return replaceDescriptorAndStates(mdibEntity, mdibEntity.getDescriptor(), states);
    }

    public MdibEntity replaceChildren(MdibEntity mdibEntity, List<String> children) {
        return factory.createMdibEntity(mdibEntity.getParent().orElse(null), children,
                mdibEntity.getDescriptor(), mdibEntity.getStates(), mdibEntity.getLastChanged());
    }
}
