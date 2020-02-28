package org.somda.sdc.biceps.common.storage;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.biceps.common.*;
import org.somda.sdc.biceps.common.access.WriteDescriptionResult;
import org.somda.sdc.biceps.common.access.WriteStateResult;
import org.somda.sdc.biceps.common.factory.MdibEntityFactory;
import org.somda.sdc.biceps.common.storage.helper.MdibStorageUtil;
import org.somda.sdc.biceps.model.participant.*;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Default implementation of {@linkplain MdibStorage}.
 */
public class MdibStorageImpl implements MdibStorage {
    private static final Logger LOG = LoggerFactory.getLogger(MdibStorageImpl.class);

    private final MdibEntityFactory entityFactory;
    private final MdibStorageUtil util;
    private final MdibTypeValidator typeValidator;

    private MdibVersion mdibVersion;
    private BigInteger mdDescriptionVersion;
    private BigInteger mdStateVersion;

    private Map<String, MdibEntity> entities;
    private ArrayList<String> rootEntities;
    private Map<String, AbstractContextState> contextStates;


    @AssistedInject
    MdibStorageImpl(MdibEntityFactory entityFactory,
                    MdibStorageUtil util,
                    MdibTypeValidator typeValidator) {
        this(MdibVersion.create(), BigInteger.valueOf(-1), BigInteger.valueOf(-1), entityFactory, util, typeValidator);
    }

    @AssistedInject
    MdibStorageImpl(@Assisted MdibVersion initialMdibVersion,
                    MdibEntityFactory entityFactory,
                    MdibStorageUtil util,
                    MdibTypeValidator typeValidator) {
        this(initialMdibVersion, BigInteger.valueOf(-1), BigInteger.valueOf(-1), entityFactory, util, typeValidator);
    }

