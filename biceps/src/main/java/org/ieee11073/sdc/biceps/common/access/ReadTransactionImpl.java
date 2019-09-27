package org.ieee11073.sdc.biceps.common.access;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.ieee11073.sdc.biceps.common.MdibEntity;
import org.ieee11073.sdc.biceps.common.storage.MdibStorage;
import org.ieee11073.sdc.biceps.model.participant.MdibVersion;
import org.ieee11073.sdc.biceps.model.participant.AbstractContextState;
import org.ieee11073.sdc.biceps.model.participant.AbstractDescriptor;
import org.ieee11073.sdc.biceps.model.participant.AbstractState;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;

public class ReadTransactionImpl implements ReadTransaction {
    private final Lock lock;
    private final MdibStorage mdibStorage;

    @AssistedInject
    public ReadTransactionImpl(@Assisted MdibStorage mdibStorage,
                               @Assisted Lock lock) {
        this.lock = lock;
        this.lock.lock();
        this.mdibStorage = mdibStorage;
    }

    @Override
    public void close() {
        lock.unlock();
    }

    @Override
    public MdibVersion getMdibVersion() {
        return mdibStorage.getMdibVersion();
    }

    @Override
    public BigInteger getMdDescriptionVersion() {
        return mdibStorage.getMdDescriptionVersion();
    }

    @Override
    public BigInteger getMdStateVersion() {
        return mdibStorage.getMdStateVersion();
    }

    @Override
    public <T extends AbstractDescriptor> Optional<T> getDescriptor(String handle, Class<T> descrClass) {
        return mdibStorage.getDescriptor(handle, descrClass);
    }

    @Override
    public Optional<AbstractDescriptor> getDescriptor(String handle) {
        return getDescriptor(handle, AbstractDescriptor.class);
    }

    @Override
    public Optional<MdibEntity> getEntity(String handle) {
        return mdibStorage.getEntity(handle);
    }

    @Override
    public List<MdibEntity> getRootEntities() {
        return mdibStorage.getRootEntities();
    }

    @Override
    public <T extends AbstractState> Optional<T> getState(String handle, Class<T> stateClass) {
        return mdibStorage.getState(handle, stateClass);
    }

    @Override
    public <T extends AbstractContextState> List<T> getContextStates(String descriptorHandle, Class<T> stateClass) {
        return mdibStorage.getContextStates(descriptorHandle, stateClass);
    }

    @Override
    public <T extends AbstractDescriptor> Collection<MdibEntity> findEntitiesByType(Class<T> type) {
        return mdibStorage.findEntitiesByType(type);
    }

    @Override
    public <T extends AbstractDescriptor> List<MdibEntity> getChildrenByType(String handle, Class<T> type) {
        return mdibStorage.getChildrenByType(handle, type);
    }
}
