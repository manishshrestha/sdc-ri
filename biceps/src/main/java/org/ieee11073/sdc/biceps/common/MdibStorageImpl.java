package org.ieee11073.sdc.biceps.common;

import com.google.inject.Inject;
import org.ieee11073.sdc.biceps.common.factory.MdibEntityFactory;
import org.ieee11073.sdc.biceps.common.helper.MdibStorageUtil;
import org.ieee11073.sdc.biceps.model.participant.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class MdibStorageImpl implements MdibStorage {
    private static final Logger LOG = LoggerFactory.getLogger(MdibStorageImpl.class);

    private final MdibEntityFactory entityFactory;
    private final MdibStorageUtil util;
    private final MdibTypeValidator typeValidator;

    private Map<String, MdibEntity> entities;
    private Set<String> rootEntities;
    private Map<String, AbstractContextState> contextStates;

    @Inject
    MdibStorageImpl(MdibEntityFactory entityFactory,
                    MdibStorageUtil util,
                    MdibTypeValidator typeValidator) {

        this.entityFactory = entityFactory;
        this.util = util;
        this.typeValidator = typeValidator;

        this.entities = new HashMap<>();
        this.rootEntities = new HashSet<>();
        this.contextStates = new HashMap<>();
    }

    public <T extends AbstractDescriptor> Optional<T> getDescriptor(String handle, Class<T> descrClass) {
        final MdibEntity mdibEntity = entities.get(handle);
        if (mdibEntity != null) {
            return util.exposeInstance(mdibEntity.getDescriptor(), descrClass);
        }
        return Optional.empty();
    }

    @Override
    public Optional<AbstractDescriptor> getDescriptor(String handle) {
        return getDescriptor(handle, AbstractDescriptor.class);
    }

    public Optional<MdibEntity> getEntity(String handle) {
        return Optional.ofNullable(entities.get(handle));
    }

    public List<MdibEntity> getRootEntities() {
        return util.exposeEntityList(entities, rootEntities);
    }

    public <T extends AbstractState> Optional<T> getState(String handle, Class<T> stateClass) {
        final MdibEntity entity = entities.get(handle);
        if (entity != null && entity.getStates().size() == 1) {
            return util.exposeInstance(entity.getStates().get(0), stateClass);
        }
        final AbstractContextState contextState = contextStates.get(handle);
        return util.exposeInstance(contextState, stateClass);
    }

    @Override
    public <T extends AbstractContextState> List<T> getContextStates(String descriptorHandle, Class<T> stateClass) {
        final MdibEntity entity = entities.get(descriptorHandle);
        if (entity == null || entity.getStates().isEmpty()) {
            return Collections.emptyList();
        }
        return util.exposeListOfType(entity.getStates(), stateClass);
    }

    @Override
    public List<AbstractContextState> getContextStates(String descriptorHandle) {
        final MdibEntity entity = entities.get(descriptorHandle);
        if (entity == null || entity.getStates().isEmpty()) {
            return Collections.emptyList();
        }
        return util.exposeListOfType(entity.getStates(), AbstractContextState.class);
    }

    @Override
    public <T extends AbstractMultiState> List<T> getMultiStates(String descriptorHandle, Class<T> stateClass) {
        final MdibEntity entity = entities.get(descriptorHandle);
        if (entity == null || entity.getStates().isEmpty()) {
            return Collections.emptyList();
        }
        return util.exposeListOfType(entity.getStates(), stateClass);
    }

    @Override
    public List<AbstractMultiState> getMultiStates(String descriptorHandle) {
        final MdibEntity entity = entities.get(descriptorHandle);
        if (entity == null || entity.getStates().isEmpty()) {
            return Collections.emptyList();
        }
        return util.exposeListOfType(entity.getStates(), AbstractMultiState.class);
    }

    @Override
    public DescriptionResult apply(MdibDescriptionModifications descriptionModifications) {
        final List<MdibEntity> insertedEntities = new ArrayList<>();
        final List<MdibEntity> updatedEntities = new ArrayList<>();
        final List<String> deletedEntities = new ArrayList<>();

        for (MdibDescriptionModification modification : descriptionModifications.getModifications()) {
            switch (modification.getModificationType()) {
                case INSERT:
                    insertEntity(modification, insertedEntities);
                    break;
                case UPDATE:
                    updateEntity(modification, updatedEntities);
                    break;
                case DELETE:
                    deleteEntity(modification, deletedEntities);
                    break;
                default:
                    LOG.warn("Unknown modification type detected. Skip entry while description modification processing.");
            }
        }

        return new DescriptionResult(insertedEntities, updatedEntities, deletedEntities);
    }

    private void deleteEntity(MdibDescriptionModification modification, List<String> deletedEntities) {
        Optional.ofNullable(entities.get(modification.getHandle())).ifPresent(mdibEntity ->
                mdibEntity.getParent().ifPresent(parentHandle ->
                        Optional.ofNullable(entities.get(parentHandle)).ifPresent(parentEntity ->
                                entities.put(parentEntity.getHandle(), entityFactory.replaceChildren(parentEntity,
                                        parentEntity.getChildren().stream()
                                                .filter(s -> s.equals(mdibEntity.getHandle()))
                                                .collect(Collectors.toList()))))));

        rootEntities.remove(modification.getHandle());
        entities.remove(modification.getHandle());
        contextStates.entrySet().removeIf(state ->
                state.getValue().getDescriptorHandle().equals(modification.getHandle()));

        deletedEntities.add(modification.getHandle());
    }

    private void updateEntity(MdibDescriptionModification modification, List<MdibEntity> updatedEntities) {
        Optional.ofNullable(entities.get(modification.getHandle())).ifPresent(mdibEntity -> {
            entities.put(mdibEntity.getHandle(), entityFactory.replaceDescriptorAndStates(
                    mdibEntity,
                    modification.getDescriptor(),
                    modification.getStates()));
            updatedEntities.add(entityFactory.createShallowCopy(mdibEntity));
        });
    }

    private void insertEntity(MdibDescriptionModification modification, List<MdibEntity> insertedEntities) {
        final MdibEntity mdibEntity = entityFactory.createMdibEntity(
                null,
                new ArrayList<>(),
                modification.getDescriptor(),
                modification.getStates());

        // Either add entity as child of a parent or expect it to be a root entity
        if (modification.getParentHandle().isPresent()) {
            Optional.ofNullable(entities.get(modification.getParentHandle().get())).ifPresent(parentEntity -> {
                final List<String> children = new ArrayList<>(parentEntity.getChildren());
                children.add(mdibEntity.getHandle());
                entities.put(parentEntity.getHandle(),
                        entityFactory.replaceChildren(parentEntity, Collections.unmodifiableList(children)));
            });
        } else {
            rootEntities.add(mdibEntity.getHandle());
        }

        // Add to entities list
        entities.put(mdibEntity.getHandle(), mdibEntity);

        // Add to context states if context entity
        if (mdibEntity.getDescriptor() instanceof AbstractContextDescriptor) {
            contextStates.putAll(mdibEntity.getStates().stream()
                    .map(state -> (AbstractContextState) state)
                    .collect(Collectors.toMap(state -> state.getHandle(), state -> state)));
        }

        insertedEntities.add(mdibEntity);
    }

    @Override
    public StateResult apply(MdibStateModifications stateModifications) {
        final List<AbstractState> modifiedStates = new ArrayList<>();
        for (AbstractState modification : stateModifications.getStates()) {
            modifiedStates.add(modification);
            final MdibEntity mdibEntity = entities.get(modification.getDescriptorHandle());
            if (mdibEntity != null) {
                mdibEntity
                        .doIfSingleState(state ->
                                entities.put(mdibEntity.getHandle(),
                                        entityFactory.replaceStates(mdibEntity, Collections.singletonList(modification))))
                        .orElse(states -> {
                            final List<AbstractState> newStates = new ArrayList<>();
                            typeValidator.toMultiState(modification).ifPresent(modifiedMultiState ->
                                    states.stream().forEach(multiState -> {
                                        if (multiState.getHandle().equals(modifiedMultiState.getHandle())) {
                                            newStates.add(modifiedMultiState);
                                        } else {
                                            newStates.add(multiState);
                                        }
                                    }));
                            entities.put(mdibEntity.getHandle(), entityFactory.replaceStates(mdibEntity,
                                    Collections.unmodifiableList(newStates)));
                        });
            }
        }

        return new StateResult(modifiedStates);
    }


    public class DescriptionResult implements MdibStorage.DescriptionResult {
        private final List<MdibEntity> insertedEntities;
        private final List<MdibEntity> updatedEntities;
        private final List<String> deletedEntities;

        public DescriptionResult(List<MdibEntity> insertedEntities,
                                 List<MdibEntity> updatedEntities,
                                 List<String> deletedEntities) {
            this.insertedEntities = insertedEntities;
            this.updatedEntities = updatedEntities;
            this.deletedEntities = deletedEntities;
        }

        @Override
        public List<MdibEntity> getInsertedEntities() {
            return insertedEntities;
        }

        @Override
        public List<MdibEntity> getUpdatedEntities() {
            return updatedEntities;
        }

        @Override
        public List<String> getDeletedEntities() {
            return deletedEntities;
        }
    }

    public class StateResult implements MdibStorage.StateResult {
        private final List<AbstractState> states;

        public StateResult(List<AbstractState> states) {
            this.states = states;
        }

        @Override
        public List<AbstractState> getStates() {
            return states;
        }
    }
}
