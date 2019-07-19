package org.ieee11073.sdc.biceps.common.factory;

import com.google.inject.assistedinject.Assisted;
import org.ieee11073.sdc.biceps.common.MdibQueue;
import org.ieee11073.sdc.biceps.common.MdibQueueConsumer;

/**
 * Factory to create an MDIB queue.
 */
public interface MdibQueueFactory {
    /**
     * Create MDIB queue.
     *
     * @param consumer The consumer handles any queued items from the MDIB query.
     */
    MdibQueue createMdibQueue(@Assisted MdibQueueConsumer consumer);
}
