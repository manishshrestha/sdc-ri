package org.somda.sdc.glue.consumer.report;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

/**
 * Class that is responsible for buffering and processing of incoming reports.
 * <p>
 * As long as there is no MDIB (plus optional context states) available, the report processor buffers incoming reports
 * and only applies them once an MDIB (plus optional context states) was set.
 * In case every report type is received through one subscription, the {@linkplain ReportProcessor} ensures data
 * coherency.
 */
public class ReportProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(ReportProcessor.class);

    private final ReentrantLock mdibReadyLock;
    private final Condition mdibReadyCondition;
    private final MdibVersionUtil mdibVersionUtil;
    private final ReportWriter reportWriter;
    private final BlockingQueue<AbstractReport> bufferedReports;

    private AtomicBoolean bufferingRequested;
    private RemoteMdibAccess mdibAccess;
    private Consumer<AbstractReport> writeReport;

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

    /**
     * Queues or processes a report.
     * <p>
     * In case no MDIB was set via {@link #startApplyingReportsOnMdib(RemoteMdibAccess, GetContextStatesResponse)} yet,
     * this function queues incoming reports.
     * Once {@link #startApplyingReportsOnMdib(RemoteMdibAccess, GetContextStatesResponse)} was called, reports are
     * directly applied on the injected {@link RemoteMdibAccess} instance.
     * <p>
     * <em>Attention: this function is not thread-safe. Calling it in parallel will invalidate the report processing!</em>
     *
     * @param report the report to process.
     * @param <T>    any report type in terms of the SDC protocol.
     */
    public <T extends AbstractReport> void processReport(T report) {
        this.writeReport.accept(report);
    }

    /**
     * Accepts an MDIB and starts applying reports on it.
     *
     * @param mdibAccess            the MDIB access to use for application of incoming reports.
     * @param contextStatesResponse optionally a get-context-states response message in case a get-mdib request did not returned some.
     * @throws PreprocessingException    is thrown in case writing to the MDIB fails.
     * @throws ReportProcessingException is thrown in case there is any error during processing of a report or accessing data from the queue.
     */
    public void startApplyingReportsOnMdib(RemoteMdibAccess mdibAccess, @Nullable GetContextStatesResponse contextStatesResponse)
            throws PreprocessingException, ReportProcessingException {
        try (AutoLock ignored = AutoLock.lock(mdibReadyLock)) {
            if (this.mdibAccess != null) {
                LOG.warn("Tried to invoke startApplyingReportsOnMdib() multiple times. " +
                        "Make sure to call it only once. " +
                        "Invocation ignored.");
                return;
            }
        }

        applyOptionalContextStates(mdibAccess, contextStatesResponse);

        applyReportsFromBuffer(mdibAccess);
        bufferingRequested.set(false);
        applyReportsFromBuffer(mdibAccess);

        try (AutoLock ignored = AutoLock.lock(mdibReadyLock)) {
            this.mdibAccess = mdibAccess;
            mdibReadyCondition.signalAll();
        }
    }

    private void applyReportsFromBuffer(RemoteMdibAccess mdibAccess) throws PreprocessingException, ReportProcessingException {
        while (!bufferedReports.isEmpty()) {
            try {
                applyReportOnMdib(bufferedReports.take(), mdibAccess);
            } catch (InterruptedException e) {
                throw new ReportProcessingException("Could not take an element from the queue though expected");
            }
        }
    }

    private void applyReportOnMdib(AbstractReport report) throws PreprocessingException, ReportProcessingException {
        applyReportOnMdib(report, mdibAccess);
    }

    private void applyReportOnMdib(AbstractReport report, RemoteMdibAccess mdibAccess) throws PreprocessingException, ReportProcessingException {
        final MdibVersion mdibAccessMdibVersion = mdibAccess.getMdibVersion();
        final MdibVersion reportMdibVersion = mdibVersionUtil.getMdibVersion(report);
        if (!mdibAccessMdibVersion.getSequenceId().equals(reportMdibVersion.getSequenceId()) ||
                !mdibAccessMdibVersion.getInstanceId().equals(reportMdibVersion.getInstanceId())) {
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

    private void applyOptionalContextStates(RemoteMdibAccess mdibAccess, @Nullable GetContextStatesResponse contextStatesResponse)
            throws PreprocessingException, ReportProcessingException {
        if (contextStatesResponse == null) {
            return;
        }

        final MdibVersion mdibVersion = mdibAccess.getMdibVersion();
        if (!mdibVersion.getSequenceId().toString().equals(contextStatesResponse.getSequenceId()) ||
                !mdibVersion.getInstanceId().equals(contextStatesResponse.getInstanceId())) {
            throw new ReportProcessingException(String.format("Received context state report belongs to different MDIB sequence/instance. Expected: %s, actual: %s",
                    mdibVersionUtil.getMdibVersion(contextStatesResponse), mdibVersion));
        }

        if (mdibVersion.getVersion().compareTo(contextStatesResponse.getMdibVersion()) > 0) {
            LOG.warn("Found a context state response whose MDIB version ({}) is older than the MDIB's MDIB version ({})",
                    contextStatesResponse.getMdibVersion(), mdibVersion.getVersion());
            return;
        }

        final MdibStateModifications modifications = MdibStateModifications.create(MdibStateModifications.Type.CONTEXT)
                .addAll(contextStatesResponse.getContextState());
        mdibAccess.writeStates(mdibVersion, modifications);
    }
}
