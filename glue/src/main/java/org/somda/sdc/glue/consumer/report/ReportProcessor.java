package org.somda.sdc.glue.consumer.report;

import com.google.inject.Inject;
import org.somda.sdc.biceps.common.MdibStateModifications;
import org.somda.sdc.biceps.common.storage.PreprocessingException;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.model.message.AbstractReport;
import org.somda.sdc.biceps.model.message.GetContextStatesResponse;
import org.somda.sdc.biceps.model.participant.MdibVersion;
import org.somda.sdc.common.util.AutoLock;
import org.somda.sdc.glue.common.MdibVersionUtil;
import org.somda.sdc.glue.consumer.report.helper.ReportWriter;

import javax.annotation.Nullable;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class ReportProcessor {
    private final ReentrantLock mdibReadyLock;
    private final Condition mdibReadyCondition;
    private final MdibVersionUtil mdibVersionUtil;
    private final ReportWriter reportWriter;
    private final BlockingQueue<AbstractReport> bufferedReports;

    private AtomicBoolean bufferingRequested;
    private RemoteMdibAccess mdibAccess;
    private Consumer<AbstractReport> writeReport;
    private GetContextStatesResponse contextStatesResponse;

    @Inject
    ReportProcessor(ReentrantLock mdibReadyLock,
                    MdibVersionUtil mdibVersionUtil,
                    ReportWriter reportWriter) {
        this.mdibReadyLock = mdibReadyLock;
        this.mdibReadyCondition = mdibReadyLock.newCondition();
        this.mdibVersionUtil = mdibVersionUtil;
        this.reportWriter = reportWriter;
        this.bufferedReports = new ArrayBlockingQueue<>(500); // todo make queue size configurable
        this.mdibAccess = null;
        this.bufferingRequested = new AtomicBoolean(true);
        initMdibAccessWait();
    }

    public <T extends AbstractReport> void processReport(T report) {
        this.writeReport.accept(report);
    }

    public void startApplyingReportsOnMdib(RemoteMdibAccess mdibAccess, @Nullable GetContextStatesResponse contextStatesResponse)
            throws InterruptedException, PreprocessingException, ReportProcessingException {

        applyReportsFromBuffer(mdibAccess);
        bufferingRequested.set(false);
        applyReportsFromBuffer(mdibAccess);


        try (AutoLock ignored = AutoLock.lock(mdibReadyLock)) {
            this.mdibAccess = mdibAccess;
            this.contextStatesResponse = contextStatesResponse;
            mdibReadyCondition.signalAll();
        }
    }

    private void applyReportsFromBuffer(RemoteMdibAccess mdibAccess) throws InterruptedException, PreprocessingException, ReportProcessingException {
        while (!bufferedReports.isEmpty()) {
            applyReportOnMdib(bufferedReports.take(), mdibAccess);
        }
    }

    private void applyReportOnMdib(AbstractReport report) throws PreprocessingException, ReportProcessingException {
        applyReportOnMdib(report, mdibAccess);
    }

    private void applyReportOnMdib(AbstractReport report, RemoteMdibAccess mdibAccess) throws PreprocessingException, ReportProcessingException {
        final MdibVersion mdibAccessMdibVersion = mdibAccess.getMdibVersion();
        final MdibVersion reportMdibVersion = mdibVersionUtil.getMdibVersion(report);
        if (!mdibAccessMdibVersion.getSequenceId().equals(reportMdibVersion.getSequenceId()) ||
                mdibAccessMdibVersion.getInstanceId().equals(reportMdibVersion.getInstanceId())) {
            throw new ReportProcessingException(String.format("MDIB version from MDIB (%s) and MDIB version from report (%s) do not match",
                    mdibAccessMdibVersion, reportMdibVersion));
        }

        if (mdibAccessMdibVersion.getVersion().compareTo(reportMdibVersion.getVersion()) >= 0) {
            return;
        }

        reportWriter.write(report, mdibAccess);
    }

    private void initMdibAccessWait() {
        this.writeReport = report -> {
            if (bufferingRequested.get()) {
                bufferedReports.add(report);
                return;
            }

            try (AutoLock ignored = AutoLock.lock(mdibReadyLock)) {
                if (mdibAccess == null) {
                    if (!mdibReadyCondition.await(10000, TimeUnit.MILLISECONDS)) {
                        throw new TimeoutException("No MDIB access set within 10s");
                    }
                    if (mdibAccess == null) {
                        throw new NullPointerException("Expected MDIB access to be set, but was null");
                    }
                }

                applyReportOnMdib(report);
                applyOptionalContextStates();
                writeReport = abstractReport -> {
                    try {
                        applyReportOnMdib(report);
                    } catch (PreprocessingException | ReportProcessingException e) {
                        throw new RuntimeException(e);
                    }
                };
            } catch (InterruptedException | PreprocessingException | TimeoutException | ReportProcessingException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private void applyOptionalContextStates() throws PreprocessingException, ReportProcessingException {
        if (contextStatesResponse == null) {
            return;
        }

        final MdibVersion mdibVersion = mdibAccess.getMdibVersion();
        if (!mdibVersion.getSequenceId().toString().equals(contextStatesResponse.getSequenceId()) ||
                !mdibVersion.getInstanceId().equals(contextStatesResponse.getInstanceId())) {
            throw new ReportProcessingException(String.format("Received context state report belongs to different MDIB sequence/instance. Expected: %s, actual: %s",
                    mdibVersionUtil.getMdibVersion(contextStatesResponse), mdibVersion));
        }

        if (mdibVersion.getVersion().compareTo(contextStatesResponse.getMdibVersion()) >= 0) {
            return;
        }

        final MdibStateModifications modifications = MdibStateModifications.create(MdibStateModifications.Type.CONTEXT)
                .addAll(contextStatesResponse.getContextState());
        mdibAccess.writeStates(mdibAccess.getMdibVersion(), modifications);
    }
}
