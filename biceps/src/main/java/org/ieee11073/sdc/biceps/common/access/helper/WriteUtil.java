package org.ieee11073.sdc.biceps.common.access.helper;

import org.ieee11073.sdc.biceps.common.MdibDescriptionModifications;
import org.ieee11073.sdc.biceps.common.MdibStateModifications;
import org.ieee11073.sdc.biceps.common.access.MdibAccess;
import org.ieee11073.sdc.biceps.common.access.WriteDescriptionResult;
import org.ieee11073.sdc.biceps.common.access.WriteStateResult;
import org.ieee11073.sdc.biceps.common.event.Distributor;
import org.ieee11073.sdc.biceps.common.storage.MdibStoragePreprocessingChain;
import org.ieee11073.sdc.biceps.common.storage.PreprocessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

/**
 * Common code for write description and states that can be used by BICEPS providers and consumers.
 * <p>
 * <em>Remark: The operations do not copy descriptors or states.</em>
 */
public class WriteUtil {
    private static final Logger LOG = LoggerFactory.getLogger(WriteUtil.class);

    private final Distributor eventDistributor;
    private final MdibStoragePreprocessingChain mdibAccessPreprocessing;
    private final ReentrantReadWriteLock readWriteLock;
    private final MdibAccess mdibAccess;

    /**
     * Constructor that accepts the dependencies in order to properly process write operations.
     *
     * @param eventDistributor        the event distributor to send event messages after write operation is done.
     * @param mdibAccessPreprocessing the preprocessing chain that is invoked before writing to the MDIB storage.
     * @param readWriteLock           a read write lock to protect against concurrent access.
     * @param mdibAccess              the MDIB access that is passed on event distribution.
     */
    public WriteUtil(Distributor eventDistributor,
                     MdibStoragePreprocessingChain mdibAccessPreprocessing,
                     ReentrantReadWriteLock readWriteLock,
                     MdibAccess mdibAccess) {
        this.eventDistributor = eventDistributor;
        this.mdibAccessPreprocessing = mdibAccessPreprocessing;
        this.readWriteLock = readWriteLock;
        this.mdibAccess = mdibAccess;
    }

    /**
     * Performs preprocessing, write operation and event distribution of description modifications.
     * <p>
     * The write operation gains a write lock, downgrades it to a read lock during event distribution and releases the
     * read lock by the end of the function.
     *
     * @param lockedWriteDescription   locked callback to finally write description.
     * @param descriptionModifications the description modifications to write.
     * @return a write description result that contains inserted, updated and deleted entities.
     * @throws PreprocessingException in case a consistency check or modifier fails.
     */
    public WriteDescriptionResult writeDescription(Function<MdibDescriptionModifications, WriteDescriptionResult> lockedWriteDescription,
                                                   MdibDescriptionModifications descriptionModifications) throws PreprocessingException {
        readWriteLock.writeLock().lock();
        WriteDescriptionResult modificationResult;
        try {
            mdibAccessPreprocessing.processDescriptionModifications(descriptionModifications);
            modificationResult = lockedWriteDescription.apply(descriptionModifications);
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

    /**
     * Performs preprocessing, write operation and event distribution of state modifications.
     * <p>
     * The write operation gains a write lock, downgrades it to a read lock during event distribution and releases the
     * read lock by the end of the function.
     *
     * @param lockedWriteStates  locked callback to finally write states.
     * @param stateModifications the state modifications to write.
     * @return a write state result that contains updated states.
     * @throws PreprocessingException in case a consistency check or modifier fails.
     */
    public WriteStateResult writeStates(Function<MdibStateModifications, WriteStateResult> lockedWriteStates,
                                        MdibStateModifications stateModifications) throws PreprocessingException {
        readWriteLock.writeLock().lock();
        WriteStateResult modificationResult;
        try {
            mdibAccessPreprocessing.processStateModifications(stateModifications);
            modificationResult = lockedWriteStates.apply(stateModifications);
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
