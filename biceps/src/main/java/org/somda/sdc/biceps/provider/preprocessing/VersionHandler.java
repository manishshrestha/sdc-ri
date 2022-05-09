package org.somda.sdc.biceps.provider.preprocessing;

import com.google.inject.Inject;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.common.MdibDescriptionModification;
import org.somda.sdc.biceps.common.MdibEntity;
import org.somda.sdc.biceps.common.MdibStateModifications;
import org.somda.sdc.biceps.common.MdibTypeValidator;
import org.somda.sdc.biceps.common.storage.DescriptionPreprocessingSegment;
import org.somda.sdc.biceps.common.storage.MdibStorage;
import org.somda.sdc.biceps.common.storage.StatePreprocessingSegment;
import org.somda.sdc.biceps.model.participant.AbstractContextDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractContextState;
import org.somda.sdc.biceps.model.participant.AbstractDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractMultiState;
import org.somda.sdc.biceps.model.participant.AbstractState;
import org.somda.sdc.biceps.model.participant.MdsDescriptor;
import org.somda.sdc.biceps.provider.preprocessing.helper.Version;
import org.somda.sdc.biceps.provider.preprocessing.helper.VersionPair;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
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
    private static final Logger LOG = LogManager.getLogger(CardinalityChecker.class);

    private final MdibTypeValidator mdibTypeValidator;

    private final Map<String, VersionPair> versionsOfDeletedArtifacts;
    private final Map<String, VersionPair> updatedVersions;
    private final Set<String> updatedParents;

    @Inject
    VersionHandler(MdibTypeValidator mdibTypeValidator) {
        this.mdibTypeValidator = mdibTypeValidator;
        this.versionsOfDeletedArtifacts = new HashMap<>();
        this.updatedVersions = new HashMap<>();
        this.updatedParents = new HashSet<>();
    }

    @Override
    public List<MdibDescriptionModification> beforeFirstModification(
        List<MdibDescriptionModification> modifications, MdibStorage storage
    ) {
        updatedParents.clear();
        updatedVersions.clear();
        return modifications;
    }

    @Override
    public void beforeFirstModification(MdibStateModifications modifications, MdibStorage storage) {
        updatedParents.clear();
        updatedVersions.clear();
    }

    @Override
    public List<MdibDescriptionModification> process(List<MdibDescriptionModification> modifications,
                                                MdibStorage storage) throws VersioningException {
        try {
            return modifications.stream().map(modification -> {
                switch (modification.getModificationType()) {
                    case INSERT:
                        return processInsert(
                            modification.getDescriptor(),
                            modification.getStates(),
                            modification.getParentHandle().orElse(null),
                            storage
                        );
                    case UPDATE:
                        return processUpdate(
                            modification.getDescriptor(), modification.getStates(),
                            modification.getParentHandle().orElse(null), storage
                        );
                    case DELETE:
                        return processDelete(modification.getDescriptor(), storage);
                    default:
                        throw new VersioningExceptionInner(new VersioningException(String.format(
                            "Default clause reached in process, modification %s was unknown",
                            modification.getModificationType()
                        )));
                }
            }).flatMap(Collection::stream)
                .collect(Collectors.toList());
        } catch (VersioningExceptionInner e) {
            throw (VersioningException) e.getCause();
        }
    }

    @Override
    public void process(MdibStateModifications modifications, MdibStorage storage)
            throws VersioningException {

        try {
            var updated = modifications.getStates().stream().map(state -> {

                final Optional<AbstractMultiState> multiState = mdibTypeValidator.toMultiState(state);
                if (multiState.isPresent()) {
                    VersionPair versionPair = getSavedVersionPair(multiState.get().getHandle())
                        .or(() -> getVersionPairContext(multiState.get().getHandle(), storage))
                        // if neither a storage version, deleted entity version or saved version is present,
                        // this state is new
                        .orElseGet(() -> {
                            LOG.debug(
                                "Encountered unknown context state {}, must be new, assigning empty state version",
                                multiState.get().getHandle()
                            );
                            return new VersionPair();
                        });

                    return multiState.get().newCopyBuilder()
                        .withStateVersion(versionPair.incrementStateVersion())
                        .withDescriptorVersion(versionPair.getDescriptorVersion())
                        .build();
                } else {
                    var pair = getSavedVersionPair(state.getDescriptorHandle())
                        .or(() -> getVersionPair(state.getDescriptorHandle(), storage))
                        .orElseThrow(() -> new VersioningExceptionInner(
                            new VersioningException(
                                "Could not determine the previous or saved version for state "
                                    + state.getDescriptorHandle()
                            )
                        ));
                    return state.newCopyBuilder()
                        .withStateVersion(pair.incrementStateVersion())
                        .withDescriptorVersion(pair.getDescriptorVersion())
                        .build();
                }
            }).collect(Collectors.toList());

            modifications.getStates().clear();
            modifications.getStates().addAll(updated);
        } catch (VersioningExceptionInner e) {
            throw (VersioningException) e.getCause();
        }
    }

    private List<MdibDescriptionModification> processDelete(AbstractDescriptor descriptor, MdibStorage storage)
        throws VersioningExceptionInner {
        var entityToDelete = storage.getEntity(descriptor.getHandle())
            .orElseThrow(() -> new VersioningExceptionInner(
                new VersioningException("Delete received not present handle " + descriptor.getHandle())
            ));

        setVersionOfDeletedArtifacts(
            entityToDelete.getHandle(),
            new VersionPair(entityToDelete)
        );

        entityToDelete.doIfMultiState(states -> states.forEach(state ->
            setVersionOfDeletedArtifacts(state.getHandle(), new VersionPair(state))
        ));

        var deleteModification = new MdibDescriptionModification(
            MdibDescriptionModification.Type.DELETE, descriptor,
            Collections.emptyList(), entityToDelete.getParent().orElse(null)
        );

        // no parent to update in case of an MDS
        if (entityToDelete.getDescriptorClass() == MdsDescriptor.class) {
            return List.of(
                deleteModification
            );
        }

        // else increment parent version on delete
        var parentHandle = entityToDelete.getParent()
            .orElseThrow(() -> new VersioningExceptionInner(
                new VersioningException("MDIB storage inconsistency: parent handle is missing")
            ));

        MdibEntity parentEntity = storage.getEntity(parentHandle).orElseThrow(() ->
            new VersioningExceptionInner(
                new VersioningException("MDIB storage inconsistency: parent entity is missing"))
        );

        var updates = new ArrayList<>(processUpdate(
            parentEntity.getDescriptor(), parentEntity.getStates(), parentEntity.getParent().orElse(null),
            storage
        ));
        updates.add(0, deleteModification);
        return updates;
    }

    private List<MdibDescriptionModification> processUpdate(AbstractDescriptor descriptor,
                               List<AbstractState> states,
                               @Nullable String parentHandle,
                               MdibStorage storage) throws VersioningExceptionInner {
        if (isUpdatedAlready(descriptor)) {
            LOG.debug(
                "processUpdate: updated versions already includes {}, rewriting versions and returning",
                descriptor.getHandle()
            );
            Pair<AbstractDescriptor, List<AbstractState>> updated;
            if (mdibTypeValidator.isMultiStateDescriptor(descriptor)) {
                updated = processUpdateAlreadyUpdatedMultiState(descriptor, states, storage);
            } else {
                updated = processUpdateAlreadyUpdatedSingleState(descriptor, states);
            }

            setUpdated(updated.getLeft(), updated.getRight());
            return List.of(new MdibDescriptionModification(
                MdibDescriptionModification.Type.UPDATE,
                updated.getLeft(), updated.getRight(),
                parentHandle
            ));
        }

        Pair<AbstractDescriptor, List<AbstractState>> updated;
        if (mdibTypeValidator.isMultiStateDescriptor(descriptor)) {
            updated = processUpdateWithMultiState(descriptor, states, storage);
        } else {
            if (states.isEmpty()) {
                Optional<AbstractState> state = storage.getState(descriptor.getHandle());
                if (state.isEmpty()) {
                    throw new VersioningExceptionInner(
                        new VersioningException("State is missing to complete the update operation")
                    );
                } else {
                    states.add(state.get());
                }
            }
            if (states.size() != 1) {
                throw new VersioningExceptionInner(
                    new VersioningException("Single state case received more than one state")
                );
            }
            updated = processUpdateWithSingleState(descriptor, states.get(0), storage);
        }

        setUpdated(updated.getLeft(), updated.getRight());
        return List.of(new MdibDescriptionModification(
            MdibDescriptionModification.Type.UPDATE, updated.getLeft(), updated.getRight(), parentHandle
        ));
    }

    private Pair<AbstractDescriptor, List<AbstractState>> processUpdateAlreadyUpdatedMultiState(
        AbstractDescriptor descriptor, List<AbstractState> states, MdibStorage storage
    ) throws VersioningExceptionInner {
        var newVersion = getSavedVersionPair(descriptor.getHandle())
            .orElseThrow(() -> new VersioningExceptionInner(
                new VersioningException("Already updated multi-state app does not have a known version")
            ));

        var updatedDescriptor = descriptor.newCopyBuilder()
            .withDescriptorVersion(newVersion.getDescriptorVersion())
            .build();

        var updatesStates = states.stream()
            .map(state -> updateContextState(descriptor.getDescriptorVersion(), state, storage))
            .collect(Collectors.toList());

        return new ImmutablePair<>(updatedDescriptor, updatesStates);
    }

    private AbstractState updateContextState(
        BigInteger descriptorVersion, AbstractState state, MdibStorage storage
    ) throws VersioningExceptionInner {
        if (!(state instanceof AbstractContextState)) {
            throw new VersioningExceptionInner(
                new VersioningException("updateContextState received a non-context state")
            );
        }
        var contextState = (AbstractContextState) state;

        var stateVersion = getSavedVersionState(contextState.getHandle())
            .or(() -> getVersionContextState(contextState.getHandle(), storage))
            .orElseGet(Version::new);

        return contextState.newCopyBuilder()
            .withDescriptorVersion(descriptorVersion)
            .withStateVersion(stateVersion.getVersion())
            .build();
    }

    private Pair<AbstractDescriptor, List<AbstractState>> processUpdateAlreadyUpdatedSingleState(
        AbstractDescriptor descriptor, List<AbstractState> states
    ) throws VersioningExceptionInner {

        if (states.size() > 1) {
            throw new VersioningExceptionInner(new VersioningException("Cardinality Error"));
        }

        var newVersion = getSavedVersionPair(descriptor.getHandle())
            .orElseThrow(() -> new VersioningExceptionInner(
                new VersioningException("Expected existing version on update, but none found")
            ));
        var updatedDescriptor = descriptor.newCopyBuilder()
            .withDescriptorVersion(newVersion.getDescriptorVersion())
            .build();

        var updatedState = states.get(0).newCopyBuilder()
            .withDescriptorVersion(newVersion.getDescriptorVersion())
            .withStateVersion(newVersion.getStateVersion()).build();

        return new ImmutablePair<>(updatedDescriptor, List.of(updatedState));
    }

    private Pair<AbstractDescriptor, List<AbstractState>> processUpdateWithMultiState(AbstractDescriptor descriptor,
                                             List<AbstractState> states,
                                             MdibStorage storage) throws VersioningExceptionInner {
        final Version newDescriptorVersion = getVersionDescriptor(descriptor.getHandle(), storage)
            .orElseThrow(() ->
                new VersioningExceptionInner(
                    new VersioningException("Expected existing version on update, but none found")
                ));

        var updatedDescriptor = descriptor.newCopyBuilder()
            .withDescriptorVersion(newDescriptorVersion.increment().getVersion())
            .build();

        final Map<String, AbstractMultiState> multiStatesFromStorage = storage
            .getMultiStates(updatedDescriptor.getHandle()).stream()
            .collect(Collectors.toMap(AbstractMultiState::getHandle, o -> o));

        Function<AbstractMultiState, AbstractMultiState> replaceVersions = multiState -> {
            final Version stateVersionPair = getVersionContextState(multiState.getHandle(), storage)
                .orElseGet(Version::new);
            return multiState.newCopyBuilder()
                    .withDescriptorVersion(updatedDescriptor.getDescriptorVersion())
                    .withStateVersion(stateVersionPair.increment().getVersion())
                    .build();
        };

        // increment versions for all states from change set
        List<AbstractState> updatedStates = states.stream().map(state -> {
            final AbstractMultiState multiState = mdibTypeValidator.toMultiState(state)
                .orElseThrow(() ->
                    new VersioningExceptionInner(
                        new VersioningException("Expected multi state, but single state found")
                    )
                );
            var updatedState = replaceVersions.apply(multiState);
            multiStatesFromStorage.remove(multiState.getHandle());
            return updatedState;
        }).collect(Collectors.toList());

        // increment versions from states in MDIB storage that are not in change set
        for (Map.Entry<String, AbstractMultiState> multiState : multiStatesFromStorage.entrySet()) {
            var updatedState = replaceVersions.apply(multiState.getValue());
            updatedStates.add(updatedState);
        }

        return new ImmutablePair<>(updatedDescriptor, updatedStates);
    }

    private Pair<AbstractDescriptor, List<AbstractState>> processUpdateWithSingleState(
        AbstractDescriptor descriptor, AbstractState state, MdibStorage storage
    ) throws VersioningExceptionInner {
        final VersionPair versionPair = getVersionPair(descriptor.getHandle(), storage).orElseThrow(() ->
                new VersioningExceptionInner(
                    new VersioningException("Expected existing version on update, but none found")
                ));

        var updatedDescriptor = descriptor.newCopyBuilder()
            .withDescriptorVersion(versionPair.getDescriptorVersion().add(BigInteger.ONE)).build();


        var updatedState = state.newCopyBuilder()
            .withDescriptorVersion(updatedDescriptor.getDescriptorVersion())
            .withStateVersion(versionPair.incrementStateVersion())
            .build();

        return new ImmutablePair<>(updatedDescriptor, List.of(updatedState));
    }

    private List<MdibDescriptionModification> processInsert(
        AbstractDescriptor descriptor,
        List<? extends AbstractState> states,
        @Nullable String parentHandle,
        MdibStorage storage
    ) throws VersioningExceptionInner {
        Pair<AbstractDescriptor, List<AbstractState>> inserted;
        if (mdibTypeValidator.isMultiStateDescriptor(descriptor)) {
            inserted = processInsertWithMultiState(descriptor, states, storage);
        } else {
            if (states.isEmpty()) {
                throw new VersioningExceptionInner(
                    new VersioningException("State is missing to complete the insert operation")
                );
            }
            inserted = processInsertWithSingleState(descriptor, states.get(0), storage);
        }
        setUpdated(inserted.getLeft(), inserted.getRight());

        var insertMod = new MdibDescriptionModification(
            MdibDescriptionModification.Type.INSERT, inserted.getLeft(), inserted.getRight(), parentHandle
        );

        if (parentHandle != null) {
            Optional<MdibEntity> entity = storage.getEntity(parentHandle);
            if (entity.isEmpty()) {
                if (!updatedParents.contains(parentHandle)) {
                    throw new VersioningExceptionInner(
                        new VersioningException("Missing parent to be inserted before child")
                    );
                }
            } else {
                var updateMod = new ArrayList<>(processUpdate(
                    entity.get().getDescriptor(), entity.get().getStates(),
                    entity.get().getParent().orElse(null), storage
                ));
                updateMod.add(0, insertMod);
                return updateMod;
            }
        }
        return List.of(insertMod);
    }

    private Pair<AbstractDescriptor, List<AbstractState>> processInsertWithSingleState(
        AbstractDescriptor descriptor, AbstractState state, MdibStorage storage
    ) {
        final VersionPair versionPair = getSavedVersionPair(descriptor.getHandle())
            .or(() -> getVersionPair(descriptor.getHandle(), storage))
            .orElseGet(VersionPair::new);

        var updatedDescriptor = descriptor.newCopyBuilder()
            .withDescriptorVersion(versionPair.incrementDescriptorVersion())
            .build();

        var updatedState = state.newCopyBuilder()
            .withDescriptorVersion(updatedDescriptor.getDescriptorVersion())
            .withStateVersion(versionPair.incrementStateVersion())
            .build();

        return new ImmutablePair<>(updatedDescriptor, List.of(updatedState));
    }

    private Pair<AbstractDescriptor, List<AbstractState>> processInsertWithMultiState(
        AbstractDescriptor descriptor, List<? extends AbstractState> states, MdibStorage storage
    ) throws VersioningExceptionInner {
//        final VersionPair versionPair = getVersionPair(descriptor).orElse(new VersionPair());

        final Version descriptorVersion = getVersionDescriptor(descriptor.getHandle(), storage)
            .orElseGet(Version::new);

        var updatedDescriptor = descriptor.newCopyBuilder()
            .withDescriptorVersion(descriptorVersion.increment().getVersion())
            .build();

        List<AbstractState> updatedStates = states.stream().map(it -> {
            final AbstractMultiState multiState = mdibTypeValidator.toMultiState(it)
                .orElseThrow(() -> new VersioningExceptionInner(
                    new VersioningException("Expected multi-state, but single state found")
                ));
            final Version stateVersion = getSavedVersionState(multiState.getHandle())
                .or(() -> getVersionContextState(multiState.getHandle(), storage))
                .orElseGet(Version::new);

            return multiState.newCopyBuilder()
                .withDescriptorVersion(updatedDescriptor.getDescriptorVersion())
                .withStateVersion(stateVersion.increment().getVersion())
                .build();
        }).collect(Collectors.toList());

        return new ImmutablePair<>(updatedDescriptor, updatedStates);
    }

    private boolean isUpdatedAlready(AbstractDescriptor descriptor) {
        return updatedParents.contains(descriptor.getHandle());
    }

    private void setUpdated(AbstractDescriptor descriptor, List<AbstractState> states) {
        updatedParents.add(descriptor.getHandle());

        if (descriptor instanceof AbstractContextDescriptor) {
            updatedVersions.put(descriptor.getHandle(), new VersionPair(descriptor.getDescriptorVersion()));
            states.stream()
                .map(it -> (AbstractContextState) it)
                .forEach(state -> updatedVersions.put(state.getHandle(), new VersionPair(state)));
        } else {
            var state = states.get(0);

            updatedVersions.put(descriptor.getHandle(), new VersionPair(
                descriptor.getDescriptorVersion(),
                state.getStateVersion()
            ));
        }
    }

    private Optional<Version> getVersionDescriptor(String handle, MdibStorage storage) {
        return storage.getEntity(handle).map(Version::new)
            .or(() -> getVersionOfDeletedArtifacts(handle)
            .map(it -> new Version(it.getDescriptorVersion())));
    }

    private Optional<Version> getSavedVersionDescriptor(String handle) {
        return Optional.ofNullable(updatedVersions.get(handle))
            .map(VersionPair::getDescriptorVersion)
            .map(Version::new);
    }

    private Optional<Version> getVersionState(String handle, MdibStorage storage) {
        return storage.getEntity(handle)
            .map(VersionPair::new)
            .map(VersionPair::getStateVersion)
            .map(Version::new)
            .or(() -> getVersionOfDeletedArtifacts(handle)
                .map(it -> new Version(it.getDescriptorVersion())));
    }

    private Optional<Version> getVersionContextState(String handle, MdibStorage storage) {
        return storage.getContextState(handle)
            .map(Version::new)
            .or(() -> getVersionOfDeletedArtifacts(handle).map(it -> new Version(it.getStateVersion())));
    }

    private Optional<Version> getSavedVersionState(String handle) {
        return Optional.ofNullable(updatedVersions.get(handle))
            .map(VersionPair::getStateVersion)
            .map(Version::new);
    }

    private Optional<VersionPair> getVersionPair(String handle, MdibStorage storage) {
        return storage.getEntity(handle)
            .map(VersionPair::new)
            .or(() -> getVersionOfDeletedArtifacts(handle));
    }

    private Optional<VersionPair> getVersionPairContext(String handle, MdibStorage storage) {
        return storage.getContextState(handle)
            .map(VersionPair::new);
    }

    private Optional<VersionPair> getSavedVersionPair(String handle) {
        return Optional.ofNullable(updatedVersions.get(handle));
    }

    private Optional<VersionPair> getVersionOfDeletedArtifacts(String handle) {
        return Optional.ofNullable(versionsOfDeletedArtifacts.get(handle));
    }

    /**
     * Stores the version for a handle on deletion.
     *
     * @param handle to store
     * @param version to store
     */
    private void setVersionOfDeletedArtifacts(String handle, VersionPair version) {
        versionsOfDeletedArtifacts.put(handle, version);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    Map<String, VersionPair> getUpdatedVersions() {
        return updatedVersions;
    }
}
