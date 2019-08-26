package org.ieee11073.sdc.biceps.provider.preprocessing;

import com.google.inject.Inject;
import org.ieee11073.sdc.biceps.common.*;
import org.ieee11073.sdc.biceps.model.participant.AbstractDescriptor;
import org.ieee11073.sdc.biceps.model.participant.AbstractMultiState;
import org.ieee11073.sdc.biceps.model.participant.AbstractState;
import org.ieee11073.sdc.common.helper.ObjectUtil;

import java.math.BigInteger;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class VersionHandler implements DescriptionPreprocessingSegment, StatePreprocessingSegment {
    private final Map<String, VersionPair> versionCache;
    private final MdibTypeValidator mdibTypeValidator;
    private final ObjectUtil objectUtil;
    private final Set<String> updatedParents;

    @Inject
    VersionHandler(MdibTypeValidator mdibTypeValidator,
                   ObjectUtil objectUtil) {
        this.mdibTypeValidator = mdibTypeValidator;
        this.objectUtil = objectUtil;
        this.versionCache = new HashMap<>();
        this.updatedParents = new HashSet<>();

    }

    @Override
    public void beforeFirstModification(MdibStorage storage) {
        updatedParents.clear();
    }

    @Override
    public void process(MdibDescriptionModifications modifications,
                        MdibDescriptionModification modification,
                        MdibStorage storage) {
        switch (modification.getModificationType()) {
            case INSERT:
                processInsert(modifications, modification.getDescriptor(), modification.getStates(), modification.getParentHandle(), storage);
                break;
            case UPDATE:
                processUpdate(modification.getDescriptor(), modification.getStates(), storage);
                break;
            case DELETE:
                // Versions are not affected by deletions
                break;
        }
    }

    @Override
    public void process(AbstractState state, MdibStorage storage) {
        final Optional<AbstractMultiState> multiState = mdibTypeValidator.toMultiState(state);
        if (multiState.isPresent()) {
            getVersionPair(multiState.get()).ifPresent(versionPair -> {
                multiState.get().setStateVersion(versionPair.getStateVersion().add(BigInteger.ONE));
                multiState.get().setDescriptorVersion(versionPair.getDescriptorVersion());
                putVersionPair(multiState.get());
            });
        } else {
            getVersionPair(state.getDescriptorHandle()).ifPresent(versionPair -> {
                state.setStateVersion(versionPair.getStateVersion().add(BigInteger.ONE));
                state.setDescriptorVersion(versionPair.getDescriptorVersion());
                putVersionPair(versionPair, state);
            });
        }
    }

    private void processUpdate(AbstractDescriptor descriptor,
                               List<? extends AbstractState> states,
                               MdibStorage storage) {
        if (isUpdatedAlready(descriptor)) {
            return;
        }

        if (mdibTypeValidator.isMultiStateDescriptor(descriptor)) {
            processUpdateWithMultiState(descriptor, states, storage);
        } else {
            if (states.isEmpty()) {
                throw new RuntimeException("State is missing to complete the insert operation.");
            }
            processUpdateWithSingleState(descriptor, states.get(0));
        }

        setUpdated(descriptor);
    }

    private void processUpdateWithMultiState(AbstractDescriptor descriptor,
                                             List<? extends AbstractState> states,
                                             MdibStorage storage) {
        final VersionPair versionPair = getVersionPair(descriptor).orElseThrow(() ->
                new RuntimeException("Expected existing version on update, but none found"));

        descriptor.setDescriptorVersion(versionPair.getDescriptorVersion().add(BigInteger.ONE));
        putVersionPair(descriptor);

        final Map<String, AbstractMultiState> multiStatesFromStorage = storage.getMultiStates(descriptor.getHandle())
                .stream().collect(Collectors.toMap(o -> o.getHandle(), o -> o));

        Consumer<AbstractMultiState> replaceVersions = (multiState) -> {
            final VersionPair stateVersionPair = getVersionPair(multiState).orElse(new VersionPair());
            multiState.setDescriptorVersion(descriptor.getDescriptorVersion());
            multiState.setStateVersion(stateVersionPair.getStateVersion().add(BigInteger.ONE));
            putVersionPair(multiState);
        };

        for (AbstractState state : states) {
            final AbstractMultiState multiState = mdibTypeValidator.toMultiState(state)
                    .orElseThrow(() -> new RuntimeException("Expected multi state, but single state found"));
            replaceVersions.accept(multiState);
            multiStatesFromStorage.remove(multiState.getHandle());
        }

        for (Map.Entry<String, AbstractMultiState> multiState : multiStatesFromStorage.entrySet()) {
            replaceVersions.accept(multiState.getValue());
        }
    }

    private void processUpdateWithSingleState(AbstractDescriptor descriptor, AbstractState state) {
        final VersionPair versionPair = getVersionPair(descriptor).orElseThrow(() ->
                new RuntimeException("Expected existing version on update, but none found"));
        descriptor.setDescriptorVersion(versionPair.getDescriptorVersion().add(BigInteger.ONE));
        state.setDescriptorVersion(descriptor.getDescriptorVersion());
        state.setStateVersion(versionPair.getStateVersion().add(BigInteger.ONE));
        putVersionPair(descriptor, state);
    }

    private void processInsert(MdibDescriptionModifications modifications, AbstractDescriptor descriptor,
                               List<? extends AbstractState> states,
                               Optional<String> parentHandle,
                               MdibStorage storage) {
        if (mdibTypeValidator.isMultiStateDescriptor(descriptor)) {
            processInsertWithMultiState(descriptor, states);
        } else {
            if (states.isEmpty()) {
                throw new RuntimeException("State is missing to complete the insert operation.");
            }
            processInsertWithSingleState(descriptor, states.get(0));
        }

        setUpdated(descriptor);
    }

    private void processInsertWithSingleState(AbstractDescriptor descriptor, AbstractState state) {
        final VersionPair versionPair = getVersionPair(descriptor).orElse(new VersionPair());
        if (!isUpdatedAlready(descriptor)) {
            descriptor.setDescriptorVersion(versionPair.getDescriptorVersion().add(BigInteger.ONE));
        }
        state.setDescriptorVersion(descriptor.getDescriptorVersion());
        state.setStateVersion(versionPair.getStateVersion().add(BigInteger.ONE));
        putVersionPair(descriptor, state);
    }

    private void processInsertWithMultiState(AbstractDescriptor descriptor, List<? extends AbstractState> states) {
        if (!isUpdatedAlready(descriptor)) {
            final VersionPair versionPair = getVersionPair(descriptor).orElse(new VersionPair());
            descriptor.setDescriptorVersion(versionPair.getDescriptorVersion().add(BigInteger.ONE));
            putVersionPair(descriptor);
        }

        for (AbstractState state : states) {
            final AbstractMultiState multiState = mdibTypeValidator.toMultiState(state)
                    .orElseThrow(() -> new RuntimeException("Expected multi state, but single state found"));
            final VersionPair stateVersionPair = getVersionPair(multiState).orElse(new VersionPair());
            multiState.setDescriptorVersion(descriptor.getDescriptorVersion());
            multiState.setStateVersion(stateVersionPair.getStateVersion().add(BigInteger.ONE));
            putVersionPair(multiState);
        }
    }

    void incrementParent(String parentHandle, MdibDescriptionModifications modifications, MdibStorage storage) {
        if (!modifications.isAddedAsInserted(parentHandle) && !modifications.isAddedAsUpdated(parentHandle)) {
            final MdibEntity entity = storage.getEntity(parentHandle)
                    .orElseThrow(() -> new RuntimeException(String.format("Parent handle not found: %s", parentHandle)));

            // Queue in update if not in modifications already
            final AbstractDescriptor descriptorCopy = objectUtil.deepCopy(entity.getDescriptor());
            entity.doIfSingleState(state -> modifications.update(descriptorCopy, objectUtil.deepCopy(state)))
                    .orElse(states -> modifications.update(descriptorCopy, objectUtil.deepCopy(states)));
        }
    }

    boolean isUpdatedAlready(AbstractDescriptor descriptor) {
        return updatedParents.contains(descriptor.getHandle());
    }

    void setUpdated(AbstractDescriptor descriptor) {
        updatedParents.add(descriptor.getHandle());
    }

    private void putVersionPair(VersionPair versionPair, AbstractState state) {
        versionCache.put(state.getDescriptorHandle(), new VersionPair(versionPair.getDescriptorVersion(), state.getStateVersion()));
    }

    private void putVersionPair(AbstractDescriptor descriptor) {
        versionCache.put(descriptor.getHandle(), new VersionPair(descriptor.getDescriptorVersion()));
    }

    private void putVersionPair(AbstractDescriptor descriptor, AbstractState state) {
        versionCache.put(descriptor.getHandle(), new VersionPair(descriptor.getDescriptorVersion(), state.getStateVersion()));
    }

    private void putVersionPair(AbstractMultiState state) {
        versionCache.put(state.getHandle(), new VersionPair(state.getDescriptorVersion(), state.getStateVersion()));
    }

    private Optional<VersionPair> getVersionPair(String handle) {
        return Optional.ofNullable(versionCache.get(handle));
    }

    private Optional<VersionPair> getVersionPair(AbstractDescriptor descriptor) {
        return Optional.ofNullable(versionCache.get(descriptor.getHandle()));
    }

    private Optional<VersionPair> getVersionPair(AbstractMultiState state) {
        return Optional.ofNullable(versionCache.get(state.getHandle()));
    }

    private class VersionPair {
        private final BigInteger descriptorVersion;
        private final BigInteger stateVersion;

        VersionPair() {
            descriptorVersion = BigInteger.valueOf(-1);
            stateVersion = BigInteger.valueOf(-1);
        }

        VersionPair(BigInteger descriptorVersion, BigInteger stateVersion) {
            this.descriptorVersion = descriptorVersion;
            this.stateVersion = stateVersion;
        }

        VersionPair(BigInteger descriptorVersion) {
            this.descriptorVersion = descriptorVersion;
            this.stateVersion = BigInteger.ZERO;
        }

        public BigInteger getDescriptorVersion() {
            return descriptorVersion;
        }

        public BigInteger getStateVersion() {
            return stateVersion;
        }
    }
}
