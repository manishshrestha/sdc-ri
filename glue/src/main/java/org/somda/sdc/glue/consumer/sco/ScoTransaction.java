package org.somda.sdc.glue.consumer.sco;

import org.somda.sdc.biceps.model.message.AbstractSetResponse;
import org.somda.sdc.biceps.model.message.OperationInvokedReport;

import java.time.Duration;
import java.util.List;

/**
 * Definition of an SDC transaction to track incoming operation invoked report parts.
 *
 * @param <T> Type of the invocation response message.
 */
public interface ScoTransaction<T extends AbstractSetResponse> {
    /**
     * Gets the transaction id.
     * <p>
     * Shortcut of accessing {@code getTransactionId()} of {@link #getResponse()}.
     *
     * @return the transaction id of this {@linkplain ScoTransactionImpl}.
     */
    long getTransactionId();

    /**
     * Gets all reports received so far.
     *
     * @return Snapshot of received reports as a copy.
     */
    List<OperationInvokedReport.ReportPart> getReports();

    /**
     * Gets set response message.
     *
     * @return a copy of the set response message of this {@linkplain ScoTransactionImpl}.
     */
    T getResponse();

    /**
     * Starts waiting for a final report.
     * <p>
     * A report is final if {@link ScoUtil#isFinalReport(OperationInvokedReport.ReportPart)} holds true.
     *
     * @param waitTime maximum wait time until this function returns.
     * @return a list that holds all reports including the final one or an empty list if no final report
     * has been received.
     */
    List<OperationInvokedReport.ReportPart> waitForFinalReport(Duration waitTime);
}
