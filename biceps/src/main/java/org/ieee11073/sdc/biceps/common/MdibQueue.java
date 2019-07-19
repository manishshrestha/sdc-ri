package org.ieee11073.sdc.biceps.common;

import com.google.common.util.concurrent.Service;

/**
 * Offers write access to an MDIB.
 *
 * The {@linkplain MdibQueue} is a blocking queue.
 */
public interface MdibQueue extends Service {
    /**
     * Queue a description modification.
     *
     * If the element cannot be added to the queue, processing is too slowly and needs to be throttled.
     *
     * @return true if the element could be added to the queue or false if queue was full.
     */
    boolean provideDescriptionModifications(MdibDescriptionModifications descriptionModifications);

    /**
     * Queue a state update report.
     *
     * If the element cannot be added to the queue, processing is too slowly and needs to be throttled.
     *
     * @return true if the element could be added to the queue or false if queue was full.
     */
    boolean provideStateModifications(MdibStateModifications stateModifications);
}
