package org.ieee11073.sdc.biceps.provider;

import com.google.inject.assistedinject.AssistedInject;
import org.ieee11073.sdc.biceps.common.*;
import org.ieee11073.sdc.biceps.common.access.*;
import org.ieee11073.sdc.biceps.common.access.factory.ReadTransactionFactory;
import org.ieee11073.sdc.biceps.common.event.Distributor;
import org.ieee11073.sdc.biceps.common.factory.MdibStorageFactory;
import org.ieee11073.sdc.biceps.common.factory.MdibStoragePreprocessingChainFactory;
import org.ieee11073.sdc.biceps.common.preprocessing.DuplicateChecker;
import org.ieee11073.sdc.biceps.common.preprocessing.TypeConsistencyChecker;
import org.ieee11073.sdc.biceps.common.storage.MdibStorage;
import org.ieee11073.sdc.biceps.common.storage.MdibStoragePreprocessingChain;
import org.ieee11073.sdc.biceps.common.storage.PreprocessingException;
import org.ieee11073.sdc.biceps.model.participant.AbstractContextState;
import org.ieee11073.sdc.biceps.model.participant.AbstractDescriptor;
import org.ieee11073.sdc.biceps.model.participant.AbstractState;
import org.ieee11073.sdc.biceps.model.participant.MdibVersion;
import org.ieee11073.sdc.biceps.provider.preprocessing.VersionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LocalMdibAccessImpl implements LocalMdibAccess {
    private static final Logger LOG = LoggerFactory.getLogger(LocalMdibAccessImpl.class);

    private final Distributor eventDistributor;
    private final MdibStorage mdibStorage;
    private MdibStoragePreprocessingChain localMdibAccessPreprocessing;
    private final ReentrantReadWriteLock readWriteLock;
    private final ReadTransactionFactory readTransactionFactory;
    private final CopyManager copyManager;

    @AssistedInject
    LocalMdibAccessImpl(Distributor eventDistributor,
                        MdibStoragePreprocessingChainFactory chainFactory,
                        MdibStorageFactory mdibStorageFactory,
                        ReentrantReadWriteLock readWriteLock,
                        ReadTransactionFactory readTransactionFactory,
                        DuplicateChecker duplicateChecker,
                        VersionHandler versionHandler,
                        TypeConsistencyChecker typeConsistencyChecker,
                        CopyManager copyManager) {
        this.eventDistributor = eventDistributor;
        this.mdibStorage = mdibStorageFactory.createMdibStorage();
        this.readWriteLock = readWriteLock;
        this.readTransactionFactory = readTransactionFactory;
        this.copyManager = copyManager;

        this.localMdibAccessPreprocessing = chainFactory.createMdibStoragePreprocessingChain(
                mdibStorage,
                Arrays.asList(duplicateChecker, typeConsistencyChecker, versionHandler),
                Arrays.asList(versionHandler));
    }

    @Override
    public WriteDescriptionResult writeDescription(MdibDescriptionModifications descriptionModifications) throws PreprocessingException {
        descriptionModifications = copyManager.processInput(descriptionModifications);

        readWriteLock.writeLock().lock();
        WriteDescriptionResult modificationResult;
        try {
            localMdibAccessPreprocessing.processDescriptionModifications(descriptionModifications);
            modificationResult = mdibStorage.apply(descriptionModifications);

            readWriteLock.readLock().lock();

        } catch (PreprocessingException e) {
            LOG.warn("Error while processing description modifications in chain segment {} on handle {}: {}",
                    e.getSegment(), e.getHandle(), e.getMessage());
            throw e;
        } finally {
            readWriteLock.writeLock().unlock();
        }

        try {
            eventDistributor.sendDescriptionModificationEvent(
                    this,
                    modificationResult.getInsertedEntities(),
                    modificationResult.getUpdatedEntities(),
                    modificationResult.getDeletedEntities());
        } finally {
            readWriteLock.readLock().unlock();
        }

        return modificationResult;
    }

    @Override
    public WriteStateResult writeStates(MdibStateModifications stateModifications) throws PreprocessingException {
        stateModifications = copyManager.processInput(stateModifications);

        readWriteLock.writeLock().lock();
        WriteStateResult modificationResult;
        try {
            localMdibAccessPreprocessing.processStateModifications(stateModifications);
            modificationResult = mdibStorage.apply(stateModifications);

            readWriteLock.readLock().lock();

        } catch (PreprocessingException e) {
            LOG.warn("Error while processing state modifications in chain segment {} on handle {}: {}",
                    e.getSegment(), e.getHandle(), e.getMessage());
            throw e;
        } finally {
            readWriteLock.writeLock().unlock();
        }

        try {
            eventDistributor.sendStateModificationEvent(
                    this,
                    stateModifications.getChangeType(),
                    modificationResult.getStates());
        } finally {
            readWriteLock.readLock().unlock();
        }

        return modificationResult;
    }

    @Override
    public void registerObserver(MdibAccessObserver observer) {
        eventDistributor.registerObserver(observer);
    }

    @Override
    public void unregisterObserver(MdibAccessObserver observer) {
        eventDistributor.unregisterObserver(observer);
    }

    @Override
    public MdibVersion getMdibVersion() {
        try (ReadTransaction transaction = startTransaction()) {
            return transaction.getMdibVersion();
        }
    }

    @Override
    public BigInteger getMdDescriptionVersion() {
        try (ReadTransaction transaction = startTransaction()) {
            return transaction.getMdDescriptionVersion();
        }
    }

    @Override
    public BigInteger getMdStateVersion() {
        try (ReadTransaction transaction = startTransaction()) {
            return transaction.getMdStateVersion();
        }
    }

    @Override
    public <T extends AbstractDescriptor> Optional<T> getDescriptor(String handle, Class<T> descrClass) {
        try (ReadTransaction transaction = startTransaction()) {
            return transaction.getDescriptor(handle, descrClass);
        }
    }

    @Override
    public Optional<AbstractDescriptor> getDescriptor(String handle) {
        try (ReadTransaction transaction = startTransaction()) {
            return transaction.getDescriptor(handle, AbstractDescriptor.class);
        }
    }

    @Override
    public Optional<MdibEntity> getEntity(String handle) {
        try (ReadTransaction transaction = startTransaction()) {
            return transaction.getEntity(handle);
        }
    }

    @Override
    public List<MdibEntity> getRootEntities() {
        try (ReadTransaction transaction = startTransaction()) {
            return transaction.getRootEntities();
        }
    }

    @Override
    public <T extends AbstractState> Optional<T> getState(String handle, Class<T> stateClass) {
        try (ReadTransaction transaction = startTransaction()) {
            return transaction.getState(handle, stateClass);
        }
    }

    @Override
    public <T extends AbstractContextState> List<T> getContextStates(String descriptorHandle, Class<T> stateClass) {
        try (ReadTransaction transaction = startTransaction()) {
            return transaction.getContextStates(descriptorHandle, stateClass);
        }
    }

    @Override
    public <T extends AbstractDescriptor> Collection<MdibEntity> findEntitiesByType(Class<T> type) {
        try (ReadTransaction transaction = startTransaction()) {
            return transaction.findEntitiesByType(type);
        }
    }

    @Override
    public <T extends AbstractDescriptor> List<MdibEntity> getChildrenByType(String handle, Class<T> type) {
        try (ReadTransaction transaction = startTransaction()) {
            return transaction.getChildrenByType(handle, type);
        }
    }

    @Override
    public ReadTransaction startTransaction() {
        return readTransactionFactory.createReadTransaction(mdibStorage, readWriteLock.readLock());
    }
}
