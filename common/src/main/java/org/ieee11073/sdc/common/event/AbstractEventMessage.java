package org.ieee11073.sdc.common.event;

/**
 * Simple message container to strongly type messages for Guava's {@link com.google.common.eventbus.EventBus}.
 */
public abstract class AbstractEventMessage<T> implements EventMessage{
    private final T payload;

    protected AbstractEventMessage(T payload) {
        this.payload = payload;
    }

    public T getPayload() {
        return payload;
    }
}
