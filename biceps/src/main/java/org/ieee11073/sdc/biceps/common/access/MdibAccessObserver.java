package org.ieee11073.sdc.biceps.common.access;

/**
 * Marks a class to be an MDIB changes observer.
 * <p>
 * Annotate any method with {@link com.google.common.eventbus.Subscribe} to get callbacks on
 * <ul>
 * <li>{@link org.ieee11073.sdc.biceps.common.event.AlertStateModificationMessage}
 * <li>{@link org.ieee11073.sdc.biceps.common.event.ComponentStateModificationMessage}
 * <li>{@link org.ieee11073.sdc.biceps.common.event.ContextStateModificationMessage}
 * <li>{@link org.ieee11073.sdc.biceps.common.event.DescriptionModificationMessage}
 * <li>{@link org.ieee11073.sdc.biceps.common.event.MetricStateModificationMessage}
 * <li>{@link org.ieee11073.sdc.biceps.common.event.OperationStateModificationMessage}
 * </ul>
 */
public interface MdibAccessObserver {
}
