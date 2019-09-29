package org.ieee11073.sdc.biceps.consumer.access;

import com.google.inject.assistedinject.AssistedInject;
import org.ieee11073.sdc.biceps.common.MdibDescriptionModifications;
import org.ieee11073.sdc.biceps.common.MdibEntity;
import org.ieee11073.sdc.biceps.common.MdibStateModifications;
import org.ieee11073.sdc.biceps.common.access.*;
import org.ieee11073.sdc.biceps.common.access.factory.ReadTransactionFactory;
import org.ieee11073.sdc.biceps.common.access.helper.WriteUtil;
import org.ieee11073.sdc.biceps.common.event.Distributor;
import org.ieee11073.sdc.biceps.common.preprocessing.DuplicateChecker;
import org.ieee11073.sdc.biceps.common.preprocessing.TypeConsistencyChecker;
import org.ieee11073.sdc.biceps.common.storage.MdibStorage;
import org.ieee11073.sdc.biceps.common.storage.MdibStoragePreprocessingChain;
import org.ieee11073.sdc.biceps.common.storage.PreprocessingException;
import org.ieee11073.sdc.biceps.common.storage.factory.MdibStorageFactory;
import org.ieee11073.sdc.biceps.common.storage.factory.MdibStoragePreprocessingChainFactory;
import org.ieee11073.sdc.biceps.consumer.preprocessing.VersionDuplicateHandler;
import org.ieee11073.sdc.biceps.model.participant.AbstractContextState;
import org.ieee11073.sdc.biceps.model.participant.AbstractDescriptor;
import org.ieee11073.sdc.biceps.model.participant.AbstractState;
import org.ieee11073.sdc.biceps.model.participant.MdibVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Default implementation of {@linkplain RemoteMdibAccessImpl}.
 */
public class RemoteMdibAccessImpl implements RemoteMdibAccess {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteMdibAccessImpl.class);

    private final Distributor eventDistributor;
    private final MdibStorage mdibStorage;
    private final MdibStoragePreprocessingChain localMdibAccessPreprocessing;
    private final ReentrantReadWriteLock readWriteLock;
    private final ReadTransactionFactory readTransactionFactory;
    private final CopyManager copyManager;

    private final WriteUtil writeUtil;

    @AssistedInject
    RemoteMdibAccessImpl(Distributor eventDistributor,
                         MdibStoragePreprocessingChainFactory chainFactory,
                         MdibStorageFactory mdibStorageFactory,
                         ReentrantReadWriteLock readWriteLock,
                         ReadTransactionFactory readTransactionFactory,
                         DuplicateChecker duplicateChecker,
                         VersionDuplicateHandler versionDuplicateHandler,
                         TypeConsistencyChecker typeConsistencyChecker,
                         CopyManager copyManager) {
        this.eventDistributor = eventDistributor;
        this.mdibStorage = mdibStorageFactory.createMdibStorage();
        this.readWriteLock = readWriteLock;
        this.readTransactionFactory = readTransactionFactory;
        this.copyManager = copyManager;

        this.localMdibAccessPreprocessing = chainFactory.createMdibStoragePreprocessingChain(
                mdibStorage,
                Arrays.asList(duplicateChecker, typeConsistencyChecker),
                Arrays.asList(versionDuplicateHandler));

        this.writeUtil = new WriteUtil(eventDistributor, localMdibAccessPreprocessing, readWriteLock, this);
    }

    @Override
    public WriteDescriptionResult writeDescription(MdibVersion mdibVersion,
                                                   @Nullable BigInteger mdDescriptionVersion,
                                                   @Nullable BigInteger mdStateVersion,
                                                   MdibDescriptionModifications mdibDescriptionModifications) throws PreprocessingException {
        // No copy of mdibDescriptionModifications here as data is read from network source
        // SDCri takes over responsibility to not change elements after write
        return writeUtil.writeDescription(descriptionModifications ->
                        mdibStorage.apply(mdibVersion, mdDescriptionVersion, mdStateVersion, descriptionModifications),
                mdibDescriptionModifications);
    }

    @Override
    public WriteStateResult writeStates(MdibVersion mdibVersion,
                                        MdibStateModifications mdibStateModifications) throws PreprocessingException {
        // No copy of mdibStateModifications here as data is read from network source
        // SDCri takes over responsibility to not change elements after write
        // return writeUtil.writeStates(mdibStateModifications);
        return writeUtil.writeStates(stateModifications ->
                        mdibStorage.apply(mdibVersion, null, stateModifications),
                mdibStateModifications);
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
    public Optional<AbstractState> getState(String handle) {
        try (ReadTransaction transaction = startTransaction()) {
            return transaction.getState(handle);
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
    public List<AbstractContextState> getContextStates(String descriptorHandle) {
        return null;
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
