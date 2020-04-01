package org.somda.sdc.glue.provider.sco;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.somda.sdc.biceps.model.message.*;
import org.somda.sdc.biceps.model.participant.InstanceIdentifier;
import org.somda.sdc.biceps.model.participant.LocalizedText;
import org.somda.sdc.biceps.model.participant.MdibVersion;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.common.util.ObjectStringifier;
import org.somda.sdc.common.util.Stringified;
import org.somda.sdc.dpws.device.EventSourceAccess;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.glue.common.ActionConstants;
import org.somda.sdc.mdpws.model.ContextValueType;
import org.somda.sdc.mdpws.model.DualChannelValueType;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Transaction context to be used on incoming set service requests in order to send reports and the initial response.
 *
 * @see InvocationResponse
 */
public class Context {
    private static final Logger LOG = LoggerFactory.getLogger(Context.class);

    @Stringified
    private final long transactionId;
    @Stringified
    private final String operationHandle;
    @Stringified
    private final InstanceIdentifier invocationSource;

    private final EventSourceAccess eventSource;
    private final LocalMdibAccess mdibAccess;
    private final Map<String, DualChannelValueType> dualChannelValues;
    private final Map<String, ContextValueType> safetyContextValues;
    private final ObjectFactory messageModelFactory;

    // this is used to track whether the last OperationInvokedReport state
    // matches the Responses state, and sends an OperationInvokedReport in case it doesn't
    private InvocationState currentReportInvocationState;

    @AssistedInject
    @Deprecated(since = "1.1.0", forRemoval = true)
    Context(@Assisted long transactionId,
            @Assisted String operationHandle,
            @Assisted InstanceIdentifier invocationSource,
            @Assisted EventSourceAccess eventSource,
            @Assisted LocalMdibAccess mdibAccess,
            ObjectFactory messageModelFactory) {
        this(transactionId,
                operationHandle,
                invocationSource,
                eventSource,
                mdibAccess,
                Collections.emptyMap(),
                Collections.emptyMap(),
                messageModelFactory);
    }

    @AssistedInject
    Context(@Assisted long transactionId,
            @Assisted String operationHandle,
            @Assisted InstanceIdentifier invocationSource,
            @Assisted EventSourceAccess eventSource,
            @Assisted LocalMdibAccess mdibAccess,
            @Assisted("dualChannelValue") Map<String, DualChannelValueType> dualChannelValues,
            @Assisted("safetyContextValue") Map<String, ContextValueType> safetyContextValues,
            ObjectFactory messageModelFactory) {
        this.transactionId = transactionId;
        this.operationHandle = operationHandle;
        this.invocationSource = invocationSource;
        this.eventSource = eventSource;
        this.mdibAccess = mdibAccess;
        this.dualChannelValues = dualChannelValues;
        this.safetyContextValues = safetyContextValues;
        this.messageModelFactory = messageModelFactory;
        this.currentReportInvocationState = null;
    }

    public LocalMdibAccess getMdibAccess() {
        return mdibAccess;
    }

    public long getTransactionId() {
        return transactionId;
    }

    public String getOperationHandle() {
        return operationHandle;
    }

    public InstanceIdentifier getInvocationSource() {
        return invocationSource;
    }

    public Map<String, DualChannelValueType> getDualChannelValues() {
        return dualChannelValues;
    }

    public Map<String, ContextValueType> getSafetyContextValues() {
        return safetyContextValues;
    }

    /**
     * Creates a successful initial invocation response based on this context.
     * <p>
     * <em>Creating the response will send an {@linkplain OperationInvokedReport} matching the response
     * if no report matching this invocation state has been sent before.</em>
     *
     * @param mdibVersion     the MDIB version that is put to the response message.
     * @param invocationState the invocation state that is put to the response message.
     *                        The enumeration is not verified.
     * @return the invocation response object.
     */
    public InvocationResponse createSuccessfulResponse(MdibVersion mdibVersion,
                                                       InvocationState invocationState) {
        if (!invocationState.equals(this.currentReportInvocationState)) {
            LOG.debug(
                    "No matching OperationInvokedReport was sent before creating response." +
                            " Sending response as OperationInvokedReport as well. Operation: {} - State: {}",
                    this.operationHandle, invocationState
            );
            sendSuccessfulReport(mdibVersion, invocationState);
        }
        return new InvocationResponse(mdibVersion, transactionId, invocationState, null, null);
    }

