package org.ieee11073.sdc.biceps.common;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.log4j.BasicConfigurator;
import org.ieee11073.sdc.biceps.common.factory.MdibQueueFactory;
import org.ieee11073.sdc.biceps.guice.DefaultBicepsConfigModule;
import org.ieee11073.sdc.biceps.guice.DefaultBicepsModule;
import org.ieee11073.sdc.common.helper.AutoLock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class MdibQueueImplTest {
    private Injector injector;
    private MdibQueueFactory mdibQueueFactory;
    private int queueSize;

    @Before
    public void setUp() {
        BasicConfigurator.configure();
        queueSize = 50;
        // Choose custom queue size for testing
        final DefaultBicepsConfigModule config = new DefaultBicepsConfigModule();
        config.bind(CommonConfig.MDIB_QUEUE_SIZE, Integer.class, queueSize);
        injector = Guice.createInjector(new DefaultBicepsModule(), config);
        mdibQueueFactory = injector.getInstance(MdibQueueFactory.class);
    }

    @Test
    public void provideAndConsume() {
        int descriptionModificationCount = queueSize*5;
        int stateModificationCount = queueSize*10;

        List<MdibDescriptionModifications> expectedDescriptionModificationsList = new ArrayList<>();
        List<MdibStateModifications> expectedStateModificationsList = new ArrayList<>();

        TestConsumer consumer = new TestConsumer();
        final MdibQueue mdibQueue = mdibQueueFactory.createMdibQueue(consumer);
        mdibQueue.startAsync().awaitRunning();

        for (int i = 0; i < descriptionModificationCount; ++i) {
            final MdibDescriptionModifications mock = Mockito.mock(MdibDescriptionModifications.class);
            if (mdibQueue.provideDescriptionModifications(mock)) {
                expectedDescriptionModificationsList.add(mock);
            }
        }

        for (int i = 0; i < stateModificationCount; ++i) {
            final MdibStateModifications mock = Mockito.mock(MdibStateModifications.class);
            if (mdibQueue.provideStateModifications(mock)) {
                expectedStateModificationsList.add(mock);
            }
        }

        consumer.waitForModifications(descriptionModificationCount + stateModificationCount, Duration.ofSeconds(10));

        // Expect those elements that could be provided to be consumed
        assertThat(consumer.getDescriptionModificationsList().size(), is(expectedDescriptionModificationsList.size()));
        assertThat(consumer.getStateModificationsList().size(), is(expectedStateModificationsList.size()));
        assertThat(consumer.getDescriptionModificationsList(), is(expectedDescriptionModificationsList));
        assertThat(consumer.getStateModificationsList(), is(expectedStateModificationsList));

        mdibQueue.stopAsync().awaitTerminated();
    }

    @Test
    public void exceedQueueSize() {
        BlockingTestConsumer consumer = new BlockingTestConsumer();

        final MdibQueue mdibQueue = mdibQueueFactory.createMdibQueue(consumer);

        try {
            mdibQueue.provideDescriptionModifications(Mockito.mock(MdibDescriptionModifications.class));
            Assert.fail("Service ought to be started before queueing elements");
        } catch (Exception e) {
        }

        mdibQueue.startAsync().awaitRunning();

        // +1 because the first element is dropped off the queue and then blocks others to be processed
        for (int i = 0; i < queueSize + 1; ++i) {
            boolean queued = mdibQueue.provideDescriptionModifications(Mockito.mock(MdibDescriptionModifications.class));
            assertThat(queued, is(true));
        }

        // Add some more and see that queueing fails
        for (int i = 0; i < 3; ++i) {
            boolean queued = mdibQueue.provideDescriptionModifications(Mockito.mock(MdibDescriptionModifications.class));
            assertThat(queued, is(false));
        }

        consumer.stop();

        mdibQueue.stopAsync().awaitTerminated();
        try {
            mdibQueue.provideDescriptionModifications(Mockito.mock(MdibDescriptionModifications.class));
            Assert.fail("No more queueing ought to be possible after service stopped");
        } catch (Exception e) {
        }
    }

    public class TestConsumer implements MdibQueueConsumer {
        private final Lock lock = new ReentrantLock();
        private final Condition condition = lock.newCondition();
        private List<MdibDescriptionModifications> descriptionModificationsList = new ArrayList<>();
        private List<MdibStateModifications> stateModificationsList = new ArrayList<>();

        public void reset() {
            try (AutoLock ignore = AutoLock.lock(lock)) {
                descriptionModificationsList.clear();
                stateModificationsList.clear();
            }
        }

        @Override
        public void consume(MdibDescriptionModifications descriptionModifications) {
            try (AutoLock ignore = AutoLock.lock(lock)) {
                descriptionModificationsList.add(descriptionModifications);
                condition.signalAll();
            }
        }

        @Override
        public void consume(MdibStateModifications stateModifications) {
            try (AutoLock ignore = AutoLock.lock(lock)) {
                stateModificationsList.add(stateModifications);
                condition.signalAll();
            }
        }

        public boolean waitForModifications(int threshold,
                                            Duration waitTime) {
            try (AutoLock ignore = AutoLock.lock(lock)) {
                if (descriptionModificationsList.size() + stateModificationsList.size() >= threshold) {
                    return true;
                }
                long wait = waitTime.toNanos();
                final long start = System.nanoTime();
                while (wait > 0) {
                    condition.await(wait, TimeUnit.NANOSECONDS);
                    if (descriptionModificationsList.size() + stateModificationsList.size() >= threshold) {
                        return true;
                    }
                    wait -= ((System.nanoTime()) - start);
                }
            } catch (InterruptedException e) {
            }
            return false;
        }

        public List<MdibDescriptionModifications> getDescriptionModificationsList() {
            return descriptionModificationsList;
        }

        public List<MdibStateModifications> getStateModificationsList() {
            return stateModificationsList;
        }
    }

    private class BlockingTestConsumer implements MdibQueueConsumer {
        private final Lock lock = new ReentrantLock();
        private final Condition condition = lock.newCondition();
        private boolean data = false;

        @Override
        public void consume(MdibDescriptionModifications descriptionModifications) {
            try (AutoLock ignored = AutoLock.lock(lock)) {
                while (!data) {
                    condition.await();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public void stop() {
            try (AutoLock ignored = AutoLock.lock(lock)) {
                data = true;
                condition.signalAll();
            }
        }
    }
}
