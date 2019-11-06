package org.somda.sdc.glue.provider.sco;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.somda.sdc.biceps.model.message.*;
import org.somda.sdc.biceps.model.participant.InstanceIdentifier;
import org.somda.sdc.biceps.model.participant.LocalizedText;
import org.somda.sdc.biceps.model.participant.MdibVersion;
import org.somda.sdc.common.util.ObjectStringifier;
import org.somda.sdc.common.util.Stringified;
import org.somda.sdc.dpws.device.EventSourceAccess;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.glue.common.ActionConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Transaction context to be used on incomibg set service requests in order to send reports and the initial response.
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
    private final ObjectFactory messageModelFactory;

    @AssistedInject
    Context(@Assisted long transactionId,
            @Assisted String operationHandle,
            @Assisted InstanceIdentifier invocationSource,
            @Assisted EventSourceAccess eventSource,
            ObjectFactory messageModelFactory) {
        this.transactionId = transactionId;
        this.operationHandle = operationHandle;
        this.invocationSource = invocationSource;
        this.eventSource = eventSource;
        this.messageModelFactory = messageModelFactory;
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

    /**
     * Creates a successful initial invocation response based on this context.
     *
     * @param mdibVersion     the MDIB version that is put to the response message.
     * @param invocationState the invocation state that is put to the response message.
     *                        The enumeration is not verified.
     * @return the invocation response object.
     */
    public InvocationResponse createSuccessfulResponse(MdibVersion mdibVersion,
                                                       InvocationState invocationState) {
        return new InvocationResponse(mdibVersion, transactionId, invocationState, null, null);
    }

    /**
     * Creates an unsuccessful initial invocation response based on this context.
     *
     * @param mdibVersion            the MDIB version that is put to the response message.
     * @param invocationState        the invocation state that is put to the response message.
     *                               The enumeration is not verified.
     * @param invocationError        the specified error.
     * @param invocationErrorMessage a human-readable text to describe the error.
     * @return the invocation response object.
     */
    public InvocationResponse createUnsucessfulResponse(MdibVersion mdibVersion,
                                                        InvocationState invocationState,
                                                        InvocationError invocationError,
                                                        List<LocalizedText> invocationErrorMessage) {
        return new InvocationResponse(mdibVersion, transactionId, invocationState, invocationError, invocationErrorMessage);
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
     * Sends an unsuccessful operation invoked report.
     *
     * @param mdibVersion            the MDIB version that is put to the notification message.
     * @param invocationState        the invocation state that is put to the notification message.
     *                               The enumeration is not verified.
     * @param invocationError        the specified error.
     * @param invocationErrorMessage a human-readable text to describe the error.
     */
    public void sendUnsucessfulReport(MdibVersion mdibVersion,
                                      InvocationState invocationState,
                                      InvocationError invocationError,
                                      List<LocalizedText> invocationErrorMessage) {
        sendReport(mdibVersion, invocationState, invocationError, invocationErrorMessage, null);
    }

    private void sendReport(MdibVersion mdibVersion,
                            InvocationState invocationState,
                            @Nullable InvocationError invocationError,
                            @Nullable List<LocalizedText> invocationErrorMessage,
                            @Nullable String operationTarget) {

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

    @Override
    public String toString() {
        return ObjectStringifier.stringify(this);
    }
}