    /**
     * Creates a successful initial invocation response based on this context with latest MDIB version.
     * <p>
     * <em>Creating the response will send an {@linkplain OperationInvokedReport} matching the response
     * if no report matching this invocation state has been sent before.</em>
     *
     * @param invocationState the invocation state that is put to the response message.
     *                        The enumeration is not verified.
     * @return the invocation response object.
     */
    public InvocationResponse createSuccessfulResponse(InvocationState invocationState) {
        return new InvocationResponse(mdibAccess.getMdibVersion(), transactionId, invocationState, null, null);
    }

    /**
     * Creates an unsuccessful initial invocation response based on this context.
     * <p>
     * <em>Creating the response will send an {@linkplain OperationInvokedReport} matching the response
     * if no report matching this invocation state has been sent before.</em>
     *
     * @param mdibVersion            the MDIB version that is put to the response message.
     * @param invocationState        the invocation state that is put to the response message.
     *                               The enumeration is not verified.
     * @param invocationError        the specified error.
     * @param invocationErrorMessage a human-readable text to describe the error.
     * @return the invocation response object.
     */
    public InvocationResponse createUnsuccessfulResponse(MdibVersion mdibVersion,
                                                         InvocationState invocationState,
                                                         InvocationError invocationError,
                                                         List<LocalizedText> invocationErrorMessage) {
        return new InvocationResponse(mdibVersion, transactionId, invocationState, invocationError, invocationErrorMessage);
    }

    /**
     * Creates an unsuccessful initial invocation response based on this context with latest MDIB version.
     * <p>
     * <em>Creating the response will send an {@linkplain OperationInvokedReport} matching the response
     * if no report matching this invocation state has been sent before.</em>
     *
     * @param invocationState        the invocation state that is put to the response message.
     *                               The enumeration is not verified.
     * @param invocationError        the specified error.
     * @param invocationErrorMessage a human-readable text to describe the error.
     * @return the invocation response object.
     */
    public InvocationResponse createUnsuccessfulResponse(InvocationState invocationState,
                                                         InvocationError invocationError,
                                                         List<LocalizedText> invocationErrorMessage) {
        return new InvocationResponse(mdibAccess.getMdibVersion(), transactionId, invocationState, invocationError, invocationErrorMessage);
    }

    /**
     * Sends a successful operation invoked report.
     *
     * @param mdibVersion     the MDIB version that is put to the notification message.
     * @param invocationState the invocation state that is put to the notification message.
     *                        The enumeration is not verified.
     */
    public void sendSuccessfulReport(MdibVersion mdibVersion,
                                     InvocationState invocationState) {
        sendReport(mdibVersion, invocationState, null, null, null);
    }

    /**
     * Sends a successful operation invoked report.
     *
     * @param mdibVersion     the MDIB version that is put to the notification message.
     * @param invocationState the invocation state that is put to the notification message.
     *                        The enumeration is not verified.
     * @param operationTarget the operation target if available or null if unknown/irrelevant.
     */
    public void sendSuccessfulReport(MdibVersion mdibVersion,
                                     InvocationState invocationState,
                                     @Nullable String operationTarget) {
        sendReport(mdibVersion, invocationState, null, null, operationTarget);
    }

    /**
     * Sends a successful operation invoked report with latest MDIB version.
     *
     * @param invocationState the invocation state that is put to the notification message.
     *                        The enumeration is not verified.
     * @param operationTarget the operation target if available or null if unknown/irrelevant.
     */
    public void sendSuccessfulReport(InvocationState invocationState,
                                     @Nullable String operationTarget) {
        sendReport(mdibAccess.getMdibVersion(), invocationState, null, null, operationTarget);
    }

