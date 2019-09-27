package org.ieee11073.sdc.biceps.common.access.helper;

import org.ieee11073.sdc.biceps.common.MdibDescriptionModifications;
import org.ieee11073.sdc.biceps.common.MdibStateModifications;
import org.ieee11073.sdc.biceps.common.access.MdibAccess;
import org.ieee11073.sdc.biceps.common.access.WriteDescriptionResult;
import org.ieee11073.sdc.biceps.common.access.WriteStateResult;
import org.ieee11073.sdc.biceps.common.event.Distributor;
import org.ieee11073.sdc.biceps.common.storage.MdibStorage;
import org.ieee11073.sdc.biceps.common.storage.MdibStoragePreprocessingChain;
import org.ieee11073.sdc.biceps.common.storage.PreprocessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class WriteUtil {
    private static final Logger LOG = LoggerFactory.getLogger(WriteUtil.class);

    private final Distributor eventDistributor;
    private final MdibStorage mdibStorage;
    private final MdibStoragePreprocessingChain localMdibAccessPreprocessing;
    private final ReentrantReadWriteLock readWriteLock;
    private final MdibAccess mdibAccess;

    public WriteUtil(Distributor eventDistributor,
                     MdibStorage mdibStorage,
                     MdibStoragePreprocessingChain localMdibAccessPreprocessing,
                     ReentrantReadWriteLock readWriteLock,
                     MdibAccess mdibAccess) {
        this.eventDistributor = eventDistributor;
        this.mdibStorage = mdibStorage;
        this.localMdibAccessPreprocessing = localMdibAccessPreprocessing;
        this.readWriteLock = readWriteLock;
        this.mdibAccess = mdibAccess;
    }

    public WriteDescriptionResult writeDescription(MdibDescriptionModifications descriptionModifications) throws PreprocessingException {
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
                    mdibAccess,
                    modificationResult.getInsertedEntities(),
                    modificationResult.getUpdatedEntities(),
                    modificationResult.getDeletedEntities());
        } finally {
            readWriteLock.readLock().unlock();
        }

        return modificationResult;
    }

    public WriteStateResult writeStates(MdibStateModifications stateModifications) throws PreprocessingException {
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
                    mdibAccess,
                    stateModifications.getChangeType(),
                    modificationResult.getStates());
        } finally {
            readWriteLock.readLock().unlock();
        }

        return modificationResult;
    }
}
