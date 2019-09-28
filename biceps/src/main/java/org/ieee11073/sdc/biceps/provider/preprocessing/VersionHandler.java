package org.ieee11073.sdc.biceps.provider.preprocessing;

import com.google.inject.Inject;
import org.ieee11073.sdc.biceps.common.*;
import org.ieee11073.sdc.biceps.common.factory.MdibEntityFactory;
import org.ieee11073.sdc.biceps.common.storage.DescriptionPreprocessingSegment;
import org.ieee11073.sdc.biceps.common.storage.MdibStorage;
import org.ieee11073.sdc.biceps.common.storage.StatePreprocessingSegment;
import org.ieee11073.sdc.biceps.model.participant.AbstractDescriptor;
import org.ieee11073.sdc.biceps.model.participant.AbstractMultiState;
import org.ieee11073.sdc.biceps.model.participant.AbstractState;
import org.ieee11073.sdc.biceps.model.participant.MdsDescriptor;
import org.ieee11073.sdc.common.helper.ObjectUtil;

import java.math.BigInteger;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Preprocessing segment that manages BICEPS versioning.
 * <p>
 * The segment takes care of incrementing descriptor and state versions.
 * It maintains a map of latest versions independent of the data that is stored in the {@link MdibStorage}.
 * This facilitates the alignment of versions without prematurely storing them in the {@link MdibStorage}.
 * <p>
 * If an error is thrown during processing, the versions stored internally in the version handler are not going to be
 * tainted.
 */
public class VersionHandler implements DescriptionPreprocessingSegment, StatePreprocessingSegment {
    private final MdibTypeValidator mdibTypeValidator;
    private final ObjectUtil objectUtil;

    private Map<String, VersionPair> versionsWorkingCopy;
    private Map<String, VersionPair> versions;
    private final Set<String> updatedParents;

    @Inject
    VersionHandler(MdibTypeValidator mdibTypeValidator,
                   ObjectUtil objectUtil) {
        this.mdibTypeValidator = mdibTypeValidator;
        this.objectUtil = objectUtil;
        this.versionsWorkingCopy = new HashMap<>();
        this.versions = new HashMap<>();
        this.updatedParents = new HashSet<>();
    }

    @Override
    public void beforeFirstModification(MdibStorage storage) {
        updatedParents.clear();
        versionsWorkingCopy = objectUtil.deepCopy(versions);
    }

    @Override
    public void afterLastModification(MdibStorage storage) {
        versions = versionsWorkingCopy;
    }

    @Override
    public void process(MdibDescriptionModifications modifications,
                        MdibDescriptionModification modification,
                        MdibStorage storage) throws VersioningException {
        switch (modification.getModificationType()) {
            case INSERT:
                processInsert(modifications, modification.getDescriptor(), modification.getStates(), modification.getParentHandle(), storage);
                break;
            case UPDATE:
                processUpdate(modification.getDescriptor(), modification.getStates(), storage);
                break;
            case DELETE:
                processDelete(modification.getDescriptor(), storage);
                break;
        }
    }

    @Override
    public void process(AbstractState state, MdibStorage storage) throws VersioningException {
        final Optional<AbstractMultiState> multiState = mdibTypeValidator.toMultiState(state);
        if (multiState.isPresent()) {
            VersionPair versionPair;
            Optional<VersionPair> multiStateVersionPair = getVersionPair(multiState.get());
            if (multiStateVersionPair.isPresent()) {
                versionPair = multiStateVersionPair.get();
            } else {
                VersionPair versionPairFromDescr = getVersionPair(multiState.get().getDescriptorHandle()).orElseThrow(() ->
                        new VersioningException("Multi-state descriptor was missing during multi state update"));
                versionPair = new VersionPair(versionPairFromDescr.getDescriptorVersion(), BigInteger.valueOf(-1));
            }

            multiState.get().setStateVersion(versionPair.getStateVersion().add(BigInteger.ONE));
            multiState.get().setDescriptorVersion(versionPair.getDescriptorVersion());
            putVersionPair(multiState.get());
        } else {
            getVersionPair(state.getDescriptorHandle()).ifPresent(versionPair -> {
                state.setStateVersion(versionPair.getStateVersion().add(BigInteger.ONE));
                state.setDescriptorVersion(versionPair.getDescriptorVersion());
                putVersionPair(versionPair, state);
            });
        }
    }

    private void processDelete(AbstractDescriptor descriptor, MdibStorage storage) throws VersioningException {
        if (descriptor instanceof MdsDescriptor) {
            // no child to update in case of an MDS
            return;
        }

        Optional<MdibEntity> entity = storage.getEntity(descriptor.getHandle());
        if (entity.isEmpty()) {
            throw new VersioningException("Deletion of an entity requires an existing entity in the MDIB storage");
        }

        String parentHandle = entity.get().getParent().orElseThrow(() ->
                new VersioningException("MDIB storage inconsistency: parent handle is missing"));

        MdibEntity parentEntity = storage.getEntity(parentHandle).orElseThrow(() ->
                new VersioningException("MDIB storage inconsistency: parent entity is missing"));

        processUpdate(parentEntity.getDescriptor(), parentEntity.getStates(), storage);
    }

