package org.ieee11073.sdc.glue.provider.sco.factory;

import com.google.inject.assistedinject.Assisted;
import org.ieee11073.sdc.biceps.model.participant.InstanceIdentifier;
import org.ieee11073.sdc.dpws.device.EventSourceAccess;
import org.ieee11073.sdc.glue.provider.sco.Context;

/**
 * Factory to create {@linkplain Context} instances.
 */
public interface ContextFactory {
    /**
     * Creates a new {@linkplain Context} instance.
     *
     * @param transactionId     transaction id that is requestable and used to create reports.
     *                          Uniqueness shall be assured by the caller.
     * @param operationHandle   the handle of operation this context belongs to.
     * @param invocationSource  the instance identifier used to add as invocation source to reports.
     * @param eventSourceAccess the event source access to send report notifications.
     * @return the new instance.
     */
    Context createContext(@Assisted long transactionId,
                          @Assisted String operationHandle,
                          @Assisted InstanceIdentifier invocationSource,
                          @Assisted EventSourceAccess eventSourceAccess);
}
