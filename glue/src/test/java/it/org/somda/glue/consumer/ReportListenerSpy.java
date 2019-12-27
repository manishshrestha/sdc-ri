package it.org.somda.glue.consumer;

import org.somda.sdc.biceps.model.message.OperationInvokedReport;
import test.org.somda.common.TimedWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class ReportListenerSpy implements java.util.function.Consumer<OperationInvokedReport.ReportPart> {
    private final TimedWait<List<OperationInvokedReport.ReportPart>> timedWaiter;

    public ReportListenerSpy() {
        this.timedWaiter = new TimedWait<>(ArrayList::new);
    }

    @Override
    public void accept(OperationInvokedReport.ReportPart reportPart) {
        timedWaiter.modifyData(reportParts -> reportParts.add(reportPart));
    }

    public List<OperationInvokedReport.ReportPart> getReports() {
        return timedWaiter.getData();
    }

    public boolean waitForReports(int reportCount, Duration waitTime) {
        return timedWaiter.waitForData(reportParts -> reportParts.size() == reportCount, waitTime);
    }

    public void reset() {
        timedWaiter.reset();
    }
}
