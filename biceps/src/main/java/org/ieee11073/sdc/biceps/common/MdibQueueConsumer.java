package org.ieee11073.sdc.biceps.common;

/**
 * Consumer interface for {@link MdibQueue} wit empty default implementations.
 */
public interface MdibQueueConsumer {
    /**
     * Consume a description modification.
     */
    default void consume(MdibDescriptionModifications descriptionModifications) {
    }

    /**
     * Consume a state update.
     */
    default void consume(MdibStateModifications stateModifications) {
    }
}
