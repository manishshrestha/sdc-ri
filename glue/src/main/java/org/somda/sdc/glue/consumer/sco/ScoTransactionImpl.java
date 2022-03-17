package org.somda.sdc.glue.consumer.sco;

import org.somda.sdc.biceps.model.message.AbstractSetResponse;
import org.somda.sdc.biceps.model.message.OperationInvokedReport;
import org.somda.sdc.common.util.AutoLock;
import org.somda.sdc.common.util.ObjectUtil;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * Default implementation of {@linkplain ScoTransaction}
 * <p>
 * The implementation supports an internal function to trigger reception of incoming reports,
 * see {@link #receiveIncomingReport(OperationInvokedReport.ReportPart)}.
 *
 * @param <T> the response type.
 */
public class ScoTransactionImpl<T extends AbstractSetResponse> implements ScoTransaction<T> {
    private final T response;
    private final Consumer<OperationInvokedReport.ReportPart> reportListener;
    private final ArrayList<OperationInvokedReport.ReportPart> collectedReports;
    private final ReentrantLock reportsLock;
    private final Condition reportsCondition;
    private final ObjectUtil objectUtil;
    private final ScoUtil scoUtil;

    public ScoTransactionImpl(T response,
                              @Nullable Consumer<OperationInvokedReport.ReportPart> reportListener,
                              ObjectUtil objectUtil,
                              ScoUtil scoUtil) {
        this.response = response;
        this.reportListener = reportListener;
        this.reportsLock = new ReentrantLock();
        this.reportsCondition = reportsLock.newCondition();
        this.objectUtil = objectUtil;
        this.scoUtil = scoUtil;
        this.collectedReports = new ArrayList<>(3);
    }

    @Override
    public long getTransactionId() {
        return response.getInvocationInfo().getTransactionId();
    }

    @Override
    public List<OperationInvokedReport.ReportPart> getReports() {
        try (var ignored = AutoLock.lock(reportsLock)) {
            return objectUtil.deepCopy(collectedReports);
        }
    }

    @Override
    public T getResponse() {
        return objectUtil.deepCopy(response);
    }

    @Override
    public List<OperationInvokedReport.ReportPart> waitForFinalReport(Duration waitTime) {
        var copyWaitTime = waitTime;
        try (var ignored = AutoLock.lock(reportsLock)) {
            if (scoUtil.hasFinalReport(collectedReports)) {
                return objectUtil.deepCopy(collectedReports);
            }

            do {
                Instant start = Instant.now();
                try {
                    if (reportsCondition.await(waitTime.toMillis(), TimeUnit.MILLISECONDS)) {
                        if (scoUtil.hasFinalReport(collectedReports)) {
                            return objectUtil.deepCopy(collectedReports);
                        }
                    } else {
                        return Collections.emptyList();
                    }
                } catch (InterruptedException e) {
                    if (scoUtil.hasFinalReport(collectedReports)) {
                        return objectUtil.deepCopy(collectedReports);
                    } else {
                        return Collections.emptyList();
                    }
                }

                Instant finish = Instant.now();
                copyWaitTime = copyWaitTime.minus(Duration.between(start, finish));
            } while (copyWaitTime.toMillis() > 0);
        }
        return Collections.emptyList();
    }

    /**
     * Internal function to trigger reception of an incoming report.
     * <p>
     * Notifies waiting threads.
     *
     * @param report the report to receive.
     */
    public void receiveIncomingReport(OperationInvokedReport.ReportPart report) {
        try (var ignored = AutoLock.lock(reportsLock)) {
            collectedReports.add(report);
            reportsCondition.signalAll();
        }

        if (reportListener != null) {
            reportListener.accept(report);
        }
    }
}
