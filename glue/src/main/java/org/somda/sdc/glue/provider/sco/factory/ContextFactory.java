package org.somda.sdc.glue.provider.sco.factory;

import com.google.inject.assistedinject.Assisted;
import org.somda.sdc.biceps.model.participant.InstanceIdentifier;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.dpws.device.EventSourceAccess;
import org.somda.sdc.glue.provider.sco.Context;
import org.somda.sdc.mdpws.model.ContextValueType;
import org.somda.sdc.mdpws.model.DualChannelValueType;

import java.util.Map;

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
     * @param mdibAccess        the MDIB access be used to read and/or modify on function callback.
     * @return the new instance.
     * @deprecated obsoleted by {@link #createContext(long, String, InstanceIdentifier, EventSourceAccess, LocalMdibAccess, Map, Map)}
     */
    @Deprecated(since = "1.1.0", forRemoval = true)
    Context createContext(@Assisted long transactionId,
                          @Assisted String operationHandle,
                          @Assisted InstanceIdentifier invocationSource,
                          @Assisted EventSourceAccess eventSourceAccess,
                          @Assisted LocalMdibAccess mdibAccess);

    /**
     * Creates a new {@linkplain Context} instance.
     *
     * @param transactionId       transaction id that is requestable and used to create reports.
     *                            Uniqueness shall be assured by the caller.
     * @param operationHandle     the handle of operation this context belongs to.
     * @param invocationSource    the instance identifier used to add as invocation source to reports.
     * @param eventSourceAccess   the event source access to send report notifications.
     * @param mdibAccess          the MDIB access be used to read and/or modify on function callback.
     * @param dualChannelValues   MDPWS dual channel values received from the service consumer.
     * @param safetyContextValues MDPWS safety context values received from the service consumer.
     * @return the new instance.
     */
    Context createContext(@Assisted long transactionId,
                          @Assisted String operationHandle,
                          @Assisted InstanceIdentifier invocationSource,
                          @Assisted EventSourceAccess eventSourceAccess,
                          @Assisted LocalMdibAccess mdibAccess,
                          @Assisted("dualChannelValue") Map<String, DualChannelValueType> dualChannelValues,
                          @Assisted("safetyContextValue") Map<String, ContextValueType> safetyContextValues);
}
