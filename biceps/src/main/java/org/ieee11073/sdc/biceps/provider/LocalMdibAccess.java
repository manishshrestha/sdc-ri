package org.ieee11073.sdc.biceps.provider;

public interface LocalMdibAccess {
    void registerObserver();
    void unregisterObserver();

    void writeDescription();
    void writeComponentStates();
    void writeAlertStates();
    void writeMetricStates();
    void writeContextStates();
    void writeRealTimeSampleArrayMetrics();
}