    private void processUpdate(AbstractDescriptor descriptor,
                               List<? extends AbstractState> states,
                               MdibStorage storage) throws VersioningException {
        if (isUpdatedAlready(descriptor)) {
            return; // state has been updated already, ignore
        }

        if (mdibTypeValidator.isMultiStateDescriptor(descriptor)) {
            processUpdateWithMultiState(descriptor, states, storage);
        } else {
            if (states.isEmpty()) {
                Optional<AbstractState> state = storage.getState(descriptor.getHandle());
                if (state.isEmpty()) {
                    throw new VersioningException("State is missing to complete the update operation");
                } else {
                    states = Collections.singletonList(state.get());
                }
            }
            processUpdateWithSingleState(descriptor, states.get(0));
        }

        setUpdated(descriptor);
    }

    private void processUpdateWithMultiState(AbstractDescriptor descriptor,
                                             List<? extends AbstractState> states,
                                             MdibStorage storage) throws VersioningException {
        final VersionPair versionPair = getVersionPair(descriptor).orElseThrow(() ->
                new VersioningException("Expected existing version on update, but none found"));

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
                    .orElseThrow(() -> new VersioningException("Expected multi state, but single state found"));
            replaceVersions.accept(multiState);
            multiStatesFromStorage.remove(multiState.getHandle());
        }

        for (Map.Entry<String, AbstractMultiState> multiState : multiStatesFromStorage.entrySet()) {
            replaceVersions.accept(multiState.getValue());
        }
    }

    private void processUpdateWithSingleState(AbstractDescriptor descriptor, AbstractState state) throws VersioningException {
        final VersionPair versionPair = getVersionPair(descriptor).orElseThrow(() ->
                new VersioningException("Expected existing version on update, but none found"));
        descriptor.setDescriptorVersion(versionPair.getDescriptorVersion().add(BigInteger.ONE));
        state.setDescriptorVersion(descriptor.getDescriptorVersion());
        state.setStateVersion(versionPair.getStateVersion().add(BigInteger.ONE));
        putVersionPair(descriptor, state);
    }

    private void processInsert(MdibDescriptionModifications modifications, AbstractDescriptor descriptor,
                               List<? extends AbstractState> states,
                               Optional<String> parentHandle,
                               MdibStorage storage) throws VersioningException {
        if (mdibTypeValidator.isMultiStateDescriptor(descriptor)) {
            processInsertWithMultiState(descriptor, states);
        } else {
            if (states.isEmpty()) {
                throw new VersioningException("State is missing to complete the insert operation");
            }
            processInsertWithSingleState(descriptor, states.get(0));
        }

        setUpdated(descriptor);

        if (parentHandle.isPresent()) {
            Optional<MdibEntity> entity = storage.getEntity(parentHandle.get());
            if (entity.isEmpty()) {
                if (!updatedParents.contains(parentHandle.get())) {
                    throw new VersioningException("Missing parent to be inserted before child");
                }
            } else {
                processUpdate(entity.get().getDescriptor(), entity.get().getStates(), storage);
            }
        }
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

    private void processInsertWithMultiState(AbstractDescriptor descriptor, List<? extends AbstractState> states) throws VersioningException {
        if (!isUpdatedAlready(descriptor)) {
            final VersionPair versionPair = getVersionPair(descriptor).orElse(new VersionPair());
            descriptor.setDescriptorVersion(versionPair.getDescriptorVersion().add(BigInteger.ONE));
            putVersionPair(descriptor);
        }

        for (AbstractState state : states) {
            final AbstractMultiState multiState = mdibTypeValidator.toMultiState(state)
                    .orElseThrow(() -> new VersioningException("Expected multi state, but single state found"));
            final VersionPair stateVersionPair = getVersionPair(multiState).orElse(new VersionPair());
            multiState.setDescriptorVersion(descriptor.getDescriptorVersion());
            multiState.setStateVersion(stateVersionPair.getStateVersion().add(BigInteger.ONE));
            putVersionPair(multiState);
        }
    }

    void incrementParent(String parentHandle, MdibDescriptionModifications modifications, MdibStorage storage) throws VersioningException {
        if (!modifications.isAddedAsInserted(parentHandle) && !modifications.isAddedAsUpdated(parentHandle)) {
            final MdibEntity entity = storage.getEntity(parentHandle)
                    .orElseThrow(() -> new VersioningException(String.format("Parent handle not found: %s", parentHandle)));

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
        versionsWorkingCopy.put(state.getDescriptorHandle(), new VersionPair(versionPair.getDescriptorVersion(), state.getStateVersion()));
    }

    private void putVersionPair(AbstractDescriptor descriptor) {
        versionsWorkingCopy.put(descriptor.getHandle(), new VersionPair(descriptor.getDescriptorVersion()));
    }

    private void putVersionPair(AbstractDescriptor descriptor, AbstractState state) {
        versionsWorkingCopy.put(descriptor.getHandle(), new VersionPair(descriptor.getDescriptorVersion(), state.getStateVersion()));
    }

    private void putVersionPair(AbstractMultiState state) {
        versionsWorkingCopy.put(state.getHandle(), new VersionPair(state.getDescriptorVersion(), state.getStateVersion()));
    }

    private Optional<VersionPair> getVersionPair(String handle) {
        return Optional.ofNullable(versionsWorkingCopy.get(handle));
    }

    private Optional<VersionPair> getVersionPair(AbstractDescriptor descriptor) {
        return Optional.ofNullable(versionsWorkingCopy.get(descriptor.getHandle()));
    }

    private Optional<VersionPair> getVersionPair(AbstractMultiState state) {
        return Optional.ofNullable(versionsWorkingCopy.get(state.getHandle()));
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
