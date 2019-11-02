package org.ieee11073.sdc.glue.provider;

import org.ieee11073.sdc.glue.provider.sco.OperationInvocationReceiver;

/**
 * SDC provider device interface.
 * <p>
 * The purpose of the {@linkplain SdcDevice} is to provide SDC data on the network.
 */
public interface SdcDevice {
    /**
     * Adds a class to receive incoming operation invocations.
     *
     * @param receiver an instance with annotated methods
     * @see org.ieee11073.sdc.glue.provider.sco.IncomingSetServiceRequest
     * @see OperationInvocationReceiver
     */
    void addOperationInvocationReceiver(OperationInvocationReceiver receiver);
}
