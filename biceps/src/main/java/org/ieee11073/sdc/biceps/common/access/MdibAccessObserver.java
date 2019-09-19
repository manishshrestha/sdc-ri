package org.ieee11073.sdc.biceps.common.access;

/**
 * Indicate class as a MDIB changes observer.
 *
 * Annotate method with {@link com.google.common.eventbus.Subscribe} to
 *
 * - {@link org.ieee11073.sdc.biceps.common.event.AlertStateModificationMessage}
 * - {@link org.ieee11073.sdc.biceps.common.event.ComponentStateModificationMessage}
 * - {@link org.ieee11073.sdc.biceps.common.event.ContextStateModificationMessage}
 * - {@link org.ieee11073.sdc.biceps.common.event.DescriptionModificationMessage}
 * - {@link org.ieee11073.sdc.biceps.common.event.MetricStateModificationMessage}
 * - {@link org.ieee11073.sdc.biceps.common.event.OperationStateModificationMessage}
 */
public interface MdibAccessObserver {
}
