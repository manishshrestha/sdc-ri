package org.ieee11073.sdc.biceps.provider;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import org.ieee11073.sdc.biceps.common.*;
import org.ieee11073.sdc.biceps.common.access.MdibAccessObserver;
import org.ieee11073.sdc.biceps.common.event.Distributor;
import org.ieee11073.sdc.biceps.common.factory.MdibStoragePreprocessingChainFactory;
import org.ieee11073.sdc.biceps.model.participant.AbstractContextState;
import org.ieee11073.sdc.biceps.model.participant.AbstractDescriptor;
import org.ieee11073.sdc.biceps.model.participant.AbstractState;
import org.ieee11073.sdc.common.helper.AutoLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LocalMdibAccessImpl implements LocalMdibAccess {
    private static final Logger LOG = LoggerFactory.getLogger(LocalMdibAccessImpl.class);

    private final Distributor eventDistributor;
    private final MdibStorage mdibStorage;
    private MdibStoragePreprocessingChain localMdibAccessPreprocessing;
    private final ReentrantReadWriteLock readWriteLock;
    private MdibVersion mdibVersion;

    @Inject
    LocalMdibAccessImpl(Distributor eventDistributor,
                        MdibStoragePreprocessingChainFactory chainFactory,
                        MdibStorage mdibStorage,
                        EventBus eventBus,
                        ReentrantReadWriteLock readWriteLock) {
        this.eventDistributor = eventDistributor;
        this.mdibStorage = mdibStorage;
        this.readWriteLock = readWriteLock;

        this.mdibVersion = MdibVersion.create();

//        this.localMdibAccessPreprocessing = chainFactory.createMdibStoragePreprocessingChain(
//                mdibStorage,
//                Collections.emptyList(),
//                Collections.emptyList());
    }

    @Override
    public void writeDescription(MdibDescriptionModifications descriptionModifications) throws PreprocessingException {
        readWriteLock.writeLock().lock();
        MdibStorage.DescriptionResult modificationResult;
        try {
            localMdibAccessPreprocessing.processDescriptionModifications(descriptionModifications);
            mdibVersion = MdibVersion.increment(mdibVersion);
            modificationResult = mdibStorage.apply(MdibDescriptionModifications.create(mdibVersion, descriptionModifications));

            readWriteLock.readLock().lock();

        } catch (PreprocessingException e) {
            LOG.warn("Error while processing description modifications in chain segment {} on handle {}: {}",
                    e.getSegment(), e.getHandle(), e.getMessage());
            throw e;
        } finally {
            readWriteLock.writeLock().unlock();
        }

        try {
            eventDistributor.sendDescriptionModificationEvent(this,
                    modificationResult.getInsertedEntities(),
                    modificationResult.getUpdatedEntities(),
                    modificationResult.getDeletedEntities());
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    @Override
    public void writeStates(MdibStateModifications stateModifications) throws PreprocessingException {
        readWriteLock.writeLock().lock();
        MdibStorage.StateResult modificationResult;
        try {
            localMdibAccessPreprocessing.processStateModifications(stateModifications);
            mdibVersion = MdibVersion.increment(mdibVersion);
            modificationResult = mdibStorage.apply(MdibStateModifications.create(mdibVersion, stateModifications));

            readWriteLock.readLock().lock();

        } catch (PreprocessingException e) {
            LOG.warn("Error while processing state modifications in chain segment {} on handle {}: {}",
                    e.getSegment(), e.getHandle(), e.getMessage());
            throw e;
        } finally {
            readWriteLock.writeLock().unlock();
        }

        try {
            eventDistributor.sendStateModificationEvent(this,
                    stateModifications.getChangeType(),
                    modificationResult.getStates());
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    @Override
    public void registerObserver(MdibAccessObserver observer) {
        eventDistributor.registerObserver(observer);
    }

    @Override
    public void unregisterObserver(MdibAccessObserver observer) {
        eventDistributor.registerObserver(observer);
    }

    @Override
    public <T extends AbstractDescriptor> Optional<T> getDescriptor(String handle, Class<T> descrClass) {
        try (AutoLock ignored = AutoLock.lock(readWriteLock.readLock())) {
            return mdibStorage.getDescriptor(handle, descrClass);
        }
    }

    @Override
    public Optional<AbstractDescriptor> getDescriptor(String handle) {
        return getDescriptor(handle, AbstractDescriptor.class);
    }

    @Override
    public Optional<MdibEntity> getEntity(String handle) {
        try (AutoLock ignored = AutoLock.lock(readWriteLock.readLock())) {
            return mdibStorage.getEntity(handle);
        }
    }

    @Override
    public List<MdibEntity> getRootEntities() {
        try (AutoLock ignored = AutoLock.lock(readWriteLock.readLock())) {
            return mdibStorage.getRootEntities();
        }
    }

    @Override
    public <T extends AbstractState> Optional<T> getState(String handle, Class<T> stateClass) {
        try (AutoLock ignored = AutoLock.lock(readWriteLock.readLock())) {
            return mdibStorage.getState(handle, stateClass);
        }
    }

    @Override
    public <T extends AbstractContextState> List<T> getContextStates(String descriptorHandle, Class<T> stateClass) {
        try (AutoLock ignored = AutoLock.lock(readWriteLock.readLock())) {
            return mdibStorage.getContextStates(descriptorHandle, stateClass);
        }
    }
}
