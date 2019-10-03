package org.ieee11073.sdc.dpws.soap.wseventing;

/**
 * Subscription manager interface that is used by event sinks.
 */
public interface SinkSubscriptionManager extends SubscriptionManager {
    boolean isAutoRenewEnabled();

    void setAutoRenewEnabled(boolean enabled);
}
