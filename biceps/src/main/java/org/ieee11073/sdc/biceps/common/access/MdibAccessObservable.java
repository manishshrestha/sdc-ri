package org.ieee11073.sdc.biceps.common.access;

public interface MdibAccessObservable {
    /**
     * Register for MDIB modification reports.
     *
     * {@linkplain MdibAccess} fires
     */
    void registerObserver(MdibAccessObserver observer);

    /**
     * Unregister from MDIB modification reports.
     */
    void unregisterObserver(MdibAccessObserver observer);
}
