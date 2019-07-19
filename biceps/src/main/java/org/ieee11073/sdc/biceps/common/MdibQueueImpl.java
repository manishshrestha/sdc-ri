package org.ieee11073.sdc.biceps.common;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Default implementation of {@link MdibQueue}.
 *
 * Runs a single thread to distribute upcoming description modification of state update requests.
 */
public class MdibQueueImpl extends AbstractExecutionThreadService implements MdibQueue {
    private static final Logger LOG = LoggerFactory.getLogger(MdibQueueImpl.class);

    private MdibQueueConsumer consumer;
    private final BlockingQueue<QueueItem> blockingQueue;

    @AssistedInject
    MdibQueueImpl(@Assisted MdibQueueConsumer consumer,
                  @Named(CommonConfig.MDIB_QUEUE_SIZE) Integer queueSize) {
        this.consumer = consumer;
        this.blockingQueue = new LinkedBlockingQueue<>(queueSize);
    }

    @Override
    protected void run() throws Exception {
        while (isRunning()) {
            final QueueItem queueItem = blockingQueue.take();
            if (queueItem.isShutDown()) {
                break;
            }
            if (queueItem.getReport() instanceof MdibDescriptionModifications) {
                consumer.consume((MdibDescriptionModifications)queueItem.getReport());
                continue;
            }
            if (queueItem.getReport() instanceof MdibStateModifications) {
                consumer.consume((MdibStateModifications)queueItem.getReport());
                continue;
            }
            LOG.warn("Unknown modifications class found: {}", queueItem.getReport().getClass());
        }
    }

    @Override
    public boolean provideDescriptionModifications(MdibDescriptionModifications descriptionModifications) {
        checkRunning();
        return blockingQueue.offer(new QueueItem(descriptionModifications));
    }

    @Override
    public boolean provideStateModifications(MdibStateModifications stateModifications) {
        checkRunning();
        return blockingQueue.offer(new QueueItem(stateModifications));
    }

    @Override
    protected void triggerShutdown() {
        blockingQueue.clear();
        if (!blockingQueue.offer(new ShutDown())) {
            LOG.warn("Could not offer shut down indicator to MDIB queue.");
        }
    }

    @Override
    protected void shutDown() {
    }

    private void checkRunning() {
        if (!isRunning()) {
            throw new RuntimeException("Service stopped. Elements can no longer be added to queue.");
        }
    }

    private class QueueItem {
        private final Object report;

        QueueItem(@Nullable Object report) {
            this.report = report;
        }

        public boolean isShutDown() {
            return report == null;
        }

        Object getReport() {
            return report;
        }
    }

    private class ShutDown extends QueueItem {
        ShutDown() {
            super(null);
        }
    }
}
