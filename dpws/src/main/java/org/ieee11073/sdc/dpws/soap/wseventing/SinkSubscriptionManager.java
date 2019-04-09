package org.ieee11073.sdc.dpws.soap.wseventing;

/**
 * Subscription manager to indicate usage on event sink side.
 */
public interface SinkSubscriptionManager extends SubscriptionManager {
    boolean isAutoRenewEnabled();

    void setAutoRenewEnabled(boolean enabled);
}
