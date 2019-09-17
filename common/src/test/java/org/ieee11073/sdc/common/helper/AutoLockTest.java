package org.ieee11073.sdc.common.helper;

import org.junit.jupiter.api.Test;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AutoLockTest {


    @Test
    public void autoClose() {
        final ReentrantReadWriteLock mutex = new ReentrantReadWriteLock();
        {
            // Without auto-closable
            assertThat(mutex.isWriteLocked(), is(false));
            ReentrantReadWriteLock.WriteLock writeLock = mutex.writeLock();
            writeLock.lock();
            assertThat(mutex.isWriteLocked(), is(true));
            writeLock.unlock();
            assertThat(mutex.isWriteLocked(), is(false));

            writeLock.lock();
            try {
                assertThat(mutex.isWriteLocked(), is(true));
            } finally {
                writeLock.unlock();
            }
            assertThat(mutex.isWriteLocked(), is(false));
        }

        {
            // With auto-closable
            assertThat(mutex.isWriteLocked(), is(false));
            try (AutoLock lock = AutoLock.lock(mutex.writeLock())) {
                assertThat(mutex.isWriteLocked(), is(true));
            }
            assertThat(mutex.isWriteLocked(), is(false));
        }
    }
}
