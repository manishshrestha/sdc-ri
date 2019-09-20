package org.ieee11073.sdc.biceps.common;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.ieee11073.sdc.biceps.common.access.CopyManager;
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
    private final List<AbstractState> states;
    private final MdibVersion mdibVersion;
    private final CopyManager copyManager;

    @AssistedInject
    MdibEntityImpl(@Assisted @Nullable String parent,
                   @Assisted("children") List<String> children,
                   @Assisted AbstractDescriptor descriptor,
                   @Assisted("states") List<AbstractState> states,
                   @Assisted MdibVersion mdibVersion,
                   CopyManager copyManager) {
        this.parent = parent;
        this.children = children;
        this.descriptor = descriptor;
        this.states = states;
        this.mdibVersion = mdibVersion;
        this.copyManager = copyManager;
    }

    @Override
    public MdibVersion getLastChanged() {
        return mdibVersion;
    }

    @Override
    public List<String> getChildren() {
        return children;
    }

    @Override
    public AbstractDescriptor getDescriptor() {
        return copyManager.processOutput(descriptor);
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
    public List<AbstractState> getStates() {
        return copyManager.processOutput(states);
    }

    @Override
    public StateAlternative<List<AbstractMultiState>> doIfSingleState(Consumer<AbstractState> consumer) {
        if (!getStates().isEmpty()) {
            if (!getStates().get(0).getClass().isAssignableFrom(AbstractMultiState.class)) {
                consumer.accept(getStates().get(0));
                return stateAlternativeConsumer -> {
                };
            }
        }

        return stateAlternativeConsumer -> stateAlternativeConsumer.accept((List) getStates());
    }

    @Override
    public StateAlternative<AbstractState> doIfMultiState(Consumer<List<AbstractMultiState>> consumer) {
        if (getStates().isEmpty()) {
            consumer.accept(Collections.emptyList());
        } else {
            if (getStates().get(0).getClass().isAssignableFrom(AbstractMultiState.class)) {
                consumer.accept((List) getStates());
            }
        }
        return stateAlternativeConsumer -> stateAlternativeConsumer.accept(getStates().get(0));
    }
}
