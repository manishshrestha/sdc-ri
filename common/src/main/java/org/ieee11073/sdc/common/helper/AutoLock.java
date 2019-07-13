package org.ieee11073.sdc.common.helper;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * Utility class to make locks auto-closable.
 */
public class AutoLock<T extends Lock> implements Lock, AutoCloseable {
    private final T lock;

    private AutoLock(T lock) {
        this.lock = lock;
    }

    /**
     * Lock the given lock and return auto-closable variant.
     */
     public static <T extends Lock> AutoLock lock(T lock) {
        lock.lock();
        return new AutoLock(lock);
    }

    /**
     * Return the wrapped lock instance.
     */
    public T getLock() {
        return lock;
    }

    @Override
    public void close() {
        lock.unlock();
    }

    @Override
    public void lock() {
        lock.lock();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        lock.lockInterruptibly();
    }

    @Override
    public boolean tryLock() {
        return lock.tryLock();
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return lock.tryLock(time, unit);
    }

    @Override
    public void unlock() {
        lock.unlock();
    }

    @Override
    public Condition newCondition() {
        return lock.newCondition();
    }
}