    @AssistedInject
    MdibStorageImpl(@Assisted MdibVersion initialMdibVersion,
                    @Assisted("mdDescriptionVersion") BigInteger mdDescriptionVersion,
                    @Assisted("mdStateVersion") BigInteger mdStateVersion,
                    MdibEntityFactory entityFactory,
                    MdibStorageUtil util,
                    MdibTypeValidator typeValidator) {
        this.mdibVersion = initialMdibVersion;
        this.mdDescriptionVersion = mdDescriptionVersion;
        this.mdStateVersion = mdStateVersion;
        this.entityFactory = entityFactory;
        this.util = util;
        this.typeValidator = typeValidator;

        this.entities = new HashMap<>();
        this.rootEntities = new ArrayList<>();
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

    @Override
    public <T extends AbstractDescriptor> Collection<MdibEntity> findEntitiesByType(Class<T> type) {
        List<MdibEntity> result = new ArrayList<>();
        for (MdibEntity entity : entities.values()) {
            if (type.isAssignableFrom(entity.getDescriptor().getClass())) {
                result.add(entity);
            }
        }
        return result;
    }

    @Override
    public <T extends AbstractDescriptor> List<MdibEntity> getChildrenByType(String handle, Class<T> type) {
        final List<MdibEntity> result = new ArrayList<>();
        final Optional<MdibEntity> entity = getEntity(handle);
        if (entity.isEmpty()) {
            return result;
        }

        for (String child : entity.get().getChildren()) {
            getEntity(child).ifPresent(childEntity ->
            {
                if (type.isAssignableFrom(childEntity.getDescriptor().getClass())) {
                    result.add(childEntity);
                }
            });
        }

        return result;
    }

    public List<MdibEntity> getRootEntities() {
        return util.exposeEntityList(entities, rootEntities);
    }

    public Optional<AbstractState> getState(String handle) {
        return getState(handle, AbstractState.class);
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
    public List<AbstractMultiState> getMultiStates(String descriptorHandle) {
        final MdibEntity entity = entities.get(descriptorHandle);
        if (entity == null || entity.getStates().isEmpty()) {
            return Collections.emptyList();
        }
        return util.exposeListOfType(entity.getStates(), AbstractMultiState.class);
    }

    @Override
    public List<AbstractContextState> getContextStates() {
        return new ArrayList<>(contextStates.values());
    }

    @Override
    public <T extends AbstractContextState> List<T> findContextStatesByType(Class<T> stateClass) {
        var result = new ArrayList<T>();
        contextStates.forEach((handle, state) -> {
            if (stateClass.isAssignableFrom(state.getClass())) {
                result.add(stateClass.cast(state));
            }
        });
        return result;
    }

    @Override
    public WriteDescriptionResult apply(MdibVersion mdibVersion,
                                        @Nullable BigInteger mdDescriptionVersion,
                                        @Nullable BigInteger mdStateVersion,
                                        MdibDescriptionModifications descriptionModifications) {
        this.mdibVersion = mdibVersion;
        Optional.ofNullable(mdDescriptionVersion).ifPresent(version -> this.mdDescriptionVersion = version);
        Optional.ofNullable(mdDescriptionVersion).ifPresent(version -> this.mdStateVersion = version);

        final List<MdibEntity> insertedEntities = new ArrayList<>();
        final List<MdibEntity> updatedEntities = new ArrayList<>();
        final List<MdibEntity> deletedEntities = new ArrayList<>();
        for (MdibDescriptionModification modification : descriptionModifications.getModifications()) {
            var notAssociatedContextStates = new ArrayList<AbstractState>();
            for (AbstractState state : modification.getStates()) {
                if (state instanceof AbstractContextState &&
                        ContextAssociation.NO.equals(((AbstractContextState) state).getContextAssociation())) {
                    notAssociatedContextStates.add(state);
                }
            }
            modification.getStates().removeAll(notAssociatedContextStates);

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

        return new WriteDescriptionResult(mdibVersion, insertedEntities, updatedEntities, deletedEntities);
    }

    private void deleteEntity(MdibDescriptionModification modification, List<MdibEntity> deletedEntities) {
        Optional.ofNullable(entities.get(modification.getHandle())).ifPresent(mdibEntity -> {
            LOG.debug("[{}] Delete entity: {}", mdibVersion.getInstanceId(), modification.getDescriptor().getHandle());
            mdibEntity.getParent().ifPresent(parentHandle ->
                    Optional.ofNullable(entities.get(parentHandle)).ifPresent(parentEntity ->
                            entities.put(parentEntity.getHandle(), entityFactory.replaceChildren(parentEntity,
                                    parentEntity.getChildren().stream()
                                            .filter(s -> s.equals(mdibEntity.getHandle()))
                                            .collect(Collectors.toList())))));
        });

        final MdibEntity deletedEntity = entities.get(modification.getHandle());
        if (deletedEntity == null) {
            LOG.warn("Possible inconsistency detected. Entity to delete was not found: {}" + modification.getHandle());
            return;
        }

        rootEntities.remove(modification.getHandle());
        entities.remove(modification.getHandle());
        contextStates.entrySet().removeIf(state ->
                state.getValue().getDescriptorHandle().equals(modification.getHandle()));

        deletedEntities.add(deletedEntity);
    }

    private void updateEntity(MdibDescriptionModification modification, List<MdibEntity> updatedEntities) {
        Optional.ofNullable(entities.get(modification.getHandle())).ifPresent(mdibEntity -> {
            LOG.debug("[{}] Update entity: {}", mdibVersion.getInstanceId(), modification.getDescriptor());
            mdibEntity = entityFactory.replaceDescriptorAndStates(
                    mdibEntity,
                    modification.getDescriptor(),
                    modification.getStates());
            entities.put(mdibEntity.getHandle(), mdibEntity);
            updatedEntities.add(mdibEntity);
        });
    }

    private void insertEntity(MdibDescriptionModification modification, List<MdibEntity> insertedEntities) {
        final MdibEntity mdibEntity = entityFactory.createMdibEntity(
                modification.getParentHandle().orElse(null),
                new ArrayList<>(),
                modification.getDescriptor(),
                modification.getStates(),
                mdibVersion);

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

        LOG.debug("[{}] Insert entity: {}", mdibVersion.getInstanceId(), mdibEntity.getDescriptor());

        // Add to context states if context entity
        if (mdibEntity.getDescriptor() instanceof AbstractContextDescriptor) {
            contextStates.putAll(mdibEntity.getStates().stream()
                    .map(state -> (AbstractContextState) state)
                    .collect(Collectors.toMap(AbstractMultiState::getHandle, state -> state)));
        }

        insertedEntities.add(mdibEntity);
    }

    @Override
    public WriteStateResult apply(MdibVersion mdibVersion,
                                  @Nullable BigInteger mdStateVersion,
                                  MdibStateModifications stateModifications) {
        this.mdibVersion = mdibVersion;
        Optional.ofNullable(mdStateVersion).ifPresent(version -> this.mdStateVersion = version);

        MdibDescriptionModifications descriptionModifications = null;

        final List<AbstractState> modifiedStates = new ArrayList<>();
        for (AbstractState modification : stateModifications.getStates()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("[{}] Update state: {}", mdibVersion.getSequenceId(), modification);
            }


            modifiedStates.add(modification);

            final MdibEntity mdibEntity = entities.get(modification.getDescriptorHandle());
            if (mdibEntity == null) {
                // Do not store context states when not associated
                var contextState = getNotAssociatedContextState(modification);
                if (contextState.isEmpty()) {
                    LOG.debug("Found update on context state {} with association=not-associated; do not store in MDIB",
                            contextState.get().getHandle());
                    continue;
                }

                // this will insert states even if no descriptor/MDIB entity exists
                // to be used in remote MDIBS in case
                if (descriptionModifications == null) {
                    descriptionModifications = MdibDescriptionModifications.create();
                }
                AbstractDescriptor descr;
                try {
                    descr = typeValidator.resolveDescriptorType(modification.getClass())
                            .getConstructor().newInstance();
                } catch (Exception e) {
                    LOG.warn(String.format("Ignore modification. Reason: could not instantiate descriptor type for handle %s.",
                            modification.getDescriptorHandle()), e);
                    continue;
                }
                descr.setHandle(modification.getDescriptorHandle());
                descr.setDescriptorVersion(BigInteger.valueOf(-1));
                descriptionModifications.insert(descr, modification);
            } else {
                mdibEntity
                        .doIfSingleState(state -> {
                            entities.put(mdibEntity.getHandle(),
                                    entityFactory.replaceStates(mdibEntity, Collections.singletonList(modification)));
                        })
                        .orElse(states -> {
                            var modificationAsMultiState = typeValidator.toMultiState(modification).orElseThrow(() ->
                                    new RuntimeException(
                                            String.format("Found a non-matching multi-state for multi-state entity update (descriptor handle: %s",
                                                    mdibEntity.getHandle())));

                            var newStates = new ArrayList<AbstractMultiState>();
                            boolean found = false;
                            for (AbstractMultiState multiState : states) {
                                if (multiState.getHandle().equals(modificationAsMultiState.getHandle())) {
                                    found = true;
                                    if (getNotAssociatedContextState(modificationAsMultiState).isPresent()) {
                                        LOG.debug("Found context state {} with association=not-associated; refuse storage in MDIB",
                                                modificationAsMultiState.getHandle());
                                        contextStates.remove(multiState.getHandle());
                                    } else {
                                        LOG.debug("Replacing already present multi-state {}", multiState.getHandle());
                                        newStates.add(modificationAsMultiState);
                                    }
                                } else {
                                    newStates.add(multiState);
                                }
                            }

                            if (!found && getNotAssociatedContextState(modificationAsMultiState).isEmpty()) {
                                LOG.debug("Adding new MultiState {}", modificationAsMultiState.getHandle());
                                newStates.add(modificationAsMultiState);
                            }

                            entities.put(mdibEntity.getHandle(), entityFactory.replaceStates(mdibEntity,
                                    Collections.unmodifiableList(newStates)));
                            newStates.stream()
                                    .filter(abstractMultiState -> abstractMultiState instanceof AbstractContextState)
                                    .map(abstractMultiState -> (AbstractContextState) abstractMultiState)
                                    .collect(Collectors.toList())
                                    .forEach(abstractContextState ->
                                            contextStates.put(abstractContextState.getHandle(), abstractContextState));
                        });
            }
        }

        // Only relevant to remote MDIBs where entities need to be present even if only states are subscribed
        // (an unlikely use-case, but should be supported nevertheless)
        if (descriptionModifications != null) {
            apply(mdibVersion, null, null, descriptionModifications);
        }

        return new WriteStateResult(mdibVersion, modifiedStates);
    }

    @Override
    public MdibVersion getMdibVersion() {
        return mdibVersion;
    }

    @Override
    public BigInteger getMdDescriptionVersion() {
        return mdDescriptionVersion;
    }

    @Override
    public BigInteger getMdStateVersion() {
        return mdStateVersion;
    }

    private Optional<AbstractContextState> getNotAssociatedContextState(AbstractState state) {
        if (state instanceof AbstractContextState) {
            var contextState = (AbstractContextState) state;
            if (contextState.getContextAssociation() == null || ContextAssociation.NO.equals(contextState.getContextAssociation())) {
                return Optional.of(contextState);
            }
        }
        return Optional.empty();
    }
}