    /**
     * Sends a successful operation invoked report with latest MDIB version.
     *
     * @param invocationState the invocation state that is put to the notification message.
     *                        The enumeration is not verified.
     */
    public void sendSuccessfulReport(InvocationState invocationState) {
        sendReport(mdibAccess.getMdibVersion(), invocationState, null, null, null);
    }

    /**
     * Sends an unsuccessful operation invoked report.
     *
     * @param mdibVersion            the MDIB version that is put to the notification message.
     * @param invocationState        the invocation state that is put to the notification message.
     *                               The enumeration is not verified.
     * @param invocationError        the specified error.
     * @param invocationErrorMessage a human-readable text to describe the error.
     */
    public void sendUnsuccessfulReport(MdibVersion mdibVersion,
                                       InvocationState invocationState,
                                       InvocationError invocationError,
                                       List<LocalizedText> invocationErrorMessage) {
        sendReport(mdibVersion, invocationState, invocationError, invocationErrorMessage, null);
    }

    /**
     * Sends an unsuccessful operation invoked report with latest MDIB version.
     *
     * @param invocationState        the invocation state that is put to the notification message.
     *                               The enumeration is not verified.
     * @param invocationError        the specified error.
     * @param invocationErrorMessage a human-readable text to describe the error.
     */
    public void sendUnsuccessfulReport(InvocationState invocationState,
                                       InvocationError invocationError,
                                       List<LocalizedText> invocationErrorMessage) {
        sendReport(mdibAccess.getMdibVersion(), invocationState, invocationError, invocationErrorMessage, null);
    }

    private void sendReport(MdibVersion mdibVersion,
                            InvocationState invocationState,
                            @Nullable InvocationError invocationError,
                            @Nullable List<LocalizedText> invocationErrorMessage,
                            @Nullable String operationTarget) {

        this.currentReportInvocationState = invocationState;

        final InvocationInfo invocationInfo = messageModelFactory.createInvocationInfo();
        invocationInfo.setInvocationState(invocationState);
        invocationInfo.setTransactionId(transactionId);
        invocationInfo.setInvocationError(invocationError);
        invocationInfo.setInvocationErrorMessage(invocationErrorMessage);

        final OperationInvokedReport.ReportPart reportPart = messageModelFactory.createOperationInvokedReportReportPart();
        reportPart.setOperationHandleRef(operationHandle);
        reportPart.setOperationTarget(operationTarget);
        reportPart.setInvocationSource(invocationSource);
        reportPart.setInvocationInfo(invocationInfo);

        final OperationInvokedReport operationInvokedReport = messageModelFactory.createOperationInvokedReport();
        operationInvokedReport.setSequenceId(mdibVersion.getSequenceId().toString());
        operationInvokedReport.setInstanceId(mdibVersion.getInstanceId());
        operationInvokedReport.setMdibVersion(mdibVersion.getVersion());
        operationInvokedReport.getReportPart().add(reportPart);

        try {
            eventSource.sendNotification(ActionConstants.ACTION_OPERATION_INVOKED_REPORT, operationInvokedReport);
        } catch (MarshallingException e) {
            LOG.warn("Could not marshal operation invoked report notification of transaction {} with invocation state {}",
                    transactionId, invocationState);
        } catch (TransportException e) {
            LOG.warn("Could not deliver operation invoked report notification of transaction {} with invocation state {}",
                    transactionId, invocationState);
        }
    }

    /**
     * Returns the last {@linkplain InvocationState} for which an {@linkplain OperationInvokedReport} was sent out.
     *
     * @return The last {@linkplain InvocationState} sent as a report.
     */
    public InvocationState getCurrentReportInvocationState() {
        return currentReportInvocationState;
    }


    @Override
    public String toString() {
        return ObjectStringifier.stringify(this);
    }
}
