package org.somda.sdc.biceps.provider.access;

import com.google.inject.assistedinject.AssistedInject;
import org.somda.sdc.biceps.common.MdibDescriptionModifications;
import org.somda.sdc.biceps.common.MdibEntity;
import org.somda.sdc.biceps.common.MdibStateModifications;
import org.somda.sdc.biceps.common.access.*;
import org.somda.sdc.biceps.common.access.factory.ReadTransactionFactory;
import org.somda.sdc.biceps.common.access.helper.WriteUtil;
import org.somda.sdc.biceps.common.event.Distributor;
import org.somda.sdc.biceps.common.storage.MdibStorage;
import org.somda.sdc.biceps.common.storage.MdibStoragePreprocessingChain;
import org.somda.sdc.biceps.common.storage.PreprocessingException;
import org.somda.sdc.biceps.common.storage.factory.MdibStorageFactory;
import org.somda.sdc.biceps.common.storage.factory.MdibStoragePreprocessingChainFactory;
import org.somda.sdc.biceps.model.participant.AbstractContextState;
import org.somda.sdc.biceps.model.participant.AbstractDescriptor;
import org.somda.sdc.biceps.model.participant.AbstractState;
import org.somda.sdc.biceps.model.participant.MdibVersion;
import org.somda.sdc.biceps.provider.preprocessing.DuplicateChecker;
import org.somda.sdc.biceps.provider.preprocessing.HandleReferenceHandler;
import org.somda.sdc.biceps.provider.preprocessing.TypeConsistencyChecker;
import org.somda.sdc.biceps.provider.preprocessing.VersionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Default implementation of {@linkplain LocalMdibAccessImpl}.
 */
public class LocalMdibAccessImpl implements LocalMdibAccess {
    private static final Logger LOG = LoggerFactory.getLogger(LocalMdibAccessImpl.class);

    private final Distributor eventDistributor;
    private final MdibStorage mdibStorage;
    private final ReentrantReadWriteLock readWriteLock;
    private final ReadTransactionFactory readTransactionFactory;
    private final CopyManager copyManager;

    private final WriteUtil writeUtil;

    private MdibVersion mdibVersion;
    private BigInteger mdDescriptionVersion;
    private BigInteger mdStateVersion;

    @AssistedInject
    LocalMdibAccessImpl(Distributor eventDistributor,
                        MdibStoragePreprocessingChainFactory chainFactory,
                        MdibStorageFactory mdibStorageFactory,
                        ReentrantReadWriteLock readWriteLock,
                        ReadTransactionFactory readTransactionFactory,
                        DuplicateChecker duplicateChecker,
                        VersionHandler versionHandler,
                        TypeConsistencyChecker typeConsistencyChecker,
                        HandleReferenceHandler handleReferenceHandler,
                        CopyManager copyManager) {
        mdibVersion = MdibVersion.create();
        mdDescriptionVersion = BigInteger.ZERO;
        mdStateVersion = BigInteger.ZERO;

        this.eventDistributor = eventDistributor;
        this.mdibStorage = mdibStorageFactory.createMdibStorage(mdibVersion, BigInteger.ZERO, BigInteger.ZERO);
        this.readWriteLock = readWriteLock;
        this.readTransactionFactory = readTransactionFactory;
        this.copyManager = copyManager;

        MdibStoragePreprocessingChain localMdibAccessPreprocessing = chainFactory.createMdibStoragePreprocessingChain(
                mdibStorage,
                Arrays.asList(duplicateChecker, typeConsistencyChecker, versionHandler, handleReferenceHandler),
                Arrays.asList(versionHandler));

        this.writeUtil = new WriteUtil(LOG, eventDistributor, localMdibAccessPreprocessing, readWriteLock, this);
    }

    @Override
    public WriteDescriptionResult writeDescription(MdibDescriptionModifications mdibDescriptionModifications) throws PreprocessingException {
        mdibDescriptionModifications = copyManager.processInput(mdibDescriptionModifications);
        return writeUtil.writeDescription(descriptionModifications -> {
            mdibVersion = MdibVersion.increment(mdibVersion);
            mdDescriptionVersion = mdDescriptionVersion.add(BigInteger.ONE);
            mdStateVersion = mdStateVersion.add(BigInteger.ONE);
            return mdibStorage.apply(mdibVersion, mdDescriptionVersion, mdStateVersion, descriptionModifications);
        }, mdibDescriptionModifications);
    }

    @Override
    public WriteStateResult writeStates(MdibStateModifications mdibStateModifications) throws PreprocessingException {
        mdibStateModifications = copyManager.processInput(mdibStateModifications);
        return writeUtil.writeStates(stateModifications -> {
            mdibVersion = MdibVersion.increment(mdibVersion);
            mdStateVersion = mdStateVersion.add(BigInteger.ONE);
            return mdibStorage.apply(mdibVersion, mdStateVersion, stateModifications);
        }, mdibStateModifications);
    }

    @Override
    public void registerObserver(MdibAccessObserver observer) {
        LOG.info("Register MDIB observer: ", observer);
        eventDistributor.registerObserver(observer);
    }

    @Override
    public void unregisterObserver(MdibAccessObserver observer) {
        LOG.info("Unreigster MDIB observer: ", observer);
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
    public List<AbstractContextState> getContextStates() {
        try (ReadTransaction transaction = startTransaction()) {
            return transaction.getContextStates();
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
