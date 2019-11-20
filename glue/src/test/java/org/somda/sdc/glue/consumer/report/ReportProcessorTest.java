package org.somda.sdc.glue.consumer.report;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.somda.sdc.biceps.common.MdibStateModifications;
import org.somda.sdc.biceps.common.storage.PreprocessingException;
import org.somda.sdc.biceps.consumer.access.RemoteMdibAccess;
import org.somda.sdc.biceps.model.message.AbstractReport;
import org.somda.sdc.biceps.model.message.EpisodicMetricReport;
import org.somda.sdc.biceps.model.message.GetContextStatesResponse;
import org.somda.sdc.biceps.model.participant.MdibVersion;
import org.somda.sdc.glue.UnitTestUtil;
import org.somda.sdc.glue.common.MdibVersionUtil;
import org.somda.sdc.glue.consumer.report.helper.ReportWriter;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ReportProcessorTest {
    private static final UnitTestUtil UT = new UnitTestUtil();

    private ReportProcessor reportProcessor;
    private ReportWriter reportWriter;
    private ArgumentCaptor<AbstractReport> reportCaptor;
    private ArgumentCaptor<RemoteMdibAccess> mdibAccessCaptor;
    private RemoteMdibAccess mdibAccess;
    private ArgumentCaptor<MdibVersion> mdibVersionCaptor;
    private ArgumentCaptor<MdibStateModifications> mdibStateModificationsCaptor;
    private MdibVersionUtil mdibVersionUtil;

    @BeforeEach
    void beforeEach() {
        reportWriter = mock(ReportWriter.class);
        Injector injector = UT.createInjectorWithOverrides(new AbstractModule() {
            @Override
            protected void configure() {
                super.configure();
                bind(ReportWriter.class).toInstance(reportWriter);
            }
        });

        reportProcessor = injector.getInstance(ReportProcessor.class);
        reportCaptor = ArgumentCaptor.forClass(AbstractReport.class);
        mdibAccessCaptor = ArgumentCaptor.forClass(RemoteMdibAccess.class);

        mdibVersionCaptor = ArgumentCaptor.forClass(MdibVersion.class);
        mdibStateModificationsCaptor = ArgumentCaptor.forClass(MdibStateModifications.class);

        mdibAccess = mock(RemoteMdibAccess.class);

        mdibVersionUtil = UT.getInjector().getInstance(MdibVersionUtil.class);
    }

    @Test
    void applyMdibWithoutPreviousReportsWithoutContexts() throws PreprocessingException, ReportProcessingException {
        reportProcessor.startApplyingReportsOnMdib(mdibAccess, null);
        verifyZeroInteractions(mdibAccess);
    }

    @Test
    void applyMdibWithoutPreviousReportsWithContexts() throws PreprocessingException, ReportProcessingException {
        MdibVersion mdibVersion = MdibVersion.create();
        MdibVersion mdibVersionIncremented = MdibVersion.increment(mdibVersion);

        when(mdibAccess.getMdibVersion()).thenReturn(mdibVersion);

        GetContextStatesResponse contextStatesResponse = createContextStatesResponse(mdibVersionIncremented);

        reportProcessor.startApplyingReportsOnMdib(mdibAccess, contextStatesResponse);
        verify(mdibAccess).writeStates(mdibVersionCaptor.capture(), mdibStateModificationsCaptor.capture());
        assertEquals(mdibVersion, mdibVersionCaptor.getValue());
    }

    @Test
    void applyWithPreviousReportsMdibWithoutContexts() throws PreprocessingException, ReportProcessingException {
        MdibVersion baseVersion = MdibVersion.create();
        MdibVersion mdibVersion = MdibVersion.setVersionCounter(baseVersion, BigInteger.TEN);

        MdibVersion reportVersion1 = MdibVersion.setVersionCounter(baseVersion, BigInteger.valueOf(7));
        MdibVersion reportVersion2 = MdibVersion.setVersionCounter(baseVersion, BigInteger.valueOf(11));
        MdibVersion reportVersion3 = MdibVersion.setVersionCounter(baseVersion, BigInteger.valueOf(13));
        EpisodicMetricReport report1 = createReport(reportVersion1);
        EpisodicMetricReport report2 = createReport(reportVersion2);
        EpisodicMetricReport report3 = createReport(reportVersion3);
        when(mdibAccess.getMdibVersion())
                .thenReturn(mdibVersion)
                .thenReturn(mdibVersion)
                .thenReturn(reportVersion2);
        reportProcessor.processReport(report1);
        reportProcessor.processReport(report2);
        reportProcessor.processReport(report3);

        reportProcessor.startApplyingReportsOnMdib(mdibAccess, null);

        verify(reportWriter, times(2)).write(reportCaptor.capture(), mdibAccessCaptor.capture());
        assertEquals(2, reportCaptor.getAllValues().size());
        assertEquals(reportVersion2, mdibVersionUtil.getMdibVersion(reportCaptor.getAllValues().get(0)));
        assertEquals(reportVersion3, mdibVersionUtil.getMdibVersion(reportCaptor.getAllValues().get(1)));
    }

    @Test
    void applyWithPreviousReportsMdibWithContexts() throws PreprocessingException, ReportProcessingException {
        MdibVersion baseVersion = MdibVersion.create();
        MdibVersion mdibVersion = MdibVersion.setVersionCounter(baseVersion, BigInteger.TEN);

        MdibVersion reportVersion1 = MdibVersion.setVersionCounter(baseVersion, BigInteger.valueOf(7));
        MdibVersion reportVersion2 = MdibVersion.setVersionCounter(baseVersion, BigInteger.valueOf(11));
        MdibVersion reportVersion3 = MdibVersion.setVersionCounter(baseVersion, BigInteger.valueOf(13));
        EpisodicMetricReport report1 = createReport(reportVersion1);
        EpisodicMetricReport report2 = createReport(reportVersion2);
        EpisodicMetricReport report3 = createReport(reportVersion3);

        when(mdibAccess.getMdibVersion())
                .thenReturn(mdibVersion)
                .thenReturn(mdibVersion)
                .thenReturn(mdibVersion)
                .thenReturn(reportVersion2);

        reportProcessor.processReport(report1);
        reportProcessor.processReport(report2);
        reportProcessor.processReport(report3);

        MdibVersion contextStatesVersion = MdibVersion.setVersionCounter(baseVersion, BigInteger.valueOf(11));
        GetContextStatesResponse contextStatesResponse = createContextStatesResponse(contextStatesVersion);
        reportProcessor.startApplyingReportsOnMdib(mdibAccess, contextStatesResponse);

        verify(reportWriter, times(2)).write(reportCaptor.capture(), mdibAccessCaptor.capture());
        assertEquals(2, reportCaptor.getAllValues().size());
        assertEquals(reportVersion2, mdibVersionUtil.getMdibVersion(reportCaptor.getAllValues().get(0)));
        assertEquals(reportVersion3, mdibVersionUtil.getMdibVersion(reportCaptor.getAllValues().get(1)));

        verify(mdibAccess).writeStates(mdibVersionCaptor.capture(), mdibStateModificationsCaptor.capture());
        assertEquals(mdibVersion, mdibVersionCaptor.getValue());
    }

    private GetContextStatesResponse createContextStatesResponse(MdibVersion mdibVersion) {
        GetContextStatesResponse response = new GetContextStatesResponse();
        try {
            UT.getInjector().getInstance(MdibVersionUtil.class).setMdibVersion(mdibVersion, response);
        } catch (Exception e) {
            throw new RuntimeException();
        }

        return response;
    }

    private EpisodicMetricReport createReport(MdibVersion mdibVersion) {
        EpisodicMetricReport report = new EpisodicMetricReport();
        try {
            mdibVersionUtil.setMdibVersion(mdibVersion, report);
        } catch (Exception e) {
            throw new RuntimeException();
        }

        return report;
    }
}