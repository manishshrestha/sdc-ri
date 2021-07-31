package org.somda.sdc.glue.provider.sco;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.somda.sdc.biceps.model.message.InvocationError;
import org.somda.sdc.biceps.model.message.InvocationInfo;
import org.somda.sdc.biceps.model.message.InvocationState;
import org.somda.sdc.biceps.model.message.ObjectFactory;
import org.somda.sdc.biceps.model.message.OperationInvokedReport;
import org.somda.sdc.biceps.model.participant.InstanceIdentifier;
import org.somda.sdc.biceps.model.participant.LocalizedText;
import org.somda.sdc.biceps.model.participant.MdibVersion;
import org.somda.sdc.biceps.provider.access.LocalMdibAccess;
import org.somda.sdc.common.CommonConfig;
import org.somda.sdc.common.logging.InstanceLogger;
import org.somda.sdc.common.util.ObjectStringifier;
import org.somda.sdc.common.util.Stringified;
import org.somda.sdc.dpws.device.EventSourceAccess;
import org.somda.sdc.dpws.soap.exception.MarshallingException;
import org.somda.sdc.dpws.soap.exception.TransportException;
import org.somda.sdc.glue.common.ActionConstants;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Transaction context to be used on incoming set service requests in order to send reports and the initial response.
 *
 * @see InvocationResponse
 */
public class Context {
    private static final Logger LOG = LogManager.getLogger(Context.class);

    @Stringified
    private final long transactionId;
    @Stringified
    private final String operationHandle;
    @Stringified
    private final InstanceIdentifier invocationSource;

    private final EventSourceAccess eventSource;
    private final LocalMdibAccess mdibAccess;
    private final ObjectFactory messageModelFactory;
    private final Logger instanceLogger;

    @AssistedInject
    Context(@Assisted long transactionId,
            @Assisted String operationHandle,
            @Assisted InstanceIdentifier invocationSource,
            @Assisted EventSourceAccess eventSource,
            @Assisted LocalMdibAccess mdibAccess,
            ObjectFactory messageModelFactory,
            @Named(CommonConfig.INSTANCE_IDENTIFIER) String frameworkIdentifier) {
        this.instanceLogger = InstanceLogger.wrapLogger(LOG, frameworkIdentifier);
        this.transactionId = transactionId;
        this.operationHandle = operationHandle;
        this.invocationSource = invocationSource;
        this.eventSource = eventSource;
        this.mdibAccess = mdibAccess;
        this.messageModelFactory = messageModelFactory;
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
     * Creates a successful initial invocation response based on this context with latest MDIB version.
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
        return new InvocationResponse(mdibVersion, transactionId, invocationState,
                invocationError, invocationErrorMessage);
    }

    /**
     * Creates an unsuccessful initial invocation response based on this context with latest MDIB version.
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
        return new InvocationResponse(mdibAccess.getMdibVersion(), transactionId, invocationState,
                invocationError, invocationErrorMessage);
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

    /**
     * Sends an operation invoked report with the information passed as arguments.
     *
     * @param mdibVersion            the invocation state that is put to the notification message.
     *                               The enumeration is not verified.
     * @param invocationState        the invocation state to send.
     * @param invocationError        the specified error or null if this is not an error report.
     * @param invocationErrorMessage a human-readable text to describe the error or null if this is not an error report.
     * @param operationTarget        the operation target if available or null if unknown/irrelevant.
     */
    public void sendReport(MdibVersion mdibVersion,
                           InvocationState invocationState,
                           @Nullable InvocationError invocationError,
                           @Nullable List<LocalizedText> invocationErrorMessage,
                           @Nullable String operationTarget) {

        final InvocationInfo invocationInfo = messageModelFactory.createInvocationInfo();
        invocationInfo.setInvocationState(invocationState);
        invocationInfo.setTransactionId(transactionId);
        invocationInfo.setInvocationError(invocationError);
        invocationInfo.setInvocationErrorMessage(invocationErrorMessage);

        final OperationInvokedReport.ReportPart reportPart =
                messageModelFactory.createOperationInvokedReportReportPart();
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
            instanceLogger.warn("Could not marshal operation invoked report notification of transaction {} " +
                            "with invocation state {}",
                    transactionId, invocationState);
        } catch (TransportException e) {
            instanceLogger.warn("Could not deliver operation invoked report notification of transaction {} " +
                            "with invocation state {}",
                    transactionId, invocationState);
        }
    }

    @Override
    public String toString() {
        return ObjectStringifier.stringify(this);
    }
}
