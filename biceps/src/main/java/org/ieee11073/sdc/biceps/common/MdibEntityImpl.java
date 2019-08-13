package org.ieee11073.sdc.biceps.common;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.ieee11073.sdc.biceps.model.participant.AbstractContextState;
import org.ieee11073.sdc.biceps.model.participant.AbstractDescriptor;
import org.ieee11073.sdc.biceps.model.participant.AbstractMultiState;
import org.ieee11073.sdc.biceps.model.participant.AbstractState;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Default implementation of {@link MdibEntity}.
 */
public class MdibEntityImpl implements MdibEntity {
    private final String parent;
    private final List<String> children;
    private final AbstractDescriptor descriptor;
    private final List<? extends AbstractState> states;

    @AssistedInject
    MdibEntityImpl(@Assisted @Nullable String parent,
                   @Assisted("children") List<String> children,
                   @Assisted AbstractDescriptor descriptor,
                   @Assisted("states") List<? extends AbstractState> states) {
        this.parent = parent;
        this.children = children;
        this.descriptor = descriptor;
        this.states = states;
    }

    @Override
    public List<String> getChildren() {
        return children;
    }

    @Override
    public AbstractDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public String getHandle() {
        return descriptor.getHandle();
    }

    @Override
    public Optional<String> getParent() {
        return Optional.ofNullable(parent);
    }

    @Override
    public List<? extends AbstractState> getStates() {
        return states;
    }

    @Override
    public StateAlternative<List<AbstractMultiState>> doIfSingleState(Consumer<AbstractState> consumer) {
        if (!getStates().isEmpty()) {
            if (!getStates().get(0).getClass().isAssignableFrom(AbstractContextState.class)) {
                consumer.accept(getStates().get(0));
                return stateAlternativeConsumer -> {};
            }
        }

        return stateAlternativeConsumer -> stateAlternativeConsumer.accept((List<AbstractMultiState>)getStates());
    }

    @Override
    public StateAlternative<AbstractState> doIfMultiState(Consumer<List<? extends AbstractMultiState>> consumer) {
        if (getStates().isEmpty()) {
            consumer.accept(Collections.emptyList());
        } else {
            if (getStates().get(0).getClass().isAssignableFrom(AbstractContextState.class)) {
                consumer.accept((List<? extends AbstractMultiState>) getStates());
            }
        }
        return stateAlternativeConsumer -> stateAlternativeConsumer.accept(getStates().get(0));
    }
}
